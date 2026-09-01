package ps.reso.instaeclipse.utils.core;

import android.os.SystemClock;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import ps.reso.instaeclipse.utils.log.ModuleLog;

/**
 * Process-local compatibility supervisor.
 *
 * Features are independently tracked so resolver/runtime failures become
 * diagnosable states rather than silent failures or process-wide failures.
 * This class deliberately contains no Instagram-specific assumptions.
 */
public final class CompatibilityRuntime {
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final long FAILURE_WINDOW_MS = 30_000L;
    private static final Map<String, FeatureHealth> FEATURES = new ConcurrentHashMap<>();

    private CompatibilityRuntime() {}

    public static FeatureHealth begin(String feature) {
        FeatureHealth health = FEATURES.computeIfAbsent(feature, ignored -> new FeatureHealth());
        health.lastAttemptMs = SystemClock.elapsedRealtime();
        return health;
    }

    public static boolean canRun(String feature) {
        FeatureHealth health = FEATURES.get(feature);
        return health == null || !health.circuitOpen;
    }

    public static void installed(String feature, String resolver) {
        FeatureHealth health = begin(feature);
        health.consecutiveFailures.set(0);
        health.circuitOpen = false;
        health.status = Status.INSTALLED;
        health.resolver = resolver == null ? "unknown" : resolver;
        log(feature, "installed via " + health.resolver);
    }

    public static void resolverFailed(String feature, String reason) {
        FeatureHealth health = begin(feature);
        health.status = Status.RESOLVER_FAILED;
        health.lastError = sanitize(reason);
        log(feature, "resolver failed: " + health.lastError);
    }

    public static void runtimeFailed(String feature, Throwable error) {
        FeatureHealth health = begin(feature);
        long now = SystemClock.elapsedRealtime();
        if (now - health.firstFailureMs > FAILURE_WINDOW_MS) {
            health.firstFailureMs = now;
            health.consecutiveFailures.set(0);
        }
        health.lastError = sanitize(error == null ? "unknown runtime failure" : error.toString());
        int failures = health.consecutiveFailures.incrementAndGet();
        health.status = Status.RUNTIME_FAILED;
        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            health.circuitOpen = true;
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
        Map<String, FeatureHealthSnapshot> result = new java.util.TreeMap<>();
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

    public enum Status { INSTALLED, RESOLVER_FAILED, RUNTIME_FAILED, CIRCUIT_OPEN }

    public static final class FeatureHealthSnapshot {
        public final Status status;
        public final String resolver;
        public final String lastError;
        public final int consecutiveFailures;
        public final boolean circuitOpen;
        public final long lastAttemptMs;

        private FeatureHealthSnapshot(FeatureHealth health) {
            status = health.status;
            resolver = health.resolver;
            lastError = health.lastError;
            consecutiveFailures = health.consecutiveFailures.get();
            circuitOpen = health.circuitOpen;
            lastAttemptMs = health.lastAttemptMs;
        }
    }

    public static final class FeatureHealth {
        private volatile Status status = Status.RESOLVER_FAILED;
        private volatile String resolver = "unknown";
        private volatile String lastError = "not initialized";
        private final AtomicInteger consecutiveFailures = new AtomicInteger();
        private volatile boolean circuitOpen;
        private volatile long firstFailureMs;
        private volatile long lastAttemptMs;

        private FeatureHealthSnapshot snapshot() {
            return new FeatureHealthSnapshot(this);
        }
    }
}
