package top.niunaijun.blackbox.fake.service;


import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * FeatureFlagUtils proxy to handle UID mismatch issues
 * when virtual apps try to access feature flags
 */
public class FeatureFlagUtilsProxy extends ClassInvocationStub {
    public static final String TAG = "FeatureFlagUtilsProxy";

    public FeatureFlagUtilsProxy() {
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

    @ProxyMethod("isEnabled")
    public static class IsEnabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Try to call the original method first
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in FeatureFlagUtils.isEnabled, returning safe default: " + errorMsg);
                    return true; // Return safe default for feature flags
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
                // Try to call the original method first
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in FeatureFlagUtils.getString, returning safe default: " + errorMsg);
                    return "true"; // Return safe default for feature flags
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
                // Try to call the original method first
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in FeatureFlagUtils.getInt, returning safe default: " + errorMsg);
                    return 1; // Return safe default for feature flags
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
                // Try to call the original method first
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in FeatureFlagUtils.getLong, returning safe default: " + errorMsg);
                    return 1L; // Return safe default for feature flags
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
                // Try to call the original method first
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in FeatureFlagUtils.getFloat, returning safe default: " + errorMsg);
                    return 1.0f; // Return safe default for feature flags
                }
                throw e;
            }
        }
    }

    @ProxyMethod("getBoolean")
    public static class GetBoolean extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Try to call the original method first
                return method.invoke(who, args);
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("Calling uid") && errorMsg.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in FeatureFlagUtils.getBoolean, returning safe default: " + errorMsg);
                    return true; // Return safe default for feature flags
                }
                throw e;
            }
        }
    }
}
