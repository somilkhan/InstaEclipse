package ps.reso.instaeclipse.utils.compat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import ps.reso.instaeclipse.utils.log.ModuleLog;

/**
 * Process-local compatibility state for host-app features.
 *
 * The runtime deliberately records failures instead of silently swallowing them.
 * A feature may trip its breaker after repeated runtime failures while the rest
 * of InstaEclipse continues operating. State is reset on every Instagram process.
 */
public final class CompatibilityRuntime {
    private static final int FAILURE_THRESHOLD = 3;
    private static final Map<String, FeatureState> FEATURES = new ConcurrentHashMap<>();
    private static volatile String hostVersion = "unknown";

    private CompatibilityRuntime() {}

    public static void initialize(String version) {
        hostVersion = version == null || version.isEmpty() ? "unknown" : version;
        FEATURES.clear();
        ModuleLog.line("(Compatibility) Host Instagram build=" + hostVersion);
    }

    public static boolean begin(String feature) {
        FeatureState state = state(feature);
        if (state.tripped) {
            ModuleLog.line("(Compatibility | " + feature + "): ⛔ circuit open");
            return false;
        }
        return true;
    }

    public static void installed(String feature, String resolver) {
        FeatureState state = state(feature);
        state.installed = true;
        state.resolver = resolver == null ? "unknown" : resolver;
        ModuleLog.line("(Compatibility | " + feature + "): ✅ installed via " + state.resolver);
    }

    public static void unavailable(String feature, String reason) {
        FeatureState state = state(feature);
        state.installed = false;
        state.lastError = reason == null ? "unknown" : reason;
        ModuleLog.line("(Compatibility | " + feature + "): ⚠️ unavailable: " + state.lastError);
    }

    public static void runtimeFailure(String feature, Throwable error) {
        FeatureState state = state(feature);
        int failures = state.failures.incrementAndGet();
        state.lastError = error == null ? "unknown" : error.toString();
        ModuleLog.line("(Compatibility | " + feature + "): ❌ runtime failure " + failures + "/" + FAILURE_THRESHOLD + ": " + state.lastError);
        if (failures >= FAILURE_THRESHOLD) {
            state.tripped = true;
            ModuleLog.line("(Compatibility | " + feature + "): 🛑 circuit opened after repeated failures");
        }
    }

    public static String snapshot() {
        StringBuilder out = new StringBuilder();
        out.append("Instagram=").append(hostVersion).append('\n');
        for (Map.Entry<String, FeatureState> entry : FEATURES.entrySet()) {
            FeatureState s = entry.getValue();
            out.append(entry.getKey()).append('=').append(s.tripped ? "BROKEN" : s.installed ? "OK" : "UNAVAILABLE")
                    .append(" resolver=").append(s.resolver)
                    .append(" failures=").append(s.failures.get());
            if (s.lastError != null) out.append(" error=").append(s.lastError);
            out.append('\n');
        }
        return out.toString();
    }

    private static FeatureState state(String feature) {
        return FEATURES.computeIfAbsent(feature == null ? "unknown" : feature, ignored -> new FeatureState());
    }

    private static final class FeatureState {
        boolean installed;
        boolean tripped;
        String resolver = "none";
        String lastError;
        final AtomicInteger failures = new AtomicInteger();
    }
}
