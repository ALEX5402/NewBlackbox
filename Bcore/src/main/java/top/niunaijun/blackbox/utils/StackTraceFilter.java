package top.niunaijun.blackbox.utils;

public class StackTraceFilter {
    static {
        install();
    }

    public static void install() {
        try {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                StackTraceElement[] original = e.getStackTrace();
                e.setStackTrace(filterStackTrace(original));
            });
        } catch (Throwable ignored) {}
    }

    private static StackTraceElement[] filterStackTrace(StackTraceElement[] stack) {
        return java.util.Arrays.stream(stack)
            .filter(element -> !isSuspicious(element.getClassName()))
            .toArray(StackTraceElement[]::new);
    }

    private static boolean isSuspicious(String className) {
        return className.toLowerCase().contains("xposed") ||
               className.toLowerCase().contains("epic") ||
               className.toLowerCase().contains("virtual") ||
               className.toLowerCase().contains("blackbox") ||
               className.toLowerCase().contains("hook");
    }
}
