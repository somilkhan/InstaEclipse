package ps.reso.instaeclipse.utils.core;

import android.os.SystemClock;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import ps.reso.instaeclipse.utils.log.ModuleLog;

/** Process-local compatibility supervisor. No Instagram-specific assumptions. */
public final class CompatibilityRuntime {
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final long FAILURE_WINDOW_MS = 30_000L;
    private static final long CIRCUIT_COOLDOWN_MS = 60_000L;
    private static final Map<String, FeatureHealth> FEATURES = new ConcurrentHashMap<>();

    private CompatibilityRuntime() {}

    public static FeatureHealth begin(String feature) {
        FeatureHealth health = FEATURES.computeIfAbsent(feature, ignored -> new FeatureHealth());
        health.lastAttemptMs = SystemClock.elapsedRealtime();
        return health;
    }

    public static boolean canRun(String feature) {
        FeatureHealth health = FEATURES.get(feature);
        if (health == null || !health.circuitOpen) return true;
        long now = SystemClock.elapsedRealtime();
        if (now - health.circuitOpenedMs >= CIRCUIT_COOLDOWN_MS) {
            health.circuitOpen = false;
            health.consecutiveFailures.set(0);
            health.status = Status.RECOVERING;
            log(feature, "circuit cooldown elapsed; allowing recovery attempt");
            return true;
        }
        return false;
    }

    public static boolean guard(String feature, Runnable action) {
        if (action == null || !canRun(feature)) return false;
        begin(feature);
        try {
            action.run();
            return true;
        } catch (Throwable t) {
            runtimeFailed(feature, t);
            return false;
        }
    }

    public static void installed(String feature, String resolver) {
        FeatureHealth health = begin(feature);
        health.consecutiveFailures.set(0);
        health.circuitOpen = false;
        health.status = Status.INSTALLED;
        health.resolver = resolver == null ? "unknown" : resolver;
        health.lastError = "";
        log(feature, "installed via " + health.resolver);
    }

    public static void resolverFailed(String feature, String reason) {
        FeatureHealth health = begin(feature);
        health.status = Status.RESOLVER_FAILED;
        health.lastError = sanitize(reason);
        log(feature, "resolver failed: " + health.lastError);
    }

    public static void installFailed(String feature, Throwable error) {
        FeatureHealth health = begin(feature);
        health.status = Status.INSTALL_FAILED;
        health.lastError = sanitize(error == null ? "unknown install failure" : error.toString());
        log(feature, "install failed: " + health.lastError);
    }

    public static void runtimeFailed(String feature, Throwable error) {
        FeatureHealth health = begin(feature);
        long now = SystemClock.elapsedRealtime();
        if (health.firstFailureMs == 0L || now - health.firstFailureMs > FAILURE_WINDOW_MS) {
            health.firstFailureMs = now;
            health.consecutiveFailures.set(0);
        }
        health.lastError = sanitize(error == null ? "unknown runtime failure" : error.toString());
        int failures = health.consecutiveFailures.incrementAndGet();
        health.status = Status.RUNTIME_FAILED;
        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            health.circuitOpen = true;
            health.circuitOpenedMs = now;
            health.status = Status.CIRCUIT_OPEN;
            log(feature, "circuit opened after " + failures + " runtime failures");
        } else {
            log(feature, "runtime failure " + failures + "/" + MAX_CONSECUTIVE_FAILURES + ": " + health.lastError);
        }
    }

    public static void reset(String feature) {
        FEATURES.remove(feature);
        log(feature, "health reset");
    }

    public static Map<String, FeatureHealthSnapshot> snapshot() {
        Map<String, FeatureHealthSnapshot> result = new TreeMap<>();
        for (Map.Entry<String, FeatureHealth> entry : FEATURES.entrySet()) {
            result.put(entry.getKey(), entry.getValue().snapshot());
        }
        return Collections.unmodifiableMap(result);
    }

    private static void log(String feature, String message) {
        ModuleLog.line("(InstaEclipse | Compat | " + feature + "): " + message);
    }

    private static String sanitize(String value) {
        if (value == null) return "unknown";
        return value.length() > 512 ? value.substring(0, 512) : value;
    }

    public enum Status { INSTALLED, RESOLVER_FAILED, INSTALL_FAILED, RUNTIME_FAILED, CIRCUIT_OPEN, RECOVERING }

    public static final class FeatureHealthSnapshot {
        public final Status status;
        public final String resolver;
        public final String lastError;
        public final int consecutiveFailures;
        public final boolean circuitOpen;
        public final long lastAttemptMs;
        public final long circuitOpenedMs;

        private FeatureHealthSnapshot(FeatureHealth health) {
            status = health.status;
            resolver = health.resolver;
            lastError = health.lastError;
            consecutiveFailures = health.consecutiveFailures.get();
            circuitOpen = health.circuitOpen;
            lastAttemptMs = health.lastAttemptMs;
            circuitOpenedMs = health.circuitOpenedMs;
        }
    }

    public static final class FeatureHealth {
        private volatile Status status = Status.RESOLVER_FAILED;
        private volatile String resolver = "unknown";
        private volatile String lastError = "not initialized";
        private final AtomicInteger consecutiveFailures = new AtomicInteger();
        private volatile boolean circuitOpen;
        private volatile long firstFailureMs;
        private volatile long circuitOpenedMs;
        private volatile long lastAttemptMs;

        private FeatureHealthSnapshot snapshot() { return new FeatureHealthSnapshot(this); }
    }
}
