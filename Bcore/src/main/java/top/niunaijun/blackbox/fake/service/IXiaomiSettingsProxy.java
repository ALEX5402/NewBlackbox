package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;
import top.niunaijun.blackbox.util.XiaomiDeviceDetector;

/**
 * IXiaomiSettings Proxy to handle MIUI-specific Settings access issues on Android 12+
 * This prevents crashes related to Xiaomi's enhanced security enforcement in system settings
 */
public class IXiaomiSettingsProxy extends ClassInvocationStub {
    public static final String TAG = "IXiaomiSettingsProxy";

    public IXiaomiSettingsProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        // Return null since this is a global hook, not a specific service
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        // This proxy handles Xiaomi-specific Settings access issues globally
        if (XiaomiDeviceDetector.isXiaomiDevice()) {
            Slog.d(TAG, "IXiaomiSettings proxy initialized for MIUI Settings UID mismatch prevention on " + 
                    XiaomiDeviceDetector.getDeviceModel() + " (MIUI " + XiaomiDeviceDetector.getMiuiVersion() + ")");
        } else {
            Slog.d(TAG, "IXiaomiSettings proxy initialized for MIUI Settings UID mismatch prevention");
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Hook for Settings.System.getStringForUser to fix UID issues on Xiaomi
    @ProxyMethod("getStringForUser")
    public static class GetStringForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (SecurityException e) {
                // Handle SecurityException specifically for UID mismatch on Xiaomi
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "Xiaomi UID mismatch in getStringForUser, returning safe default: " + message);
                    return getSafeDefaultForXiaomiSettings();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi getStringForUser hook: " + e.getMessage());
                // Return safe default for Xiaomi devices
                return getSafeDefaultForXiaomiSettings();
            }
        }
    }

    // Hook for Settings.System.getString to fix UID issues on Xiaomi
    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (SecurityException e) {
                // Handle SecurityException specifically for UID mismatch on Xiaomi
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "Xiaomi UID mismatch in getString, returning safe default: " + message);
                    return getSafeDefaultForXiaomiSettings();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi getString hook: " + e.getMessage());
                // Return safe default for Xiaomi devices
                return getSafeDefaultForXiaomiSettings();
            }
        }
    }

    // Hook for Settings.System.getIntForUser to fix UID issues on Xiaomi
    @ProxyMethod("getIntForUser")
    public static class GetIntForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (SecurityException e) {
                // Handle SecurityException specifically for UID mismatch on Xiaomi
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "Xiaomi UID mismatch in getIntForUser, returning safe default: " + message);
                    return getSafeDefaultIntForXiaomiSettings();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi getIntForUser hook: " + e.getMessage());
                // Return safe default for Xiaomi devices
                return getSafeDefaultIntForXiaomiSettings();
            }
        }
    }

    // Hook for Settings.System.getInt to fix UID issues on Xiaomi
    @ProxyMethod("getInt")
    public static class GetInt extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (SecurityException e) {
                // Handle SecurityException specifically for UID mismatch on Xiaomi
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "Xiaomi UID mismatch in getInt, returning safe default: " + message);
                    return getSafeDefaultIntForXiaomiSettings();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi getInt hook: " + e.getMessage());
                // Return safe default for Xiaomi devices
                return getSafeDefaultIntForXiaomiSettings();
            }
        }
    }

    // Hook for Settings.Global.getStringForUser to fix UID issues on Xiaomi
    @ProxyMethod("getStringForUser")
    public static class GlobalGetStringForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (SecurityException e) {
                // Handle SecurityException specifically for UID mismatch on Xiaomi
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "Xiaomi UID mismatch in Global.getStringForUser, returning safe default: " + message);
                    return getSafeDefaultForXiaomiSettings();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi Global.getStringForUser hook: " + e.getMessage());
                // Return safe default for Xiaomi devices
                return getSafeDefaultForXiaomiSettings();
            }
        }
    }

    // Hook for Settings.Global.getString to fix UID issues on Xiaomi
    @ProxyMethod("getString")
    public static class GlobalGetString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (SecurityException e) {
                // Handle SecurityException specifically for UID mismatch on Xiaomi
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "Xiaomi UID mismatch in Global.getString, returning safe default: " + message);
                    return getSafeDefaultForXiaomiSettings();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi Global.getString hook: " + e.getMessage());
                // Return safe default for Xiaomi devices
                return getSafeDefaultForXiaomiSettings();
            }
        }
    }

    /**
     * Get safe default value for Xiaomi Settings access
     */
    private static String getSafeDefaultForXiaomiSettings() {
        // Return empty string as safe default for Xiaomi devices
        return "";
    }

    /**
     * Get safe default int value for Xiaomi Settings access
     */
    private static int getSafeDefaultIntForXiaomiSettings() {
        // Return 0 as safe default for Xiaomi devices
        return 0;
    }
}
