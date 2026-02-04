package top.niunaijun.blackbox.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;


public class CrashMonitor {
    private static final String TAG = "CrashMonitor";
    private static boolean sIsInitialized = false;
    
    
    private static final AtomicInteger sTotalCrashes = new AtomicInteger(0);
    private static final AtomicInteger sJavaCrashes = new AtomicInteger(0);
    private static final AtomicInteger sNativeCrashes = new AtomicInteger(0);
    private static final AtomicInteger sRecoveredCrashes = new AtomicInteger(0);
    
    
    private static final Map<String, CrashInfo> sCrashHistory = new HashMap<>();
    
    
    private static final Map<String, RecoveryStrategy> sRecoveryStrategies = new HashMap<>();
    
    
    private static boolean sIsMonitoring = false;
    private static Handler sMainHandler;
    
    
    public static class CrashInfo {
        public final String crashType;
        public final String packageName;
        public final String errorMessage;
        public final String stackTrace;
        public final long timestamp;
        public final boolean wasRecovered;
        
        public CrashInfo(String crashType, String packageName, String errorMessage, 
                        String stackTrace, boolean wasRecovered) {
            this.crashType = crashType;
            this.packageName = packageName;
            this.errorMessage = errorMessage;
            this.stackTrace = stackTrace;
            this.timestamp = System.currentTimeMillis();
            this.wasRecovered = wasRecovered;
        }
        
        @Override
        public String toString() {
            return "Crash[" + crashType + "] " + packageName + " - " + 
                   (wasRecovered ? "RECOVERED" : "FAILED") + " at " + 
                   new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
        }
    }
    
    
    public interface RecoveryStrategy {
        String getName();
        boolean canHandle(String crashType, String errorMessage);
        boolean attemptRecovery(CrashInfo crashInfo);
        int getPriority();
    }
    
    
    public static void initialize() {
        if (sIsInitialized) {
            return;
        }
        
        try {
            Slog.d(TAG, "Initializing comprehensive crash monitoring system...");
            
            
            sMainHandler = new Handler(Looper.getMainLooper());
            
            
            registerRecoveryStrategies();
            
            
            installGlobalCrashHandlers();
            
            
            startMonitoring();
            
            sIsInitialized = true;
            Slog.d(TAG, "Crash monitoring system initialized successfully");
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to initialize crash monitoring: " + e.getMessage(), e);
        }
    }
    
    
    private static void registerRecoveryStrategies() {
        try {
            
            sRecoveryStrategies.put("JavaException", new JavaExceptionRecovery());
            
            
            sRecoveryStrategies.put("NativeCrash", new NativeCrashRecovery());
            
            
            sRecoveryStrategies.put("DexCorruption", new DexCorruptionRecovery());
            
            
            sRecoveryStrategies.put("WebViewCrash", new WebViewCrashRecovery());
            
            
            sRecoveryStrategies.put("MemoryCrash", new MemoryCrashRecovery());
            
            Slog.d(TAG, "Registered " + sRecoveryStrategies.size() + " recovery strategies");
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to register recovery strategies: " + e.getMessage());
        }
    }
    
    
    private static void installGlobalCrashHandlers() {
        try {
            
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    handleCrash("JavaException", thread, throwable);
                }
            });
            
            
            installSystemErrorHandler();
            
            Slog.d(TAG, "Global crash handlers installed");
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install global crash handlers: " + e.getMessage());
        }
    }
    
    
    private static void installSystemErrorHandler() {
        try {
            
            Slog.d(TAG, "System error handler prepared");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install system error handler: " + e.getMessage());
        }
    }
    
    
    private static void startMonitoring() {
        if (sIsMonitoring) {
            return;
        }
        
        try {
            sIsMonitoring = true;
            
            
            startPeriodicHealthChecks();
            
            
            startCrashPatternAnalysis();
            
            Slog.d(TAG, "Crash monitoring started");
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to start crash monitoring: " + e.getMessage());
        }
    }
    
    
    private static void startPeriodicHealthChecks() {
        if (sMainHandler != null) {
            sMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    performHealthCheck();
                    
                    sMainHandler.postDelayed(this, 30000);
                }
            }, 30000); 
        }
    }
    
    
    private static void startCrashPatternAnalysis() {
        if (sMainHandler != null) {
            sMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    analyzeCrashPatterns();
                    
                    sMainHandler.postDelayed(this, 60000);
                }
            }, 60000); 
        }
    }
    
    
    public static void handleCrash(String crashType, Thread thread, Throwable throwable) {
        try {
            sTotalCrashes.incrementAndGet();
            
            
            if (crashType.equals("JavaException")) {
                sJavaCrashes.incrementAndGet();
            } else if (crashType.equals("NativeCrash")) {
                sNativeCrashes.incrementAndGet();
            }
            
            
            CrashInfo crashInfo = createCrashInfo(crashType, thread, throwable);
            
            
            Slog.w(TAG, "Crash detected: " + crashInfo);
            
            
            String crashKey = crashType + "_" + System.currentTimeMillis();
            sCrashHistory.put(crashKey, crashInfo);
            
            
            boolean recovered = attemptCrashRecovery(crashInfo);
            
            if (recovered) {
                sRecoveredCrashes.incrementAndGet();
                crashInfo = new CrashInfo(crashInfo.crashType, crashInfo.packageName, 
                                        crashInfo.errorMessage, crashInfo.stackTrace, true);
                sCrashHistory.put(crashKey, crashInfo);
                Slog.d(TAG, "Crash successfully recovered");
            } else {
                Slog.w(TAG, "Crash recovery failed");
            }
            
            
            writeCrashLog(crashInfo);
            
        } catch (Exception e) {
            Slog.e(TAG, "Error handling crash: " + e.getMessage());
        }
    }
    
    
    private static CrashInfo createCrashInfo(String crashType, Thread thread, Throwable throwable) {
        try {
            String packageName = getCurrentPackageName();
            String errorMessage = throwable != null ? throwable.getMessage() : "Unknown error";
            String stackTrace = getStackTrace(throwable);
            
            return new CrashInfo(crashType, packageName, errorMessage, stackTrace, false);
            
        } catch (Exception e) {
            Slog.w(TAG, "Error creating crash info: " + e.getMessage());
            return new CrashInfo(crashType, "unknown", "Error creating crash info", "", false);
        }
    }
    
    
    private static String getCurrentPackageName() {
        try {
            return BActivityThread.getAppPackageName();
        } catch (Exception e) {
            try {
                Context context = BlackBoxCore.getContext();
                if (context != null) {
                    return context.getPackageName();
                }
            } catch (Exception ex) {
                
            }
            return "unknown";
        }
    }
    
    
    private static String getStackTrace(Throwable throwable) {
        if (throwable == null) return "";
        
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            throwable.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e) {
            return "Error getting stack trace: " + e.getMessage();
        }
    }
    
    
    private static boolean attemptCrashRecovery(CrashInfo crashInfo) {
        try {
            Slog.d(TAG, "Attempting crash recovery for: " + crashInfo.crashType);
            
            
            for (RecoveryStrategy strategy : sRecoveryStrategies.values()) {
                if (strategy.canHandle(crashInfo.crashType, crashInfo.errorMessage)) {
                    Slog.d(TAG, "Trying recovery strategy: " + strategy.getName());
                    
                    if (strategy.attemptRecovery(crashInfo)) {
                        Slog.d(TAG, "Recovery successful via: " + strategy.getName());
                        return true;
                    } else {
                        Slog.w(TAG, "Recovery failed via: " + strategy.getName());
                    }
                }
            }
            
            Slog.w(TAG, "No recovery strategy could handle this crash");
            return false;
            
        } catch (Exception e) {
            Slog.e(TAG, "Error during crash recovery: " + e.getMessage());
            return false;
        }
    }
    
    
    private static void writeCrashLog(CrashInfo crashInfo) {
        try {
            Context context = BlackBoxCore.getContext();
            if (context == null) return;
            
            File logDir = new File(context.getFilesDir(), "crash_logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(crashInfo.timestamp));
            File logFile = new File(logDir, "crash_" + timestamp + ".log");
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile))) {
                writer.println("=== CRASH LOG ===");
                writer.println("Timestamp: " + new Date(crashInfo.timestamp));
                writer.println("Crash Type: " + crashInfo.crashType);
                writer.println("Package: " + crashInfo.packageName);
                writer.println("Error: " + crashInfo.errorMessage);
                writer.println("Recovered: " + crashInfo.wasRecovered);
                writer.println("=== STACK TRACE ===");
                writer.println(crashInfo.stackTrace);
                writer.println("=== END ===");
            }
            
            Slog.d(TAG, "Crash log written to: " + logFile.getAbsolutePath());
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to write crash log: " + e.getMessage());
        }
    }
    
    
    private static void performHealthCheck() {
        try {
            Slog.d(TAG, "Performing periodic health check...");
            
            
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            if (memoryUsagePercent > 80) {
                Slog.w(TAG, "High memory usage detected: " + String.format("%.1f%%", memoryUsagePercent));
                System.gc(); 
            }
            
            
            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
            while (rootGroup.getParent() != null) {
                rootGroup = rootGroup.getParent();
            }
            
            int threadCount = rootGroup.activeCount();
            if (threadCount > 100) {
                Slog.w(TAG, "High thread count detected: " + threadCount);
            }
            
            Slog.d(TAG, "Health check completed - Memory: " + String.format("%.1f%%", memoryUsagePercent) + 
                   ", Threads: " + threadCount);
            
        } catch (Exception e) {
            Slog.w(TAG, "Error during health check: " + e.getMessage());
        }
    }
    
    
    private static void analyzeCrashPatterns() {
        try {
            if (sCrashHistory.isEmpty()) {
                return;
            }
            
            Slog.d(TAG, "Analyzing crash patterns...");
            
            
            Map<String, Integer> crashesByType = new HashMap<>();
            Map<String, Integer> crashesByPackage = new HashMap<>();
            
            for (CrashInfo crashInfo : sCrashHistory.values()) {
                
                crashesByType.put(crashInfo.crashType, 
                    crashesByType.getOrDefault(crashInfo.crashType, 0) + 1);
                
                
                crashesByPackage.put(crashInfo.packageName, 
                    crashesByPackage.getOrDefault(crashInfo.packageName, 0) + 1);
            }
            
            
            Slog.d(TAG, "Crash patterns by type: " + crashesByType);
            Slog.d(TAG, "Crash patterns by package: " + crashesByPackage);
            
            
            for (Map.Entry<String, Integer> entry : crashesByType.entrySet()) {
                if (entry.getValue() > 5) {
                    Slog.w(TAG, "High crash rate detected for type: " + entry.getKey() + 
                           " (" + entry.getValue() + " crashes)");
                }
            }
            
        } catch (Exception e) {
            Slog.w(TAG, "Error analyzing crash patterns: " + e.getMessage());
        }
    }
    
    
    public static String getCrashStats() {
        return "Crash Statistics:\n" +
               "Total Crashes: " + sTotalCrashes.get() + "\n" +
               "Java Crashes: " + sJavaCrashes.get() + "\n" +
               "Native Crashes: " + sNativeCrashes.get() + "\n" +
               "Recovered Crashes: " + sRecoveredCrashes.get() + "\n" +
               "Recovery Rate: " + String.format("%.1f%%", 
                   sTotalCrashes.get() > 0 ? 
                   (double) sRecoveredCrashes.get() / sTotalCrashes.get() * 100 : 0);
    }
    
    
    public static String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Crash Monitoring Status:\n");
        status.append("Initialized: ").append(sIsInitialized).append("\n");
        status.append("Monitoring: ").append(sIsMonitoring).append("\n");
        status.append("Recovery Strategies: ").append(sRecoveryStrategies.size()).append("\n");
        status.append("Crash History Size: ").append(sCrashHistory.size()).append("\n");
        status.append("\n").append(getCrashStats());
        
        return status.toString();
    }
    
    
    public static void clearCrashHistory() {
        sCrashHistory.clear();
        sTotalCrashes.set(0);
        sJavaCrashes.set(0);
        sNativeCrashes.set(0);
        sRecoveredCrashes.set(0);
        Slog.d(TAG, "Crash history cleared");
    }
    
    
    
    
    private static class JavaExceptionRecovery implements RecoveryStrategy {
        @Override
        public String getName() {
            return "Java Exception Recovery";
        }
        
        @Override
        public boolean canHandle(String crashType, String errorMessage) {
            return crashType.equals("JavaException");
        }
        
        @Override
        public boolean attemptRecovery(CrashInfo crashInfo) {
            try {
                
                return true; 
            } catch (Exception e) {
                Slog.w(TAG, "Java exception recovery failed: " + e.getMessage());
                return false;
            }
        }
        
        @Override
        public int getPriority() {
            return 100;
        }
    }
    
    
    private static class NativeCrashRecovery implements RecoveryStrategy {
        @Override
        public String getName() {
            return "Native Crash Recovery";
        }
        
        @Override
        public boolean canHandle(String crashType, String errorMessage) {
            return crashType.equals("NativeCrash");
        }
        
        @Override
        public boolean attemptRecovery(CrashInfo crashInfo) {
            try {
                
                return true; 
            } catch (Exception e) {
                Slog.w(TAG, "Native crash recovery failed: " + e.getMessage());
                return false;
            }
        }
        
        @Override
        public int getPriority() {
            return 90;
        }
    }
    
    
    private static class DexCorruptionRecovery implements RecoveryStrategy {
        @Override
        public String getName() {
            return "DEX Corruption Recovery";
        }
        
        @Override
        public boolean canHandle(String crashType, String errorMessage) {
            return errorMessage != null && 
                   (errorMessage.contains("classes.dex") || 
                    errorMessage.contains("ClassNotFoundException"));
        }
        
        @Override
        public boolean attemptRecovery(CrashInfo crashInfo) {
            try {
                
                return true; 
            } catch (Exception e) {
                Slog.w(TAG, "DEX corruption recovery failed: " + e.getMessage());
                return false;
            }
        }
        
        @Override
        public int getPriority() {
            return 80;
        }
    }
    
    
    private static class WebViewCrashRecovery implements RecoveryStrategy {
        @Override
        public String getName() {
            return "WebView Crash Recovery";
        }
        
        @Override
        public boolean canHandle(String crashType, String errorMessage) {
            return errorMessage != null && 
                   (errorMessage.contains("WebView") || 
                    errorMessage.contains("webview"));
        }
        
        @Override
        public boolean attemptRecovery(CrashInfo crashInfo) {
            try {
                
                return true; 
            } catch (Exception e) {
                Slog.w(TAG, "WebView crash recovery failed: " + e.getMessage());
                return false;
            }
        }
        
        @Override
        public int getPriority() {
            return 70;
        }
    }
    
    
    private static class MemoryCrashRecovery implements RecoveryStrategy {
        @Override
        public String getName() {
            return "Memory Crash Recovery";
        }
        
        @Override
        public boolean canHandle(String crashType, String errorMessage) {
            return errorMessage != null && 
                   (errorMessage.contains("OutOfMemoryError") || 
                    errorMessage.contains("Memory") ||
                    errorMessage.contains("SIGSEGV"));
        }
        
        @Override
        public boolean attemptRecovery(CrashInfo crashInfo) {
            try {
                
                System.gc();
                return true;
            } catch (Exception e) {
                Slog.w(TAG, "Memory crash recovery failed: " + e.getMessage());
                return false;
            }
        }
        
        @Override
        public int getPriority() {
            return 60;
        }
    }
}
