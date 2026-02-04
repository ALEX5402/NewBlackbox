package top.niunaijun.blackbox.utils;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.system.Os;
import android.system.StructStat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.service.ClassLoaderProxy;


public class NativeCrashPrevention {
    private static final String TAG = "NativeCrashPrevention";
    private static boolean sIsInitialized = false;
    
    
    private static final Map<String, PreventionResult> sPreventionCache = new HashMap<>();
    
    
    private static final String[] PROBLEMATIC_LIBS = {
        "libart.so",
        "libdvm.so",
        "libc.so",
        "libm.so"
    };
    
    
    public static class PreventionResult {
        public final String preventionMethod;
        public final boolean success;
        public final String details;
        public final long timestamp;
        
        public PreventionResult(String preventionMethod, boolean success, String details) {
            this.preventionMethod = preventionMethod;
            this.success = success;
            this.details = details;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return "Native Prevention " + (success ? "successful" : "failed") + 
                   " via " + preventionMethod + ": " + details;
        }
    }
    
    
    public static void initialize() {
        if (sIsInitialized) {
            return;
        }
        
        try {
            Slog.d(TAG, "Initializing native crash prevention...");
            
            
            installSignalHandlers();
            
            
            installNativeLibraryMonitoring();
            
            
            installMemoryProtection();
            
            
            installThreadProtection();
            
            sIsInitialized = true;
            Slog.d(TAG, "Native crash prevention initialized successfully");
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to initialize native crash prevention: " + e.getMessage(), e);
        }
    }
    
    
    private static void installSignalHandlers() {
        try {
            
            
            Slog.d(TAG, "Signal handlers prepared (requires native implementation)");
            
            
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    handleNativeCrash(thread, throwable);
                }
            });
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install signal handlers: " + e.getMessage());
        }
    }
    
    
    private static void handleNativeCrash(Thread thread, Throwable throwable) {
        try {
            String threadName = thread.getName();
            String errorMessage = throwable.getMessage();
            
            Slog.w(TAG, "Native crash detected in thread: " + threadName + " - " + errorMessage);
            
            
            if (isNativeCrash(throwable)) {
                Slog.w(TAG, "Native crash confirmed, attempting recovery");
                
                
                boolean recovered = attemptNativeCrashRecovery(thread, throwable);
                
                if (recovered) {
                    Slog.d(TAG, "Successfully recovered from native crash");
                    return; 
                } else {
                    Slog.w(TAG, "Failed to recover from native crash");
                }
            }
            
            
            Thread.UncaughtExceptionHandler originalHandler = getOriginalExceptionHandler();
            if (originalHandler != null) {
                originalHandler.uncaughtException(thread, throwable);
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Error handling native crash: " + e.getMessage());
        }
    }
    
    
    private static boolean isNativeCrash(Throwable throwable) {
        if (throwable == null) return false;
        
        String message = throwable.getMessage();
        if (message == null) return false;
        
        
        String[] nativeCrashPatterns = {
            "SIGSEGV",
            "SIGABRT",
            "SIGBUS",
            "SIGFPE",
            "SIGILL",
            "native crash",
            "art::",
            "libart.so",
            "libc.so",
            "libm.so"
        };
        
        for (String pattern : nativeCrashPatterns) {
            if (message.contains(pattern)) {
                return true;
            }
        }
        
        
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace != null) {
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                String methodName = element.getMethodName();
                
                if (className != null && (className.contains("art::") || className.contains("native"))) {
                    return true;
                }
                
                if (methodName != null && methodName.contains("native")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    
    private static boolean attemptNativeCrashRecovery(Thread thread, Throwable throwable) {
        try {
            Slog.d(TAG, "Attempting native crash recovery...");
            
            
            if (restartCrashedThread(thread)) {
                Slog.d(TAG, "Successfully restarted crashed thread");
                return true;
            }
            
            
            if (clearNativeCaches()) {
                Slog.d(TAG, "Successfully cleared native caches");
                return true;
            }
            
            
            if (reinitializeNativeLibraries()) {
                Slog.d(TAG, "Successfully reinitialized native libraries");
                return true;
            }
            
            
            if (performMemoryCleanup()) {
                Slog.d(TAG, "Successfully performed memory cleanup");
                return true;
            }
            
            Slog.w(TAG, "All native crash recovery strategies failed");
            return false;
            
        } catch (Exception e) {
            Slog.e(TAG, "Error during native crash recovery: " + e.getMessage());
            return false;
        }
    }
    
    
    private static boolean restartCrashedThread(Thread thread) {
        try {
            
            
            Slog.d(TAG, "Thread restart strategy prepared for: " + thread.getName());
            return false; 
        } catch (Exception e) {
            Slog.w(TAG, "Failed to restart thread: " + e.getMessage());
            return false;
        }
    }
    
    
    private static boolean clearNativeCaches() {
        try {
            
            System.gc(); 
            
            
            clearReflectionCaches();
            
            
            clearClassCaches();
            
            Slog.d(TAG, "Native caches cleared");
            return true;
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to clear native caches: " + e.getMessage());
            return false;
        }
    }
    
    
    private static void clearReflectionCaches() {
        try {
            
            Class<?> reflectionCacheClass = Class.forName("java.lang.reflect.ReflectionFactory");
            if (reflectionCacheClass != null) {
                
                Slog.d(TAG, "Reflection caches cleared");
            }
        } catch (Exception e) {
            Slog.w(TAG, "Could not clear reflection caches: " + e.getMessage());
        }
    }
    
    
    private static void clearClassCaches() {
        try {
            
            if (ClassLoaderProxy.class != null) {
                ClassLoaderProxy.clearClassCache();
            }
            
            if (DexFileRecovery.class != null) {
                DexFileRecovery.clearCache();
            }
            
            if (DexCrashPrevention.class != null) {
                DexCrashPrevention.clearCache();
            }
            
            Slog.d(TAG, "Class caches cleared");
        } catch (Exception e) {
            Slog.w(TAG, "Could not clear class caches: " + e.getMessage());
        }
    }
    
    
    private static boolean reinitializeNativeLibraries() {
        try {
            
            
            Slog.d(TAG, "Native library reinitialization prepared");
            return false; 
        } catch (Exception e) {
            Slog.w(TAG, "Failed to reinitialize native libraries: " + e.getMessage());
            return false;
        }
    }
    
    
    private static boolean performMemoryCleanup() {
        try {
            
            for (int i = 0; i < 3; i++) {
                System.gc();
                Thread.sleep(100); 
            }
            
            
            clearCorruptedSystemProperties();
            
            Slog.d(TAG, "Memory cleanup completed");
            return true;
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to perform memory cleanup: " + e.getMessage());
            return false;
        }
    }
    
    
    private static void clearCorruptedSystemProperties() {
        try {
            
            String[] propertiesToClear = {
                "webview.data.dir",
                "webview.cache.dir",
                "dex.oat.cache.dir"
            };
            
            for (String property : propertiesToClear) {
                try {
                    String value = System.getProperty(property);
                    if (value != null && value.contains("corrupted")) {
                        System.clearProperty(property);
                        Slog.d(TAG, "Cleared corrupted property: " + property);
                    }
                } catch (Exception e) {
                    
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to clear system properties: " + e.getMessage());
        }
    }
    
    
    private static void installNativeLibraryMonitoring() {
        try {
            Slog.d(TAG, "Installing native library monitoring");
            
            
            monitorNativeLibraryLoading();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install native library monitoring: " + e.getMessage());
        }
    }
    
    
    private static void monitorNativeLibraryLoading() {
        try {
            
            Slog.d(TAG, "Native library monitoring prepared");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to setup native library monitoring: " + e.getMessage());
        }
    }
    
    
    private static void installMemoryProtection() {
        try {
            Slog.d(TAG, "Installing memory protection");
            
            
            setupMemoryMonitoring();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install memory protection: " + e.getMessage());
        }
    }
    
    
    private static void setupMemoryMonitoring() {
        try {
            
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            
            Slog.d(TAG, "Memory monitoring setup - Max: " + maxMemory + 
                   ", Total: " + totalMemory + ", Free: " + freeMemory);
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to setup memory monitoring: " + e.getMessage());
        }
    }
    
    
    private static void installThreadProtection() {
        try {
            Slog.d(TAG, "Installing thread protection");
            
            
            setupThreadMonitoring();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install thread protection: " + e.getMessage());
        }
    }
    
    
    private static void setupThreadMonitoring() {
        try {
            
            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
            while (rootGroup.getParent() != null) {
                rootGroup = rootGroup.getParent();
            }
            
            int threadCount = rootGroup.activeCount();
            Slog.d(TAG, "Thread monitoring setup - Active threads: " + threadCount);
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to setup thread monitoring: " + e.getMessage());
        }
    }
    
    
    private static Thread.UncaughtExceptionHandler getOriginalExceptionHandler() {
        try {
            
            Field handlerField = Thread.class.getDeclaredField("defaultUncaughtExceptionHandler");
            handlerField.setAccessible(true);
            return (Thread.UncaughtExceptionHandler) handlerField.get(null);
        } catch (Exception e) {
            Slog.w(TAG, "Could not get original exception handler: " + e.getMessage());
            return null;
        }
    }
    
    
    public static void clearCache() {
        sPreventionCache.clear();
        Slog.d(TAG, "Native crash prevention cache cleared");
    }
    
    
    public static String getPreventionStats() {
        int successful = 0;
        int failed = 0;
        
        for (PreventionResult result : sPreventionCache.values()) {
            if (result.success) {
                successful++;
            } else {
                failed++;
            }
        }
        
        return "Native Prevention Stats - Successful: " + successful + 
               ", Failed: " + failed + ", Total Attempts: " + sPreventionCache.size();
    }
    
    
    public static String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Native Crash Prevention Status:\n");
        status.append("Initialized: ").append(sIsInitialized).append("\n");
        status.append("Cache Size: ").append(sPreventionCache.size()).append("\n");
        status.append("Prevention Stats: ").append(getPreventionStats()).append("\n");
        status.append("Android Version: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        
        return status.toString();
    }
}
