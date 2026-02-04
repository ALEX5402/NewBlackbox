package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;


public class ISystemSensorManagerProxy extends ClassInvocationStub {
    public static final String TAG = "ISystemSensorManagerProxy";

    public ISystemSensorManagerProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        
        Slog.d(TAG, "ISystemSensorManager proxy initialized for UID mismatch prevention");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    @ProxyMethod("SystemSensorManager")
    public static class Constructor extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (SecurityException e) {
                
                String message = e.getMessage();
                if (message != null && message.contains("Calling uid") && message.contains("doesn't match source uid")) {
                    Slog.w(TAG, "UID mismatch in SystemSensorManager constructor, creating safe fallback: " + message);
                    return createSafeSensorManager();
                }
                throw e;
            } catch (Exception e) {
                Slog.w(TAG, "Error in SystemSensorManager constructor hook: " + e.getMessage());
                
                return createSafeSensorManager();
            }
        }
    }

    
    @ProxyMethod("parsePackageList")
    public static class ParsePackageList extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
                
                
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error in parsePackageList hook: " + e.getMessage());
                
                return new java.util.ArrayList<>();
            }
        }
    }

    
    private static Object createSafeSensorManager() {
        try {
            
            Class<?> sensorManagerClass = Class.forName("android.hardware.SystemSensorManager");
            
            
            Object sensorManager = null;
            
            try {
                
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
