package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Device ID proxy to handle GMS device ID retrieval issues.
 */
public class DeviceIdProxy extends ClassInvocationStub {
    public static final String TAG = "DeviceIdProxy";

    public DeviceIdProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        return null; // Not needed for class method hooks
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // Hook device ID related class methods directly
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Hook device ID getter methods to prevent NullPointerException
    @ProxyMethod("getDeviceId")
    public static class GetDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (who == null) {
                    Slog.w(TAG, "GetDeviceId called on null object, returning default device ID");
                    return "default_device_id";
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GetDeviceId error, returning default device ID: " + e.getMessage());
                return "default_device_id";
            }
        }
    }

    // Hook device ID setter methods
    @ProxyMethod("setDeviceId")
    public static class SetDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (who == null) {
                    Slog.w(TAG, "SetDeviceId called on null object, ignoring");
                    return null;
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "SetDeviceId error, ignoring: " + e.getMessage());
                return null;
            }
        }
    }

    // Hook device ID validation methods
    @ProxyMethod("isValidDeviceId")
    public static class IsValidDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (who == null) {
                    Slog.w(TAG, "IsValidDeviceId called on null object, returning true");
                    return true;
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "IsValidDeviceId error, returning true: " + e.getMessage());
                return true;
            }
        }
    }

    // Hook device ID generation methods
    @ProxyMethod("generateDeviceId")
    public static class GenerateDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GenerateDeviceId error, returning default device ID: " + e.getMessage());
                return "generated_device_id_" + System.currentTimeMillis();
            }
        }
    }

    // Hook device ID storage methods
    @ProxyMethod("storeDeviceId")
    public static class StoreDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (who == null) {
                    Slog.w(TAG, "StoreDeviceId called on null object, ignoring");
                    return null;
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "StoreDeviceId error, ignoring: " + e.getMessage());
                return null;
            }
        }
    }

    // Hook device ID retrieval methods
    @ProxyMethod("retrieveDeviceId")
    public static class RetrieveDeviceId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (who == null) {
                    Slog.w(TAG, "RetrieveDeviceId called on null object, returning default device ID");
                    return "retrieved_device_id";
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "RetrieveDeviceId error, returning default device ID: " + e.getMessage());
                return "retrieved_device_id";
            }
        }
    }
}
