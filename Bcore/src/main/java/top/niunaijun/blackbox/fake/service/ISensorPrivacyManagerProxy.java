package top.niunaijun.blackbox.fake.service;

import android.os.IInterface;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Force microphone sensor privacy OFF for sandboxed apps (Android 12+).
 */
public class ISensorPrivacyManagerProxy extends BinderInvocationStub {
    public static final String TAG = "SensorPrivacyProxy";

    public ISensorPrivacyManagerProxy() {
        super(BRServiceManager.get().getService("sensor_privacy"));
    }

    @Override
    protected Object getWho() {
        try {
            // Try multiple reflection paths for different Android versions
            Object stub = null;
            
            // Android 16+ path
            try {
                stub = Reflector.on("android.hardware.ISensorPrivacyManager$Stub")
                        .call("asInterface", BRServiceManager.get().getService("sensor_privacy"));
            } catch (Exception e1) {
                Slog.d(TAG, "Failed Android 16+ path, trying alternative: " + e1.getMessage());
                
                // Alternative path for older versions
                try {
                    stub = Reflector.on("android.hardware.ISensorPrivacyManager")
                            .call("asInterface", BRServiceManager.get().getService("sensor_privacy"));
                } catch (Exception e2) {
                    Slog.d(TAG, "Failed alternative path: " + e2.getMessage());
                    
                    // Last resort: try direct interface casting
                    try {
                        Class<?> stubClass = Class.forName("android.hardware.ISensorPrivacyManager$Stub");
                        Method asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder.class);
                        stub = asInterfaceMethod.invoke(null, BRServiceManager.get().getService("sensor_privacy"));
                    } catch (Exception e3) {
                        Slog.e(TAG, "All reflection paths failed for ISensorPrivacyManager", e3);
                        return null;
                    }
                }
            }
            
            if (stub != null) {
                Slog.d(TAG, "Successfully obtained ISensorPrivacyManager interface");
                return (IInterface) stub;
            } else {
                Slog.e(TAG, "Reflection succeeded but returned null interface");
                return null;
            }
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get ISensorPrivacyManager interface", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("sensor_privacy");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Newer APIs: boolean isSensorPrivacyEnabled(int sensor)
    @ProxyMethod("isSensorPrivacyEnabled")
    public static class IsSensorPrivacyEnabled extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "SensorPrivacy: isSensorPrivacyEnabled returning false");
            return false;
        }
    }

    // Some versions: boolean isSensorPrivacyEnabled(int userId, int sensor)
    @ProxyMethod("isSensorPrivacyEnabledForUser")
    public static class IsSensorPrivacyEnabledForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "SensorPrivacy: isSensorPrivacyEnabledForUser returning false");
            return false;
        }
    }

    // Some versions: boolean isSensorPrivacyEnabled(int userId, int sensor, String packageName)
    @ProxyMethod("isSensorPrivacyEnabledForProfile")
    public static class IsSensorPrivacyEnabledForProfile extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "SensorPrivacy: isSensorPrivacyEnabledForProfile returning false");
            return false;
        }
    }

    // Allow sensor access
    @ProxyMethod("setSensorPrivacy")
    public static class SetSensorPrivacy extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "SensorPrivacy: setSensorPrivacy allowing");
            return method.invoke(who, args);
        }
    }

    // Allow sensor access
    @ProxyMethod("setSensorPrivacyForProfile")
    public static class SetSensorPrivacyForProfile extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "SensorPrivacy: setSensorPrivacyForProfile allowing");
            return method.invoke(who, args);
        }
    }
}


