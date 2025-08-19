package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;
import top.niunaijun.blackbox.util.XiaomiDeviceDetector;

/**
 * IXiaomiMiuiServices Proxy to handle MIUI-specific service issues on Android 12+
 * This prevents crashes related to Xiaomi's MIUI framework and proprietary services
 */
public class IXiaomiMiuiServicesProxy extends ClassInvocationStub {
    public static final String TAG = "IXiaomiMiuiServicesProxy";

    public IXiaomiMiuiServicesProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        // Return null since this is a global hook, not a specific service
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        // This proxy handles Xiaomi-specific MIUI service issues globally
        if (XiaomiDeviceDetector.isXiaomiDevice()) {
            Slog.d(TAG, "IXiaomiMiuiServices proxy initialized for MIUI service crash prevention on " + 
                    XiaomiDeviceDetector.getDeviceModel() + " (MIUI " + XiaomiDeviceDetector.getMiuiVersion() + ")");
        } else {
            Slog.d(TAG, "IXiaomiMiuiServices proxy initialized for MIUI service crash prevention");
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Hook for MiuiCameraCoveredManager to prevent NullPointerException on Xiaomi
    @ProxyMethod("MiuiCameraCoveredManager")
    public static class MiuiCameraCoveredManager extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (NullPointerException e) {
                // Handle NullPointerException specifically for MIUI camera manager on Xiaomi
                String message = e.getMessage();
                if (message != null && message.contains("Attempt to invoke virtual method") && 
                    message.contains("android.provider.MiuiSettings$SettingsCloudData$CloudData.getString")) {
                    Slog.w(TAG, "MIUI Camera Manager NullPointerException on Xiaomi, creating safe fallback: " + message);
                    return createSafeMiuiCameraManagerForXiaomi();
                }
                throw e;
            } catch (SecurityException e) {
                // Handle SecurityException for UID mismatch on Xiaomi
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "MIUI Camera Manager UID mismatch on Xiaomi, creating safe fallback: " + message);
                    return createSafeMiuiCameraManagerForXiaomi();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in MIUI Camera Manager hook: " + e.getMessage());
                // Create safe fallback for Xiaomi devices
                return createSafeMiuiCameraManagerForXiaomi();
            }
        }
    }

    // Hook for MiuiForceDarkConfig to prevent crashes on Xiaomi
    @ProxyMethod("MiuiForceDarkConfig")
    public static class MiuiForceDarkConfig extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in MIUI Force Dark Config hook: " + e.getMessage());
                // Return safe default for Xiaomi devices
                return null;
            }
        }
    }

    // Hook for MiuiMonitorThread to prevent crashes on Xiaomi
    @ProxyMethod("MiuiMonitorThread")
    public static class MiuiMonitorThread extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in MIUI Monitor Thread hook: " + e.getMessage());
                // Return safe default for Xiaomi devices
                return null;
            }
        }
    }

    // Hook for MiuiDownscaleImpl to prevent crashes on Xiaomi
    @ProxyMethod("MiuiDownscaleImpl")
    public static class MiuiDownscaleImpl extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in MIUI Downscale Implementation hook: " + e.getMessage());
                // Return safe default for Xiaomi devices
                return null;
            }
        }
    }

    // Hook for ForceDarkHelperStubImpl to prevent crashes on Xiaomi
    @ProxyMethod("ForceDarkHelperStubImpl")
    public static class ForceDarkHelperStubImpl extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in Force Dark Helper Stub Implementation hook: " + e.getMessage());
                // Return safe default for Xiaomi devices
                return null;
            }
        }
    }

    // Hook for HandWritingStubImpl to prevent crashes on Xiaomi
    @ProxyMethod("HandWritingStubImpl")
    public static class HandWritingStubImpl extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in Hand Writing Stub Implementation hook: " + e.getMessage());
                // Return safe default for Xiaomi devices
                return null;
            }
        }
    }

    // Hook for OplusScrollToTopManager to prevent crashes on Xiaomi
    @ProxyMethod("OplusScrollToTopManager")
    public static class OplusScrollToTopManager extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in Oplus Scroll To Top Manager hook: " + e.getMessage());
                // Return safe default for Xiaomi devices
                return null;
            }
        }
    }

    // Hook for OplusOverScrollerExtImpl to prevent crashes on Xiaomi
    @ProxyMethod("OplusOverScrollerExtImpl")
    public static class OplusOverScrollerExtImpl extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in Oplus Over Scroller Extension Implementation hook: " + e.getMessage());
                // Return safe default for Xiaomi devices
                return null;
            }
        }
    }

    /**
     * Create a safe MiuiCameraManager specifically for Xiaomi devices
     */
    private static Object createSafeMiuiCameraManagerForXiaomi() {
        try {
            // Try to create a safe MiuiCameraCoveredManager using reflection
            Class<?> miuiCameraManagerClass = Class.forName("android.provider.MiuiSettings$SettingsCloudData$CloudData");
            
            // Try different constructor signatures that work on Xiaomi
            Object miuiCameraManager = null;
            
            try {
                // Try default constructor
                java.lang.reflect.Constructor<?> constructor = miuiCameraManagerClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                miuiCameraManager = constructor.newInstance();
                Slog.d(TAG, "Created safe MiuiCameraManager for Xiaomi using default constructor");
            } catch (Exception e) {
                Slog.w(TAG, "Could not create safe MiuiCameraManager for Xiaomi: " + e.getMessage());
                return null;
            }
            
            return miuiCameraManager;
        } catch (Exception e) {
            Slog.w(TAG, "Error creating safe MiuiCameraManager for Xiaomi: " + e.getMessage());
            return null;
        }
    }
}
