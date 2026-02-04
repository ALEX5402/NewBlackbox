package top.niunaijun.blackbox.fake.service;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


public class ISettingsProviderProxy extends ClassInvocationStub {
    public static final String TAG = "ISettingsProviderProxy";

    public ISettingsProviderProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        return null;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
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
                
                if (args != null && args.length > 0) {
                    String key = (String) args[0];
                    if (key != null && key.contains("feature_flag")) {
                        Slog.d(TAG, "Intercepting feature flag query: " + key + ", returning safe default");
                        return "true"; 
                    }
                }
                
                
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in getStringForUser, returning safe default: " + errorMsg);
                    return "true"; 
                }
                throw e;
            }
        }
    }

    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                if (args != null && args.length > 0) {
                    String key = (String) args[0];
                    if (key != null && key.contains("feature_flag")) {
                        Slog.d(TAG, "Intercepting feature flag query: " + key + ", returning safe default");
                        return "true"; 
                    }
                }
                
                
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in getString, returning safe default: " + errorMsg);
                    return "true"; 
                }
                throw e;
            }
        }
    }

    @ProxyMethod("getIntForUser")
    public static class GetIntForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in getIntForUser, returning safe default: " + errorMsg);
                    return 1; 
                }
                throw e;
            }
        }
    }

    @ProxyMethod("getInt")
    public static class GetInt extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in getInt, returning safe default: " + errorMsg);
                    return 1; 
                }
                throw e;
            }
        }
    }

    @ProxyMethod("getLongForUser")
    public static class GetLongForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in getLongForUser, returning safe default: " + errorMsg);
                    return 1L; 
                }
                throw e;
            }
        }
    }

    @ProxyMethod("getLong")
    public static class GetLong extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in getLong, returning safe default: " + errorMsg);
                    return 1L; 
                }
                throw e;
            }
        }
    }

    @ProxyMethod("getFloatForUser")
    public static class GetFloatForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in getFloatForUser, returning safe default: " + errorMsg);
                    return 1.0f; 
                }
                throw e;
            }
        }
    }

    @ProxyMethod("getFloat")
    public static class GetFloat extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in getFloat, returning safe default: " + errorMsg);
                    return 1.0f; 
                }
                throw e;
            }
        }
    }
}
