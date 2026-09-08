package ps.reso.instaeclipse.utils.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import ps.reso.instaeclipse.utils.log.ModuleLog;

/**
 * Version-independent resolver orchestration.
 *
 * A feature supplies ordered candidates (cheap/cache-backed first, expensive
 * structural/semantic discovery later) and a validation predicate. A method
 * is never considered resolved merely because it was found: it must pass the
 * feature's validation contract first.
 */
public final class AdaptiveResolver {
    private AdaptiveResolver() {}

    public static Result resolve(
            String feature,
            List<Candidate> candidates,
            Predicate<Method> validator) {
        if (candidates == null || candidates.isEmpty()) {
            return Result.failure("no candidates");
        }

        List<Candidate> ordered = new ArrayList<>(candidates);
        Collections.sort(ordered, Comparator.comparingInt(Candidate::priority));

        for (Candidate candidate : ordered) {
            Method method = null;
            try {
                method = candidate.resolve.get();
            } catch (Throwable t) {
                ModuleLog.line("(InstaEclipse | Resolver | " + feature + "): "
                        + candidate.name + " threw: " + safe(t));
            }

            if (method == null) {
                ModuleLog.line("(InstaEclipse | Resolver | " + feature + "): "
                        + candidate.name + " → not found");
                continue;
            }

            boolean valid = false;
            try {
                valid = validator == null || validator.test(method);
            } catch (Throwable t) {
                ModuleLog.line("(InstaEclipse | Resolver | " + feature + "): "
                        + candidate.name + " → validation threw: " + safe(t));
            }

            if (!valid) {
                ModuleLog.line("(InstaEclipse | Resolver | " + feature + "): "
                        + candidate.name + " → rejected by validation");
                continue;
            }

            ModuleLog.line("(InstaEclipse | Resolver | " + feature + "): "
                    + "✅ " + candidate.name + " validated");
            return Result.success(method, candidate.name);
        }

        return Result.failure("all candidates rejected");
    }

    private static String safe(Throwable t) {
        String value = t == null ? "unknown" : t.toString();
        return value.length() > 300 ? value.substring(0, 300) : value;
    }

    public static final class Candidate {
        private final String name;
        private final int priority;
        private final Supplier<Method> resolve;

        public Candidate(String name, int priority, Supplier<Method> resolve) {
            if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name");
            if (resolve == null) throw new IllegalArgumentException("resolve");
            this.name = name;
            this.priority = priority;
            this.resolve = resolve;
        }

        public String name() { return name; }
        public int priority() { return priority; }
    }

    public static final class Result {
        private final Method method;
        private final String resolver;
        private final String failureReason;

        private Result(Method method, String resolver, String failureReason) {
            this.method = method;
            this.resolver = resolver;
            this.failureReason = failureReason;
        }

        public static Result success(Method method, String resolver) {
            return new Result(method, resolver, null);
        }

        public static Result failure(String reason) {
            return new Result(null, null, reason);
        }

        public boolean isSuccess() { return method != null; }
        public Method method() { return method; }
        public String resolver() { return resolver; }
        public String failureReason() { return failureReason; }
    }
}
