package top.niunaijun.blackbox.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;

/**
 * Comprehensive DEX file recovery utility to handle "classes.dex: Entry not found" errors.
 * This class provides multiple recovery strategies for corrupted or missing DEX files.
 */
public class DexFileRecovery {
    private static final String TAG = "DexFileRecovery";
    
    // Cache for recovery attempts to avoid repeated work
    private static final Map<String, RecoveryResult> sRecoveryCache = new HashMap<>();
    
    // Recovery strategies
    private static final List<RecoveryStrategy> sRecoveryStrategies = new ArrayList<>();
    
    // Maximum recovery attempts per file
    private static final int MAX_RECOVERY_ATTEMPTS = 3;
    
    static {
        initializeRecoveryStrategies();
    }
    
    /**
     * Recovery result containing the recovered file path and method used
     */
    public static class RecoveryResult {
        public final String recoveredFilePath;
        public final String recoveryMethod;
        public final boolean success;
        public final String errorMessage;
        
        public RecoveryResult(String recoveredFilePath, String recoveryMethod) {
            this.recoveredFilePath = recoveredFilePath;
            this.recoveryMethod = recoveryMethod;
            this.success = recoveredFilePath != null;
            this.errorMessage = null;
        }
        
        public RecoveryResult(String errorMessage) {
            this.recoveredFilePath = null;
            this.recoveryMethod = null;
            this.success = false;
            this.errorMessage = errorMessage;
        }
        
        @Override
        public String toString() {
            if (success) {
                return "Recovery successful via " + recoveryMethod + ": " + recoveredFilePath;
            } else {
                return "Recovery failed: " + errorMessage;
            }
        }
    }
    
    /**
     * Recovery strategy interface
     */
    public interface RecoveryStrategy {
        String getName();
        RecoveryResult attemptRecovery(String corruptedFilePath);
        int getPriority(); // Higher priority strategies are tried first
    }
    
    /**
     * Initialize all available recovery strategies
     */
    private static void initializeRecoveryStrategies() {
        sRecoveryStrategies.add(new AlternativeApkStrategy());
        sRecoveryStrategies.add(new HostAppApkStrategy());
        sRecoveryStrategies.add(new SystemApkStrategy());
        sRecoveryStrategies.add(new DexExtractionStrategy());
        sRecoveryStrategies.add(new BackupRestoreStrategy());
        
        // Sort by priority (highest first)
        sRecoveryStrategies.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        
        Slog.d(TAG, "Initialized " + sRecoveryStrategies.size() + " recovery strategies");
    }
    
    /**
     * Attempt to recover a corrupted DEX file
     */
    public static RecoveryResult recoverDexFile(String corruptedFilePath) {
        if (corruptedFilePath == null) {
            return new RecoveryResult("Corrupted file path is null");
        }
        
        // Check cache first
        if (sRecoveryCache.containsKey(corruptedFilePath)) {
            RecoveryResult cached = sRecoveryCache.get(corruptedFilePath);
            Slog.d(TAG, "Using cached recovery result for " + corruptedFilePath + ": " + cached);
            return cached;
        }
        
        Slog.w(TAG, "Attempting to recover corrupted DEX file: " + corruptedFilePath);
        
        // Try each recovery strategy in order of priority
        for (RecoveryStrategy strategy : sRecoveryStrategies) {
            try {
                Slog.d(TAG, "Trying recovery strategy: " + strategy.getName());
                RecoveryResult result = strategy.attemptRecovery(corruptedFilePath);
                
                if (result.success) {
                    Slog.d(TAG, "Recovery successful via " + strategy.getName() + ": " + result.recoveredFilePath);
                    sRecoveryCache.put(corruptedFilePath, result);
                    return result;
                } else {
                    Slog.w(TAG, "Recovery failed via " + strategy.getName() + ": " + result.errorMessage);
                }
            } catch (Exception e) {
                Slog.w(TAG, "Recovery strategy " + strategy.getName() + " threw exception: " + e.getMessage());
            }
        }
        
        // All strategies failed
        RecoveryResult failure = new RecoveryResult("All recovery strategies failed for " + corruptedFilePath);
        sRecoveryCache.put(corruptedFilePath, failure);
        return failure;
    }
    
    /**
     * Strategy 1: Find alternative APK files in the same directory
     */
    private static class AlternativeApkStrategy implements RecoveryStrategy {
        @Override
        public String getName() {
            return "Alternative APK";
        }
        
        @Override
        public int getPriority() {
            return 100; // Highest priority
        }
        
        @Override
        public RecoveryResult attemptRecovery(String corruptedFilePath) {
            try {
                File corruptedFile = new File(corruptedFilePath);
                File parent = corruptedFile.getParentFile();
                
                if (parent != null && parent.exists()) {
                    File[] files = parent.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().endsWith(".apk") && 
                                file.length() > 1000000 && // > 1MB
                                !file.getAbsolutePath().equals(corruptedFilePath)) {
                                
                                // Validate this APK file
                                if (isValidApkFile(file)) {
                                    Slog.d(TAG, "Found valid alternative APK: " + file.getAbsolutePath());
                                    return new RecoveryResult(file.getAbsolutePath(), getName());
                                }
                            }
                        }
                    }
                }
                
                return new RecoveryResult("No valid alternative APK files found");
            } catch (Exception e) {
                return new RecoveryResult("Error in alternative APK strategy: " + e.getMessage());
            }
        }
    }
    
    /**
     * Strategy 2: Use host app's APK as fallback
     */
    private static class HostAppApkStrategy implements RecoveryStrategy {
        @Override
        public String getName() {
            return "Host App APK";
        }
        
        @Override
        public int getPriority() {
            return 80;
        }
        
        @Override
        public RecoveryResult attemptRecovery(String corruptedFilePath) {
            try {
                Context hostContext = BlackBoxCore.getContext();
                if (hostContext != null) {
                    ApplicationInfo appInfo = hostContext.getApplicationInfo();
                    if (appInfo != null && appInfo.sourceDir != null) {
                        File hostApk = new File(appInfo.sourceDir);
                        if (hostApk.exists() && isValidApkFile(hostApk)) {
                            Slog.d(TAG, "Using host app APK as fallback: " + appInfo.sourceDir);
                            return new RecoveryResult(appInfo.sourceDir, getName());
                        }
                    }
                }
                
                return new RecoveryResult("Host app APK not available or invalid");
            } catch (Exception e) {
                return new RecoveryResult("Error in host app APK strategy: " + e.getMessage());
            }
        }
    }
    
    /**
     * Strategy 3: Use system APK files as fallback
     */
    private static class SystemApkStrategy implements RecoveryStrategy {
        @Override
        public String getName() {
            return "System APK";
        }
        
        @Override
        public int getPriority() {
            return 60;
        }
        
        @Override
        public RecoveryResult attemptRecovery(String corruptedFilePath) {
            try {
                // Try to find system APK files
                String[] systemPaths = {
                    "/system/app",
                    "/system/priv-app",
                    "/vendor/app",
                    "/product/app"
                };
                
                for (String systemPath : systemPaths) {
                    File systemDir = new File(systemPath);
                    if (systemDir.exists() && systemDir.isDirectory()) {
                        File[] files = systemDir.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                if (file.isDirectory()) {
                                    File[] apkFiles = file.listFiles((dir, name) -> name.endsWith(".apk"));
                                    if (apkFiles != null && apkFiles.length > 0) {
                                        File apkFile = apkFiles[0];
                                        if (isValidApkFile(apkFile)) {
                                            Slog.d(TAG, "Found valid system APK: " + apkFile.getAbsolutePath());
                                            return new RecoveryResult(apkFile.getAbsolutePath(), getName());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                return new RecoveryResult("No valid system APK files found");
            } catch (Exception e) {
                return new RecoveryResult("Error in system APK strategy: " + e.getMessage());
            }
        }
    }
    
    /**
     * Strategy 4: Extract DEX from corrupted APK if possible
     */
    private static class DexExtractionStrategy implements RecoveryStrategy {
        @Override
        public String getName() {
            return "DEX Extraction";
        }
        
        @Override
        public int getPriority() {
            return 40;
        }
        
        @Override
        public RecoveryResult attemptRecovery(String corruptedFilePath) {
            try {
                File corruptedFile = new File(corruptedFilePath);
                if (!corruptedFile.exists() || !corruptedFile.getName().endsWith(".apk")) {
                    return new RecoveryResult("Not an APK file or file doesn't exist");
                }
                
                // Try to extract classes.dex from the corrupted APK
                String extractedDexPath = extractDexFromApk(corruptedFile);
                if (extractedDexPath != null) {
                    Slog.d(TAG, "Successfully extracted DEX from corrupted APK: " + extractedDexPath);
                    return new RecoveryResult(extractedDexPath, getName());
                }
                
                return new RecoveryResult("Failed to extract DEX from corrupted APK");
            } catch (Exception e) {
                return new RecoveryResult("Error in DEX extraction strategy: " + e.getMessage());
            }
        }
        
        private String extractDexFromApk(File apkFile) {
            try (ZipFile zipFile = new ZipFile(apkFile)) {
                ZipEntry dexEntry = zipFile.getEntry("classes.dex");
                if (dexEntry != null) {
                    // Create extraction directory
                    File extractDir = new File(BlackBoxCore.getContext().getCacheDir(), "dex_recovery");
                    if (!extractDir.exists()) {
                        extractDir.mkdirs();
                    }
                    
                    // Extract the DEX file
                    File extractedDex = new File(extractDir, "classes_" + System.currentTimeMillis() + ".dex");
                    try (InputStream input = zipFile.getInputStream(dexEntry);
                         FileOutputStream output = new FileOutputStream(extractedDex)) {
                        
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    Slog.d(TAG, "Extracted DEX to: " + extractedDex.getAbsolutePath());
                    return extractedDex.getAbsolutePath();
                }
            } catch (Exception e) {
                Slog.w(TAG, "Failed to extract DEX from APK: " + e.getMessage());
            }
            
            return null;
        }
    }
    
    /**
     * Strategy 5: Restore from backup if available
     */
    private static class BackupRestoreStrategy implements RecoveryStrategy {
        @Override
        public String getName() {
            return "Backup Restore";
        }
        
        @Override
        public int getPriority() {
            return 20; // Lowest priority
        }
        
        @Override
        public RecoveryResult attemptRecovery(String corruptedFilePath) {
            try {
                // Look for backup files
                String backupPath = findBackupFile(corruptedFilePath);
                if (backupPath != null && isValidApkFile(new File(backupPath))) {
                    Slog.d(TAG, "Found valid backup file: " + backupPath);
                    return new RecoveryResult(backupPath, getName());
                }
                
                return new RecoveryResult("No valid backup files found");
            } catch (Exception e) {
                return new RecoveryResult("Error in backup restore strategy: " + e.getMessage());
            }
        }
        
        private String findBackupFile(String originalPath) {
            try {
                File original = new File(originalPath);
                File parent = original.getParentFile();
                String baseName = original.getName().replaceFirst("[.][^.]+$", ""); // Remove extension
                
                if (parent != null && parent.exists()) {
                    File[] files = parent.listFiles((dir, name) -> 
                        name.startsWith(baseName) && 
                        (name.endsWith(".bak") || name.endsWith(".backup") || name.endsWith(".old"))
                    );
                    
                    if (files != null && files.length > 0) {
                        // Return the most recent backup
                        File mostRecent = files[0];
                        for (File file : files) {
                            if (file.lastModified() > mostRecent.lastModified()) {
                                mostRecent = file;
                            }
                        }
                        return mostRecent.getAbsolutePath();
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "Error finding backup file: " + e.getMessage());
            }
            
            return null;
        }
    }
    
    /**
     * Validate if an APK file is valid and contains classes.dex
     */
    public static boolean isValidApkFile(File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            return false;
        }
        
        try {
            // Check file size (APK should be at least 1MB)
            if (apkFile.length() < 1000000) {
                return false;
            }
            
            // Check if it's a valid ZIP file containing classes.dex
            try (ZipFile zipFile = new ZipFile(apkFile)) {
                ZipEntry dexEntry = zipFile.getEntry("classes.dex");
                return dexEntry != null && dexEntry.getSize() > 0;
            } catch (Exception e) {
                Slog.w(TAG, "APK validation failed for " + apkFile.getAbsolutePath() + ": " + e.getMessage());
                return false;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error validating APK file " + apkFile.getAbsolutePath() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Clear recovery cache
     */
    public static void clearCache() {
        sRecoveryCache.clear();
        Slog.d(TAG, "Recovery cache cleared");
    }
    
    /**
     * Get recovery statistics
     */
    public static String getRecoveryStats() {
        int successful = 0;
        int failed = 0;
        
        for (RecoveryResult result : sRecoveryCache.values()) {
            if (result.success) {
                successful++;
            } else {
                failed++;
            }
        }
        
        return "Recovery Stats - Successful: " + successful + ", Failed: " + failed + 
               ", Total Attempts: " + sRecoveryCache.size();
    }
    
    /**
     * Force recovery attempt for a specific file
     */
    public static RecoveryResult forceRecovery(String filePath) {
        // Clear cache for this file to force retry
        sRecoveryCache.remove(filePath);
        return recoverDexFile(filePath);
    }
}
