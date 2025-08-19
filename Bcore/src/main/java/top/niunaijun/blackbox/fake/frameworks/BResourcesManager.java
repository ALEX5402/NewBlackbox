package top.niunaijun.blackbox.fake.frameworks;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import top.niunaijun.blackbox.fake.hook.HookManager;
import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;

/**
 * Utility class to handle resource loading issues in virtual environment
 * This provides safe resource loading methods to prevent crashes from missing system resources
 */
public class BResourcesManager implements IInjectHook {
    private static final String TAG = "BResourcesManager";

    @Override
    public void injectHook() {
        // This hook is not needed for the current approach
        // The resource loading issues are handled at the application level
        Log.d(TAG, "BResourcesManager hook initialized");
    }

    @Override
    public boolean isBadEnv() {
        return false; // Don't always inject this hook
    }

    /**
     * Safely load app label with fallback to package name
     */
    public static String safeLoadAppLabel(Object applicationInfo) {
        if (applicationInfo == null) {
            return "Unknown App";
        }
        
        // Try multiple approaches to get the package name
        String packageName = getPackageNameSafely(applicationInfo);
        
        // If we have a package name, use it as the label to avoid resource loading
        if (packageName != null && !packageName.isEmpty()) {
            return packageName;
        }
        
        // Try to get the label from the application info directly without triggering resource loading
        try {
            // Try to get the label from the application info's labelRes field first
            Method getLabelResMethod = applicationInfo.getClass().getMethod("getLabelRes");
            Integer labelRes = (Integer) getLabelResMethod.invoke(applicationInfo);
            
            if (labelRes != null && labelRes != 0) {
                // If we have a label resource, try to get it from the package manager
                Object packageManager = getPackageManager();
                if (packageManager != null) {
                    Method getTextMethod = packageManager.getClass().getMethod("getText", String.class, int.class, android.content.pm.ApplicationInfo.class);
                    Object label = getTextMethod.invoke(packageManager, packageName, labelRes, applicationInfo);
                    if (label != null) {
                        return label.toString();
                    }
                }
            }
            
            // Fallback to loadLabel method
            Method loadLabelMethod = applicationInfo.getClass().getMethod("loadLabel", android.content.pm.PackageManager.class);
            Object packageManager = getPackageManager();
            if (packageManager != null) {
                Object label = loadLabelMethod.invoke(applicationInfo, packageManager);
                if (label != null) {
                    return label.toString();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load app label: " + e.getMessage());
        }
        
        return "Unknown App";
    }
    
    /**
     * Get package name safely using multiple approaches
     */
    private static String getPackageNameSafely(Object applicationInfo) {
        // Try direct field access first
        try {
            Field packageNameField = applicationInfo.getClass().getDeclaredField("packageName");
            packageNameField.setAccessible(true);
            Object packageName = packageNameField.get(applicationInfo);
            if (packageName != null) {
                return packageName.toString();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get package name via field: " + e.getMessage());
        }
        
        // Try getPackageName method
        try {
            Method getPackageNameMethod = applicationInfo.getClass().getMethod("getPackageName");
            Object packageName = getPackageNameMethod.invoke(applicationInfo);
            if (packageName != null) {
                return packageName.toString();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get package name via method: " + e.getMessage());
        }
        
        // Try toString method to see if it contains package info
        try {
            String toString = applicationInfo.toString();
            if (toString.contains("packageName=")) {
                int start = toString.indexOf("packageName=") + 12;
                int end = toString.indexOf(" ", start);
                if (end == -1) end = toString.length();
                return toString.substring(start, end);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to get package name via toString: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Safely load app icon with fallback to null
     */
    public static Object safeLoadAppIcon(Object applicationInfo) {
        try {
            if (applicationInfo != null) {
                // First try to get the icon resource ID directly
                Method getIconMethod = applicationInfo.getClass().getMethod("getIcon");
                Integer iconRes = (Integer) getIconMethod.invoke(applicationInfo);
                
                if (iconRes != null && iconRes != 0) {
                    // Try to load the icon using the resource ID directly
                    Object packageManager = getPackageManager();
                    if (packageManager != null) {
                        try {
                            Method getDrawableMethod = packageManager.getClass().getMethod("getDrawable", String.class, int.class, android.content.pm.ApplicationInfo.class);
                            Object drawable = getDrawableMethod.invoke(packageManager, getPackageName(applicationInfo), iconRes, applicationInfo);
                            if (drawable != null) {
                                return drawable;
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to load icon via getDrawable: " + e.getMessage());
                        }
                    }
                }
                
                // Fallback to loadIcon method
                Method loadIconMethod = applicationInfo.getClass().getMethod("loadIcon", android.content.pm.PackageManager.class);
                Object packageManager = getPackageManager();
                if (packageManager != null) {
                    return loadIconMethod.invoke(applicationInfo, packageManager);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to load app icon: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get package name from application info
     */
    private static String getPackageName(Object applicationInfo) {
        String packageName = getPackageNameSafely(applicationInfo);
        return packageName != null ? packageName : "";
    }

    /**
     * Get PackageManager safely
     */
    private static Object getPackageManager() {
        try {
            Class<?> blackBoxCoreClass = Class.forName("top.niunaijun.blackbox.BlackBoxCore");
            Method getPackageManagerMethod = blackBoxCoreClass.getMethod("getPackageManager");
            return getPackageManagerMethod.invoke(null);
        } catch (Exception e) {
            Log.w(TAG, "Failed to get PackageManager: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Create a safe resource manager that doesn't load problematic overlays
     */
    public static Object createSafeResourceManager(Context context) {
        try {
            // Create a custom resource manager that skips problematic overlays
            Class<?> resourcesManagerClass = Class.forName("android.app.ResourcesManager");
            Object resourcesManager = resourcesManagerClass.newInstance();
            
            // Set a flag to disable overlay loading
            try {
                Field disableOverlayField = resourcesManagerClass.getDeclaredField("mDisableOverlayLoading");
                disableOverlayField.setAccessible(true);
                disableOverlayField.setBoolean(resourcesManager, true);
            } catch (Exception e) {
                Log.w(TAG, "Could not set overlay loading flag: " + e.getMessage());
            }
            
            return resourcesManager;
        } catch (Exception e) {
            Log.w(TAG, "Failed to create safe resource manager: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if a path is a problematic overlay path
     */
    public static boolean isProblematicOverlayPath(String path) {
        return path != null && path.contains("/data/resource-cache/") && path.contains(".frro");
    }
}
