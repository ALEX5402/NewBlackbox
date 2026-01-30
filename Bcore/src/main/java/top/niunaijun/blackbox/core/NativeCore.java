package top.niunaijun.blackbox.core;


import android.os.Process;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.File;
import java.util.List;

import dalvik.system.DexFile;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.utils.compat.DexFileCompat;

import top.niunaijun.blackbox.core.system.JarManager;
import top.niunaijun.blackbox.utils.Slog;

/**
 * updated by alex5402 on 4/9/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 
 */
public class NativeCore {
    public static final String TAG = "NativeCore";

    static {

        System.loadLibrary("blackbox");
    }

    public static native void init(int apiLevel);

    public static native void enableIO();

    public static native void addIORule(String targetPath, String relocatePath);

    public static native void hideXposed();

    public static native boolean disableHiddenApi();
    
    public static native boolean disableResourceLoading();


    @Keep
    public static int getCallingUid(int origCallingUid) {
        try {
            // System UIDs should remain unchanged
            if (origCallingUid > 0 && origCallingUid < Process.FIRST_APPLICATION_UID)
                return origCallingUid;
            // Non-user app UIDs should remain unchanged
            if (origCallingUid > Process.LAST_APPLICATION_UID)
                return origCallingUid;

            if (origCallingUid == BlackBoxCore.getHostUid()) {
                //microG
                String appPackageName = BlackBoxCore.getAppPackageName();
                if (appPackageName != null && appPackageName.equals("com.google.android.gms")){
                    //return Process.ROOT_UID;
                }
                //webview WV.qE Process.myUid()函数没有做hook所以只能特殊处理
                //if (Binder.getCallingUid() == Process.myUid()) {
                //throw new SecurityException("recordMetrics() may only be called by non-embedded WebView processes");
                if (appPackageName != null && appPackageName.equals("com.google.android.webview")){
                    return Process.myUid();
                }
                
                // Enhanced UID handling for sandboxed environments
                try {
                    int callingBUid = BlackBoxCore.getCallingBUid();
                    if (callingBUid > 0 && callingBUid < Process.LAST_APPLICATION_UID) {
                        return callingBUid;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error getting calling BUid: " + e.getMessage());
                }
                
                // Special handling for system settings access to prevent UID mismatch
                try {
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    for (StackTraceElement element : stackTrace) {
                        String className = element.getClassName();
                        String methodName = element.getMethodName();
                        
                        // Check if this is a system settings access that might cause UID issues
                        if ((className.contains("Settings") || className.contains("FeatureFlag")) &&
                            (methodName.contains("getString") || methodName.contains("getInt") || 
                             methodName.contains("getLong") || methodName.contains("getFloat"))) {
                            
                            Log.d(TAG, "System settings access detected, using system UID to prevent mismatch");
                            // Return a system UID to prevent SecurityException
                            return Process.SYSTEM_UID; // UID 1000
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error analyzing stack trace for UID resolution: " + e.getMessage());
                }
                
                // Fallback to host UID if calling BUid is invalid
                return BlackBoxCore.getHostUid();
            }
            return origCallingUid;
        } catch (Exception e) {
            Log.e(TAG, "Error in getCallingUid: " + e.getMessage());
            // Return a safe fallback UID
            return Process.myUid();
        }
    }

    @Keep
    public static String redirectPath(String path) {
        return IOCore.get().redirectPath(path);
    }

    @Keep
    public static File redirectPath(File path) {
        return IOCore.get().redirectPath(path);
    }

    @Keep
    public static long[] loadEmptyDex() {
        try {
            File emptyJar = JarManager.getInstance().getEmptyJar();
            if (emptyJar == null) {
                Log.w(TAG, "Empty JAR not available, attempting sync initialization");
                JarManager.getInstance().initializeSync();
                emptyJar = JarManager.getInstance().getEmptyJar();
            }
            
            if (emptyJar == null || !emptyJar.exists()) {
                Log.e(TAG, "Empty JAR file not found or invalid");
                return new long[]{};
            }
            
            DexFile dexFile = new DexFile(emptyJar);
            List<Long> cookies = DexFileCompat.getCookies(dexFile);
            long[] longs = new long[cookies.size()];
            for (int i = 0; i < cookies.size(); i++) {
                longs[i] = cookies.get(i);
            }
            Log.d(TAG, "Successfully loaded empty DEX with " + cookies.size() + " cookies");
            return longs;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load empty DEX", e);
        }
        return new long[]{};
    }
    
    /**
     * Create a fallback empty DEX when the main one fails
     */
    private static long[] createFallbackEmptyDex() {
        try {
            Slog.d(TAG, "Creating fallback empty DEX");
            
            // Create a minimal empty DEX file in memory
            // This is a simplified approach that should work for most cases
            byte[] emptyDexBytes = createMinimalDexBytes();
            
            // Write to a temporary file
            File tempDexFile = File.createTempFile("fallback_empty", ".dex");
            tempDexFile.deleteOnExit();
            
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempDexFile);
            fos.write(emptyDexBytes);
            fos.close();
            
            // Load the temporary DEX file
            DexFile dexFile = new DexFile(tempDexFile);
            List<Long> cookies = DexFileCompat.getCookies(dexFile);
            
            if (cookies != null && !cookies.isEmpty()) {
                long[] longs = new long[cookies.size()];
                for (int i = 0; i < cookies.size(); i++) {
                    longs[i] = cookies.get(i);
                }
                
                Slog.d(TAG, "Successfully created fallback empty DEX with " + cookies.size() + " cookies");
                return longs;
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Error creating fallback empty DEX: " + e.getMessage());
        }
        
        // Last resort: return empty array
        Slog.w(TAG, "Returning empty DEX array as last resort");
        return new long[]{};
    }
    
    /**
     * Create minimal DEX bytes for fallback
     */
    private static byte[] createMinimalDexBytes() {
        // This is a minimal valid DEX file structure
        // In a real implementation, you would create a proper DEX file
        // For now, we'll return a simple byte array that represents a minimal DEX
        
        // Minimal DEX header (simplified)
        byte[] dexHeader = {
            'd', 'e', 'x', '\n',  // Magic
            0x30, 0x33, 0x35, 0x00,  // Version
            0x00, 0x00, 0x00, 0x00,  // Checksum
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  // Signature
            0x00, 0x00, 0x00, 0x00,  // File size
            0x70, 0x00, 0x00, 0x00,  // Header size
            0x00, 0x00, 0x00, 0x00,  // Endian tag
            0x00, 0x00, 0x00, 0x00,  // Link size
            0x00, 0x00, 0x00, 0x00,  // Link offset
            0x00, 0x00, 0x00, 0x00,  // Map offset
            0x00, 0x00, 0x00, 0x00,  // String ids size
            0x00, 0x00, 0x00, 0x00,  // String ids offset
            0x00, 0x00, 0x00, 0x00,  // Type ids size
            0x00, 0x00, 0x00, 0x00,  // Type ids offset
            0x00, 0x00, 0x00, 0x00,  // Proto ids size
            0x00, 0x00, 0x00, 0x00,  // Proto ids offset
            0x00, 0x00, 0x00, 0x00,  // Field ids size
            0x00, 0x00, 0x00, 0x00,  // Field ids offset
            0x00, 0x00, 0x00, 0x00,  // Method ids size
            0x00, 0x00, 0x00, 0x00,  // Method ids offset
            0x00, 0x00, 0x00, 0x00,  // Class defs size
            0x00, 0x00, 0x00, 0x00,  // Class defs offset
            0x00, 0x00, 0x00, 0x00,  // Data size
            0x00, 0x00, 0x00, 0x00   // Data offset
        };
        
        return dexHeader;
    }
}
