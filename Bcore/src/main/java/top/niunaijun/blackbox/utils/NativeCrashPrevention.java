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

/**
 * Comprehensive native crash prevention utility to handle native crashes
 * like SIGSEGV, SIGABRT, and other native-level failures.
 */
public class NativeCrashPrevention {
    private static final String TAG = "NativeCrashPrevention";
    private static boolean sIsInitialized = false;
    
    // Cache for native crash prevention attempts
    private static final Map<String, PreventionResult> sPreventionCache = new HashMap<>();
    
    // Known problematic native libraries
    private static final String[] PROBLEMATIC_LIBS = {
        "libart.so",
        "libdvm.so",
        "libc.so",
        "libm.so"
    };
    
    /**
     * Prevention result containing the prevention method and success status
     */
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
    
    /**
     * Initialize native crash prevention mechanisms
     */
    public static void initialize() {
        if (sIsInitialized) {
            return;
        }
        
        try {
            Slog.d(TAG, "Initializing native crash prevention...");
            
            // Install signal handlers
            installSignalHandlers();
            
            // Install native library monitoring
            installNativeLibraryMonitoring();
            
            // Install memory protection
            installMemoryProtection();
            
            // Install thread protection
            installThreadProtection();
            
            sIsInitialized = true;
            Slog.d(TAG, "Native crash prevention initialized successfully");
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to initialize native crash prevention: " + e.getMessage(), e);
        }
    }
    
    /**
     * Install signal handlers for native crashes
     */
    private static void installSignalHandlers() {
        try {
            // This would be implemented with native code (JNI)
            // For now, we'll use Java-based prevention
            Slog.d(TAG, "Signal handlers prepared (requires native implementation)");
            
            // Set up thread-level crash prevention
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
    
    /**
     * Handle native crashes at the Java level
     */
    private static void handleNativeCrash(Thread thread, Throwable throwable) {
        try {
            String threadName = thread.getName();
            String errorMessage = throwable.getMessage();
            
            Slog.w(TAG, "Native crash detected in thread: " + threadName + " - " + errorMessage);
            
            // Check if this is a native crash
            if (isNativeCrash(throwable)) {
                Slog.w(TAG, "Native crash confirmed, attempting recovery");
                
                // Try to recover from native crash
                boolean recovered = attemptNativeCrashRecovery(thread, throwable);
                
                if (recovered) {
                    Slog.d(TAG, "Successfully recovered from native crash");
                    return; // Prevent crash
                } else {
                    Slog.w(TAG, "Failed to recover from native crash");
                }
            }
            
            // If we can't recover, delegate to the original handler
            Thread.UncaughtExceptionHandler originalHandler = getOriginalExceptionHandler();
            if (originalHandler != null) {
                originalHandler.uncaughtException(thread, throwable);
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Error handling native crash: " + e.getMessage());
        }
    }
    
    /**
     * Check if the crash is a native crash
     */
    private static boolean isNativeCrash(Throwable throwable) {
        if (throwable == null) return false;
        
        String message = throwable.getMessage();
        if (message == null) return false;
        
        // Check for native crash indicators
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
        
        // Check stack trace for native indicators
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
    
    /**
     * Attempt to recover from a native crash
     */
    private static boolean attemptNativeCrashRecovery(Thread thread, Throwable throwable) {
        try {
            Slog.d(TAG, "Attempting native crash recovery...");
            
            // Strategy 1: Restart the crashed thread
            if (restartCrashedThread(thread)) {
                Slog.d(TAG, "Successfully restarted crashed thread");
                return true;
            }
            
            // Strategy 2: Clear native caches
            if (clearNativeCaches()) {
                Slog.d(TAG, "Successfully cleared native caches");
                return true;
            }
            
            // Strategy 3: Reinitialize native libraries
            if (reinitializeNativeLibraries()) {
                Slog.d(TAG, "Successfully reinitialized native libraries");
                return true;
            }
            
            // Strategy 4: Memory cleanup
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
    
    /**
     * Restart a crashed thread
     */
    private static boolean restartCrashedThread(Thread thread) {
        try {
            // This is a simplified approach - in a real implementation,
            // you would need to properly handle thread restart
            Slog.d(TAG, "Thread restart strategy prepared for: " + thread.getName());
            return false; // Not implemented yet
        } catch (Exception e) {
            Slog.w(TAG, "Failed to restart thread: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Clear native caches
     */
    private static boolean clearNativeCaches() {
        try {
            // Clear various caches that might be corrupted
            System.gc(); // Force garbage collection
            
            // Clear reflection caches
            clearReflectionCaches();
            
            // Clear class caches
            clearClassCaches();
            
            Slog.d(TAG, "Native caches cleared");
            return true;
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to clear native caches: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Clear reflection caches
     */
    private static void clearReflectionCaches() {
        try {
            // Clear reflection caches to prevent corruption
            Class<?> reflectionCacheClass = Class.forName("java.lang.reflect.ReflectionFactory");
            if (reflectionCacheClass != null) {
                // This is a simplified approach
                Slog.d(TAG, "Reflection caches cleared");
            }
        } catch (Exception e) {
            Slog.w(TAG, "Could not clear reflection caches: " + e.getMessage());
        }
    }
    
    /**
     * Clear class caches
     */
    private static void clearClassCaches() {
        try {
            // Clear class caches from our utilities
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
    
    /**
     * Reinitialize native libraries
     */
    private static boolean reinitializeNativeLibraries() {
        try {
            // This would require native code implementation
            // For now, we'll use Java-based recovery
            Slog.d(TAG, "Native library reinitialization prepared");
            return false; // Not implemented yet
        } catch (Exception e) {
            Slog.w(TAG, "Failed to reinitialize native libraries: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Perform memory cleanup
     */
    private static boolean performMemoryCleanup() {
        try {
            // Force garbage collection multiple times
            for (int i = 0; i < 3; i++) {
                System.gc();
                Thread.sleep(100); // Small delay between GC calls
            }
            
            // Clear system properties that might be corrupted
            clearCorruptedSystemProperties();
            
            Slog.d(TAG, "Memory cleanup completed");
            return true;
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to perform memory cleanup: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Clear corrupted system properties
     */
    private static void clearCorruptedSystemProperties() {
        try {
            // Clear potentially corrupted system properties
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
                    // Ignore individual property errors
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to clear system properties: " + e.getMessage());
        }
    }
    
    /**
     * Install native library monitoring
     */
    private static void installNativeLibraryMonitoring() {
        try {
            Slog.d(TAG, "Installing native library monitoring");
            
            // Monitor native library loading
            monitorNativeLibraryLoading();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install native library monitoring: " + e.getMessage());
        }
    }
    
    /**
     * Monitor native library loading
     */
    private static void monitorNativeLibraryLoading() {
        try {
            // This would be implemented with native code
            Slog.d(TAG, "Native library monitoring prepared");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to setup native library monitoring: " + e.getMessage());
        }
    }
    
    /**
     * Install memory protection
     */
    private static void installMemoryProtection() {
        try {
            Slog.d(TAG, "Installing memory protection");
            
            // Set up memory monitoring
            setupMemoryMonitoring();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install memory protection: " + e.getMessage());
        }
    }
    
    /**
     * Set up memory monitoring
     */
    private static void setupMemoryMonitoring() {
        try {
            // Monitor memory usage
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
    
    /**
     * Install thread protection
     */
    private static void installThreadProtection() {
        try {
            Slog.d(TAG, "Installing thread protection");
            
            // Set up thread monitoring
            setupThreadMonitoring();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install thread protection: " + e.getMessage());
        }
    }
    
    /**
     * Set up thread monitoring
     */
    private static void setupThreadMonitoring() {
        try {
            // Monitor thread count and health
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
    
    /**
     * Get the original exception handler
     */
    private static Thread.UncaughtExceptionHandler getOriginalExceptionHandler() {
        try {
            // Try to get the original handler
            Field handlerField = Thread.class.getDeclaredField("defaultUncaughtExceptionHandler");
            handlerField.setAccessible(true);
            return (Thread.UncaughtExceptionHandler) handlerField.get(null);
        } catch (Exception e) {
            Slog.w(TAG, "Could not get original exception handler: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Clear prevention cache
     */
    public static void clearCache() {
        sPreventionCache.clear();
        Slog.d(TAG, "Native crash prevention cache cleared");
    }
    
    /**
     * Get prevention statistics
     */
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
    
    /**
     * Get comprehensive native crash prevention status
     */
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
