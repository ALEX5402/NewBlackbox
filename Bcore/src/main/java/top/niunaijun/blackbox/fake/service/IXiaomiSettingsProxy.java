package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;
import top.niunaijun.blackbox.util.XiaomiDeviceDetector;


public class IXiaomiSettingsProxy extends ClassInvocationStub {
    public static final String TAG = "IXiaomiSettingsProxy";

    public IXiaomiSettingsProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        
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

    
    @ProxyMethod("getStringForUser")
    public static class GetStringForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (SecurityException e) {
                
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "Xiaomi UID mismatch in getStringForUser, returning safe default: " + message);
                    return getSafeDefaultForXiaomiSettings();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi getStringForUser hook: " + e.getMessage());
                
                return getSafeDefaultForXiaomiSettings();
            }
        }
    }

    
    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (SecurityException e) {
                
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "Xiaomi UID mismatch in getString, returning safe default: " + message);
                    return getSafeDefaultForXiaomiSettings();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi getString hook: " + e.getMessage());
                
                return getSafeDefaultForXiaomiSettings();
            }
        }
    }

    
    @ProxyMethod("getIntForUser")
    public static class GetIntForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (SecurityException e) {
                
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "Xiaomi UID mismatch in getIntForUser, returning safe default: " + message);
                    return getSafeDefaultIntForXiaomiSettings();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi getIntForUser hook: " + e.getMessage());
                
                return getSafeDefaultIntForXiaomiSettings();
            }
        }
    }

    
    @ProxyMethod("getInt")
    public static class GetInt extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (SecurityException e) {
                
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "Xiaomi UID mismatch in getInt, returning safe default: " + message);
                    return getSafeDefaultIntForXiaomiSettings();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi getInt hook: " + e.getMessage());
                
                return getSafeDefaultIntForXiaomiSettings();
            }
        }
    }

    
    @ProxyMethod("getStringForUser")
    public static class GlobalGetStringForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (SecurityException e) {
                
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "Xiaomi UID mismatch in Global.getStringForUser, returning safe default: " + message);
                    return getSafeDefaultForXiaomiSettings();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi Global.getStringForUser hook: " + e.getMessage());
                
                return getSafeDefaultForXiaomiSettings();
            }
        }
    }

    
    @ProxyMethod("getString")
    public static class GlobalGetString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (SecurityException e) {
                
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "Xiaomi UID mismatch in Global.getString, returning safe default: " + message);
                    return getSafeDefaultForXiaomiSettings();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi Global.getString hook: " + e.getMessage());
                
                return getSafeDefaultForXiaomiSettings();
            }
        }
    }

    
    private static String getSafeDefaultForXiaomiSettings() {
        
        return "";
    }

    
    private static int getSafeDefaultIntForXiaomiSettings() {
        
        return 0;
    }
}
