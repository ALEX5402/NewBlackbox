package top.niunaijun.blackbox.util;

import android.os.Build;
import android.text.TextUtils;

import top.niunaijun.blackbox.utils.Slog;

/**
 * created by alex5402 to fix crash on xiaomi's MEME Ui
 * Utility class to detect Xiaomi devices and provide device-specific information
 * This helps the Xiaomi proxies apply the correct fixes for different MIUI versions
 */
public class XiaomiDeviceDetector {
    private static final String TAG = "XiaomiDeviceDetector";
    
    // Xiaomi device manufacturers
    private static final String[] XIAOMI_MANUFACTURERS = {
        "Xiaomi", "Redmi", "POCO", "Black Shark", "Mi"
    };
    
    // MIUI version patterns
    private static final String[] MIUI_VERSION_PATTERNS = {
        "MIUI", "HyperOS", "MIUI Global", "MIUI China"
    };
    
    // Known Xiaomi device models
    private static final String[] XIAOMI_MODELS = {
        "Mi ", "Redmi ", "POCO ", "Black Shark ", "Xiaomi "
    };
    
    private static boolean sIsXiaomiDevice = false;
    private static String sMiuiVersion = null;
    private static String sDeviceModel = null;
    private static int sAndroidVersion = 0;
    
    /**
     * Check if the current device is a Xiaomi device
     */
    public static boolean isXiaomiDevice() {
        if (sIsXiaomiDevice) {
            return true;
        }
        
        try {
            // Check manufacturer
            String manufacturer = Build.MANUFACTURER;
            if (isXiaomiManufacturer(manufacturer)) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by manufacturer: " + manufacturer);
                return true;
            }
            
            // Check brand
            String brand = Build.BRAND;
            if (isXiaomiManufacturer(brand)) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by brand: " + brand);
                return true;
            }
            
            // Check model
            String model = Build.MODEL;
            if (isXiaomiModel(model)) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by model: " + model);
                return true;
            }
            
            // Check product
            String product = Build.PRODUCT;
            if (isXiaomiModel(product)) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by product: " + product);
                return true;
            }
            
            // Check device
            String device = Build.DEVICE;
            if (isXiaomiModel(device)) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by device: " + device);
                return true;
            }
            
            // Check fingerprint
            String fingerprint = Build.FINGERPRINT;
            if (isXiaomiFingerprint(fingerprint)) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by fingerprint: " + fingerprint);
                return true;
            }
            
            // Check system properties
            if (checkXiaomiSystemProperties()) {
                sIsXiaomiDevice = true;
                Slog.d(TAG, "Detected Xiaomi device by system properties");
                return true;
            }
            
        } catch (Exception e) {
            Slog.w(TAG, "Error detecting Xiaomi device: " + e.getMessage());
        }
        
        sIsXiaomiDevice = false;
        return false;
    }
    
    /**
     * Get MIUI version if available
     */
    public static String getMiuiVersion() {
        if (sMiuiVersion != null) {
            return sMiuiVersion;
        }
        
        if (!isXiaomiDevice()) {
            return null;
        }
        
        try {
            // Try to get MIUI version from system properties
            sMiuiVersion = getSystemProperty("ro.miui.ui.version.name");
            if (!TextUtils.isEmpty(sMiuiVersion)) {
                Slog.d(TAG, "Detected MIUI version: " + sMiuiVersion);
                return sMiuiVersion;
            }
            
            // Try alternative property
            sMiuiVersion = getSystemProperty("ro.miui.version.code");
            if (!TextUtils.isEmpty(sMiuiVersion)) {
                Slog.d(TAG, "Detected MIUI version code: " + sMiuiVersion);
                return sMiuiVersion;
            }
            
            // Try build description
            String buildDesc = Build.DISPLAY;
            if (!TextUtils.isEmpty(buildDesc) && containsMiuiVersion(buildDesc)) {
                sMiuiVersion = extractMiuiVersion(buildDesc);
                Slog.d(TAG, "Detected MIUI version from build: " + sMiuiVersion);
                return sMiuiVersion;
            }
            
        } catch (Exception e) {
            Slog.w(TAG, "Error getting MIUI version: " + e.getMessage());
        }
        
        sMiuiVersion = "Unknown";
        return sMiuiVersion;
    }
    
    /**
     * Get device model
     */
    public static String getDeviceModel() {
        if (sDeviceModel != null) {
            return sDeviceModel;
        }
        
        sDeviceModel = Build.MODEL;
        return sDeviceModel;
    }
    
    /**
     * Get Android version
     */
    public static int getAndroidVersion() {
        if (sAndroidVersion > 0) {
            return sAndroidVersion;
        }
        
        sAndroidVersion = Build.VERSION.SDK_INT;
        return sAndroidVersion;
    }
    
    /**
     * Check if device is running MIUI 12 or higher
     */
    public static boolean isMiui12OrHigher() {
        if (!isXiaomiDevice()) {
            return false;
        }
        
        String miuiVersion = getMiuiVersion();
        if (TextUtils.isEmpty(miuiVersion) || "Unknown".equals(miuiVersion)) {
            return false;
        }
        
        try {
            // Extract version number from MIUI version string
            String versionNumber = extractVersionNumber(miuiVersion);
            if (!TextUtils.isEmpty(versionNumber)) {
                int version = Integer.parseInt(versionNumber);
                return version >= 12;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error checking MIUI version: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Check if device is running MIUI 13 or higher
     */
    public static boolean isMiui13OrHigher() {
        if (!isXiaomiDevice()) {
            return false;
        }
        
        String miuiVersion = getMiuiVersion();
        if (TextUtils.isEmpty(miuiVersion) || "Unknown".equals(miuiVersion)) {
            return false;
        }
        
        try {
            // Extract version number from MIUI version string
            String versionNumber = extractVersionNumber(miuiVersion);
            if (!TextUtils.isEmpty(versionNumber)) {
                int version = Integer.parseInt(versionNumber);
                return version >= 13;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error checking MIUI version: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Check if device is running HyperOS (MIUI 15+)
     */
    public static boolean isHyperOS() {
        if (!isXiaomiDevice()) {
            return false;
        }
        
        String miuiVersion = getMiuiVersion();
        if (TextUtils.isEmpty(miuiVersion) || "Unknown".equals(miuiVersion)) {
            return false;
        }
        
        return miuiVersion.contains("HyperOS") || miuiVersion.contains("OS1.");
    }
    
    /**
     * Get device-specific information for debugging
     */
    public static String getDeviceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        info.append("Android: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        info.append("Xiaomi Device: ").append(isXiaomiDevice()).append("\n");
        
        if (isXiaomiDevice()) {
            info.append("MIUI Version: ").append(getMiuiVersion()).append("\n");
            info.append("MIUI 12+: ").append(isMiui12OrHigher()).append("\n");
            info.append("MIUI 13+: ").append(isMiui13OrHigher()).append("\n");
            info.append("HyperOS: ").append(isHyperOS()).append("\n");
        }
        
        return info.toString();
    }
    
    // Private helper methods
    
    private static boolean isXiaomiManufacturer(String manufacturer) {
        if (TextUtils.isEmpty(manufacturer)) {
            return false;
        }
        
        String lowerManufacturer = manufacturer.toLowerCase();
        for (String xiaomiManufacturer : XIAOMI_MANUFACTURERS) {
            if (lowerManufacturer.contains(xiaomiManufacturer.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean isXiaomiModel(String model) {
        if (TextUtils.isEmpty(model)) {
            return false;
        }
        
        String lowerModel = model.toLowerCase();
        for (String xiaomiModel : XIAOMI_MODELS) {
            if (lowerModel.contains(xiaomiModel.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean isXiaomiFingerprint(String fingerprint) {
        if (TextUtils.isEmpty(fingerprint)) {
            return false;
        }
        
        String lowerFingerprint = fingerprint.toLowerCase();
        return lowerFingerprint.contains("xiaomi") || 
               lowerFingerprint.contains("redmi") || 
               lowerFingerprint.contains("poco") ||
               lowerFingerprint.contains("blackshark") ||
               lowerFingerprint.contains("mi ");
    }
    
    private static boolean checkXiaomiSystemProperties() {
        try {
            // Check for MIUI-specific system properties
            String miuiProperty = getSystemProperty("ro.miui.ui.version.name");
            if (!TextUtils.isEmpty(miuiProperty)) {
                return true;
            }
            
            String miuiCodeProperty = getSystemProperty("ro.miui.version.code");
            if (!TextUtils.isEmpty(miuiCodeProperty)) {
                return true;
            }
            
            String miuiBuildProperty = getSystemProperty("ro.miui.build.version");
            if (!TextUtils.isEmpty(miuiBuildProperty)) {
                return true;
            }
            
        } catch (Exception e) {
            Slog.w(TAG, "Error checking Xiaomi system properties: " + e.getMessage());
        }
        
        return false;
    }
    
    private static boolean containsMiuiVersion(String text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        for (String pattern : MIUI_VERSION_PATTERNS) {
            if (lowerText.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    private static String extractMiuiVersion(String text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        
        // Try to extract version like "MIUI 14.0.1" or "HyperOS 1.0.0"
        String[] parts = text.split(" ");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equalsIgnoreCase("MIUI") || parts[i].equalsIgnoreCase("HyperOS")) {
                return parts[i] + " " + parts[i + 1];
            }
        }
        
        return text;
    }
    
    private static String extractVersionNumber(String miuiVersion) {
        if (TextUtils.isEmpty(miuiVersion)) {
            return null;
        }
        
        // Extract version number like "14" from "MIUI 14.0.1"
        String[] parts = miuiVersion.split(" ");
        if (parts.length > 1) {
            String versionPart = parts[1];
            String[] versionParts = versionPart.split("\\.");
            if (versionParts.length > 0) {
                return versionParts[0];
            }
        }
        
        return null;
    }
    
    private static String getSystemProperty(String key) {
        try {
            // Use reflection to access SystemProperties
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method getMethod = systemPropertiesClass.getMethod("get", String.class);
            return (String) getMethod.invoke(null, key);
        } catch (Exception e) {
            Slog.w(TAG, "Error getting system property " + key + ": " + e.getMessage());
            return null;
        }
    }
}
