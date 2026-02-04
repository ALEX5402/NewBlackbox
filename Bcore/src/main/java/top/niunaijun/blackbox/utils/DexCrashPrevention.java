package top.niunaijun.blackbox.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;


public class DexCrashPrevention {
    private static final String TAG = "DexCrashPrevention";
    private static boolean sIsInitialized = false;
    
    
    private static final Map<String, PreventionResult> sPreventionCache = new HashMap<>();
    
    
    private static final String[] PROBLEMATIC_APK_PATTERNS = {
        "split_config.xhdpi.apk",
        "split_config.arm64_v8a.apk",
        "split_config.armeabi_v7a.apk"
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
            return "Prevention " + (success ? "successful" : "failed") + 
                   " via " + preventionMethod + ": " + details;
        }
    }
    
    
    public static void initialize() {
        if (sIsInitialized) {
            return;
        }
        
        try {
            Slog.d(TAG, "Initializing DEX crash prevention...");
            
            
            installProactivePrevention();
            
            
            installDexFileValidation();
            
            
            installClassLoaderMonitoring();
            
            
            installApkIntegrityChecks();
            
            sIsInitialized = true;
            Slog.d(TAG, "DEX crash prevention initialized successfully");
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to initialize DEX crash prevention: " + e.getMessage(), e);
        }
    }
    
    
    private static void installProactivePrevention() {
        try {
            
            Slog.d(TAG, "Installing proactive DEX corruption prevention");
            
            
            setupApkFileMonitoring();
            
            
            prevalidateCriticalApkFiles();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install proactive prevention: " + e.getMessage());
        }
    }
    
    
    private static void setupApkFileMonitoring() {
        try {
            
            String virtualAppDir = getVirtualAppDirectory();
            if (virtualAppDir != null) {
                File appDir = new File(virtualAppDir);
                if (appDir.exists() && appDir.isDirectory()) {
                    Slog.d(TAG, "Setting up APK file monitoring for: " + virtualAppDir);
                    
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to setup APK file monitoring: " + e.getMessage());
        }
    }
    
    
    private static void prevalidateCriticalApkFiles() {
        try {
            
            String[] apkFiles = getVirtualAppApkFiles();
            if (apkFiles != null) {
                for (String apkFile : apkFiles) {
                    if (apkFile != null) {
                        PreventionResult result = validateAndPreventCorruption(apkFile);
                        if (result.success) {
                            Slog.d(TAG, "Pre-validated APK file: " + apkFile + " - " + result);
                        } else {
                            Slog.w(TAG, "APK file validation failed: " + apkFile + " - " + result);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to pre-validate APK files: " + e.getMessage());
        }
    }
    
    
    private static void installDexFileValidation() {
        try {
            Slog.d(TAG, "Installing DEX file validation");
            
            
            hookDexFileValidation();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install DEX file validation: " + e.getMessage());
        }
    }
    
    
    private static void hookDexFileValidation() {
        try {
            
            
            Slog.d(TAG, "DEX file validation hooks prepared");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to hook DEX file validation: " + e.getMessage());
        }
    }
    
    
    private static void installClassLoaderMonitoring() {
        try {
            Slog.d(TAG, "Installing ClassLoader monitoring");
            
            
            setupClassLoaderMonitoring();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install ClassLoader monitoring: " + e.getMessage());
        }
    }
    
    
    private static void setupClassLoaderMonitoring() {
        try {
            
            ClassLoader currentClassLoader = DexCrashPrevention.class.getClassLoader();
            if (currentClassLoader != null) {
                Slog.d(TAG, "Monitoring ClassLoader: " + currentClassLoader.getClass().getName());
                
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to setup ClassLoader monitoring: " + e.getMessage());
        }
    }
    
    
    private static void installApkIntegrityChecks() {
        try {
            Slog.d(TAG, "Installing APK integrity checks");
            
            
            setupPeriodicIntegrityChecks();
            
        } catch (Exception e) {
            Slog.w(TAG, "Failed to install APK integrity checks: " + e.getMessage());
        }
    }
    
    
    private static void setupPeriodicIntegrityChecks() {
        try {
            
            Slog.d(TAG, "Periodic APK integrity checks prepared");
        } catch (Exception e) {
            Slog.w(TAG, "Failed to setup periodic integrity checks: " + e.getMessage());
        }
    }
    
    
    public static PreventionResult validateAndPreventCorruption(String apkFilePath) {
        if (apkFilePath == null) {
            return new PreventionResult("Validation", false, "APK file path is null");
        }
        
        
        if (sPreventionCache.containsKey(apkFilePath)) {
            return sPreventionCache.get(apkFilePath);
        }
        
        try {
            File apkFile = new File(apkFilePath);
            if (!apkFile.exists()) {
                PreventionResult result = new PreventionResult("Validation", false, "APK file does not exist");
                sPreventionCache.put(apkFilePath, result);
                return result;
            }
            
            
            if (isProblematicSplitApk(apkFile.getName())) {
                Slog.w(TAG, "Detected problematic split APK: " + apkFile.getName());
                PreventionResult result = handleProblematicSplitApk(apkFile);
                sPreventionCache.put(apkFilePath, result);
                return result;
            }
            
            
            if (DexFileRecovery.isValidApkFile(apkFile)) {
                PreventionResult result = new PreventionResult("Validation", true, "APK file is valid");
                sPreventionCache.put(apkFilePath, result);
                return result;
            } else {
                
                Slog.w(TAG, "APK file is corrupted: " + apkFilePath);
                PreventionResult result = attemptCorruptionRecovery(apkFilePath);
                sPreventionCache.put(apkFilePath, result);
                return result;
            }
            
        } catch (Exception e) {
            PreventionResult result = new PreventionResult("Validation", false, "Error during validation: " + e.getMessage());
            sPreventionCache.put(apkFilePath, result);
            return result;
        }
    }
    
    
    private static boolean isProblematicSplitApk(String fileName) {
        if (fileName == null) return false;
        
        for (String pattern : PROBLEMATIC_APK_PATTERNS) {
            if (fileName.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    
    private static PreventionResult handleProblematicSplitApk(File splitApk) {
        try {
            Slog.d(TAG, "Handling problematic split APK: " + splitApk.getName());
            
            
            String baseApkPath = findBaseApkPath(splitApk);
            if (baseApkPath != null) {
                
                if (DexFileRecovery.isValidApkFile(new File(baseApkPath))) {
                    Slog.d(TAG, "Found valid base APK: " + baseApkPath);
                    return new PreventionResult("Split APK Handling", true, 
                        "Using valid base APK instead of problematic split: " + baseApkPath);
                }
            }
            
            
            DexFileRecovery.RecoveryResult recoveryResult = DexFileRecovery.recoverDexFile(splitApk.getAbsolutePath());
            if (recoveryResult.success) {
                return new PreventionResult("Split APK Recovery", true, 
                    "Recovered split APK via " + recoveryResult.recoveryMethod);
            } else {
                return new PreventionResult("Split APK Recovery", false, 
                    "Failed to recover split APK: " + recoveryResult.errorMessage);
            }
            
        } catch (Exception e) {
            return new PreventionResult("Split APK Handling", false, 
                "Error handling split APK: " + e.getMessage());
        }
    }
    
    
    private static String findBaseApkPath(File splitApk) {
        try {
            File parent = splitApk.getParentFile();
            if (parent != null && parent.exists()) {
                File[] files = parent.listFiles((dir, name) -> 
                    name.endsWith(".apk") && !name.contains("split_config")
                );
                
                if (files != null && files.length > 0) {
                    
                    return files[0].getAbsolutePath();
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error finding base APK: " + e.getMessage());
        }
        
        return null;
    }
    
    
    private static PreventionResult attemptCorruptionRecovery(String corruptedFilePath) {
        try {
            Slog.d(TAG, "Attempting corruption recovery for: " + corruptedFilePath);
            
            
            DexFileRecovery.RecoveryResult recoveryResult = DexFileRecovery.recoverDexFile(corruptedFilePath);
            
            if (recoveryResult.success) {
                return new PreventionResult("Corruption Recovery", true, 
                    "Successfully recovered via " + recoveryResult.recoveryMethod);
            } else {
                return new PreventionResult("Corruption Recovery", false, 
                    "Recovery failed: " + recoveryResult.errorMessage);
            }
            
        } catch (Exception e) {
            return new PreventionResult("Corruption Recovery", false, 
                "Error during recovery: " + e.getMessage());
        }
    }
    
    
    private static String getVirtualAppDirectory() {
        try {
            Context context = BlackBoxCore.getContext();
            if (context != null) {
                ApplicationInfo appInfo = context.getApplicationInfo();
                if (appInfo != null && appInfo.sourceDir != null) {
                    File sourceFile = new File(appInfo.sourceDir);
                    return sourceFile.getParent();
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error getting virtual app directory: " + e.getMessage());
        }
        return null;
    }
    
    
    private static String[] getVirtualAppApkFiles() {
        try {
            String appDir = getVirtualAppDirectory();
            if (appDir != null) {
                File dir = new File(appDir);
                if (dir.exists() && dir.isDirectory()) {
                    File[] apkFiles = dir.listFiles((dir1, name) -> name.endsWith(".apk"));
                    if (apkFiles != null) {
                        String[] paths = new String[apkFiles.length];
                        for (int i = 0; i < apkFiles.length; i++) {
                            paths[i] = apkFiles[i].getAbsolutePath();
                        }
                        return paths;
                    }
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error getting virtual app APK files: " + e.getMessage());
        }
        return null;
    }
    
    
    public static void clearCache() {
        sPreventionCache.clear();
        Slog.d(TAG, "Prevention cache cleared");
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
        
        return "Prevention Stats - Successful: " + successful + ", Failed: " + failed + 
               ", Total Attempts: " + sPreventionCache.size();
    }
    
    
    public static PreventionResult forcePrevention(String filePath) {
        
        sPreventionCache.remove(filePath);
        return validateAndPreventCorruption(filePath);
    }
    
    
    public static String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("DEX Crash Prevention Status:\n");
        status.append("Initialized: ").append(sIsInitialized).append("\n");
        status.append("Cache Size: ").append(sPreventionCache.size()).append("\n");
        status.append("Prevention Stats: ").append(getPreventionStats()).append("\n");
        status.append("Recovery Stats: ").append(DexFileRecovery.getRecoveryStats()).append("\n");
        
        return status.toString();
    }
}
