package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;

/**
 * ISystemSensorManager Proxy to handle AttributionSource UID issues on Android 12+
 * This prevents crashes related to SystemSensorManager UID enforcement
 */
public class ISystemSensorManagerProxy extends ClassInvocationStub {
    public static final String TAG = "ISystemSensorManagerProxy";

    public ISystemSensorManagerProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        // Return null since this is a global hook, not a specific service
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        // This proxy handles SystemSensorManager calls globally
        Slog.d(TAG, "ISystemSensorManager proxy initialized for UID mismatch prevention");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Hook for SystemSensorManager constructor to fix AttributionSource issues
    @ProxyMethod("SystemSensorManager")
    public static class Constructor extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (SecurityException e) {
                // Handle SecurityException specifically for UID mismatch
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in SystemSensorManager constructor, creating safe fallback: " + message);
                    return createSafeSensorManager();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in SystemSensorManager constructor hook: " + e.getMessage());
                // Create safe fallback
                return createSafeSensorManager();
            }
        }
    }

    // Hook for parsePackageList method to fix AttributionSource issues
    @ProxyMethod("parsePackageList")
    public static class ParsePackageList extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Fix AttributionSource in args before calling original method
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                // Call original method
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in parsePackageList hook: " + e.getMessage());
                // Return empty list to prevent crashes
                return new java.util.ArrayList<>();
            }
        }
    }

    /**
     * Create a safe SystemSensorManager for fallback
     */
    private static Object createSafeSensorManager() {
        try {
            // Try to create a safe SystemSensorManager using reflection
            Class<?> sensorManagerClass = Class.forName("android.hardware.SystemSensorManager");
            
            // Try different constructor signatures
            Object sensorManager = null;
            
            try {
                // Try default constructor
                java.lang.reflect.Constructor<?> constructor = sensorManagerClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                sensorManager = constructor.newInstance();
                Slog.d(TAG, "Created safe SystemSensorManager using default constructor");
            } catch (Exception e) {
                Slog.w(TAG, "Could not create safe SystemSensorManager: " + e.getMessage());
                return null;
            }
            
            return sensorManager;
        } catch (Exception e) {
            Slog.w(TAG, "Error creating safe SystemSensorManager: " + e.getMessage());
            return null;
        }
    }
}
