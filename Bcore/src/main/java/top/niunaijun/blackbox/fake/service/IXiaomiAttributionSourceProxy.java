package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;
import top.niunaijun.blackbox.util.XiaomiDeviceDetector;

/**
 * IXiaomiAttributionSource Proxy to handle MIUI-specific UID issues on Android 12+
 * This prevents crashes related to Xiaomi's enhanced security enforcement
 */
public class IXiaomiAttributionSourceProxy extends ClassInvocationStub {
    public static final String TAG = "IXiaomiAttributionSourceProxy";

    public IXiaomiAttributionSourceProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        // Return null since this is a global hook, not a specific service
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        // This proxy handles Xiaomi-specific AttributionSource issues globally
        if (XiaomiDeviceDetector.isXiaomiDevice()) {
            Slog.d(TAG, "IXiaomiAttributionSource proxy initialized for MIUI UID mismatch prevention on " + 
                    XiaomiDeviceDetector.getDeviceModel() + " (MIUI " + XiaomiDeviceDetector.getMiuiVersion() + ")");
        } else {
            Slog.d(TAG, "IXiaomiAttributionSource proxy initialized for MIUI UID mismatch prevention");
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Hook for AttributionSource constructor to fix UID issues on Xiaomi
    @ProxyMethod("AttributionSource")
    public static class AttributionSourceConstructor extends MethodHook {
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
                    Slog.w(TAG, "Xiaomi UID mismatch in AttributionSource constructor, creating safe fallback: " + message);
                    return AttributionSourceUtils.createSafeAttributionSource();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi AttributionSource constructor hook: " + e.getMessage());
                // Create safe fallback for Xiaomi devices
                return AttributionSourceUtils.createSafeAttributionSource();
            }
        }
    }

    // Hook for enforceCallingUid to bypass Xiaomi's strict enforcement
    @ProxyMethod("enforceCallingUid")
    public static class EnforceCallingUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Always allow on Xiaomi devices to prevent crashes
            Slog.d(TAG, "Xiaomi enforceCallingUid bypassed to prevent UID mismatch crashes");
            return null; // Indicate success
        }
    }

    // Hook for enforceCallingUidAndPid to bypass Xiaomi's strict enforcement
    @ProxyMethod("enforceCallingUidAndPid")
    public static class EnforceCallingUidAndPid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Always allow on Xiaomi devices to prevent crashes
            Slog.d(TAG, "Xiaomi enforceCallingUidAndPid bypassed to prevent UID mismatch crashes");
            return null; // Indicate success
        }
    }

    // Hook for fromParcel to fix UID issues in deserialized objects
    @ProxyMethod("fromParcel")
    public static class FromParcel extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Call original method first
                Object result = method.invoke(who, args);
                
                // Fix the UID of the created object for Xiaomi compatibility
                if (result != null) {
                    AttributionSourceUtils.fixAttributionSourceUid(result);
                }
                
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Error in Xiaomi fromParcel hook: " + e.getMessage());
                // Create safe fallback for Xiaomi devices
                return AttributionSourceUtils.createSafeAttributionSource();
            }
        }
    }
}
