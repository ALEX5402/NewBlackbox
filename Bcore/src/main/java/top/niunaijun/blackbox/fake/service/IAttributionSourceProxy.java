package top.niunaijun.blackbox.fake.service;



import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * AttributionSource Proxy to handle UID mismatch issues on Android 12+
 * This prevents crashes related to AttributionSource UID enforcement
 */
public class IAttributionSourceProxy extends ClassInvocationStub {
    public static final String TAG = "IAttributionSourceProxy";

    public IAttributionSourceProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        // Return null since this is a global hook, not a specific service
        return null;
    }

    @Override
    protected void inject(Object base, Object proxy) {
        // This proxy handles AttributionSource creation globally
        Slog.d(TAG, "AttributionSource proxy initialized for UID mismatch prevention");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Hook for AttributionSource constructor to fix UID issues
    @ProxyMethod("AttributionSource")
    public static class AttributionSourceConstructor extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Always create AttributionSource with correct UID to prevent crashes
                int uid = BlackBoxCore.getHostUid();
                String packageName = BlackBoxCore.getHostPkg();
                
                Slog.d(TAG, "Creating AttributionSource with UID: " + uid + ", package: " + packageName);
                
                // Create AttributionSource with proper UID
                Object attributionSource = createSafeAttributionSource(uid, packageName);
                if (attributionSource != null) {
                    return attributionSource;
                }
                
                // Fallback to original method if creation fails
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Error creating AttributionSource, using fallback: " + e.getMessage());
                return createSafeAttributionSource(BlackBoxCore.getHostUid(), BlackBoxCore.getHostPkg());
            }
        }
        
        private Object createSafeAttributionSource(int uid, String packageName) {
            try {
                // Use reflection to create AttributionSource with correct parameters
                Class<?> attributionSourceClass = Class.forName("android.content.AttributionSource");
                
                // Try different constructor signatures
                Constructor<?> constructor = null;
                try {
                    // Try constructor with uid, packageName, attributionTag
                    constructor = attributionSourceClass.getDeclaredConstructor(int.class, String.class, String.class);
                } catch (NoSuchMethodException e) {
                    try {
                        // Try constructor with uid, packageName
                        constructor = attributionSourceClass.getDeclaredConstructor(int.class, String.class);
                    } catch (NoSuchMethodException e2) {
                        // Try default constructor
                        constructor = attributionSourceClass.getDeclaredConstructor();
                    }
                }
                
                if (constructor != null) {
                    constructor.setAccessible(true);
                    
                    if (constructor.getParameterCount() == 3) {
                        return constructor.newInstance(uid, packageName, null);
                    } else if (constructor.getParameterCount() == 2) {
                        return constructor.newInstance(uid, packageName);
                    } else {
                        return constructor.newInstance();
                    }
                }
                
                return null;
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create safe AttributionSource: " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for AttributionSource.enforceCallingUid to prevent UID mismatch crashes
    @ProxyMethod("enforceCallingUid")
    public static class EnforceCallingUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Always allow the call to proceed to prevent UID mismatch crashes
                Slog.d(TAG, "Intercepting enforceCallingUid to prevent UID mismatch crashes");
                return null; // Return null to indicate success
            } catch (Exception e) {
                Slog.w(TAG, "Error in enforceCallingUid hook: " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for AttributionSource.enforceCallingUidAndPid to prevent UID mismatch crashes
    @ProxyMethod("enforceCallingUidAndPid")
    public static class EnforceCallingUidAndPid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Always allow the call to proceed to prevent UID mismatch crashes
                Slog.d(TAG, "Intercepting enforceCallingUidAndPid to prevent UID mismatch crashes");
                return null; // Return null to indicate success
            } catch (Exception e) {
                Slog.w(TAG, "Error in enforceCallingUidAndPid hook: " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for AttributionSource.fromParcel to handle Parcel creation
    @ProxyMethod("fromParcel")
    public static class FromParcel extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    // Fix the UID in the created AttributionSource
                    fixAttributionSourceUid(result);
                    return result;
                }
                
                // If original method fails, create a safe fallback
                return createSafeAttributionSource(BlackBoxCore.getHostUid(), BlackBoxCore.getHostPkg());
            } catch (Exception e) {
                Slog.w(TAG, "Error in fromParcel, using fallback: " + e.getMessage());
                return createSafeAttributionSource(BlackBoxCore.getHostUid(), BlackBoxCore.getHostPkg());
            }
        }
        
        private void fixAttributionSourceUid(Object attributionSource) {
            try {
                // Use reflection to fix the UID
                Class<?> attributionSourceClass = attributionSource.getClass();
                Method setUidMethod = attributionSourceClass.getDeclaredMethod("setUid", int.class);
                setUidMethod.setAccessible(true);
                setUidMethod.invoke(attributionSource, BlackBoxCore.getHostUid());
                
                Slog.d(TAG, "Fixed AttributionSource UID to: " + BlackBoxCore.getHostUid());
            } catch (Exception e) {
                Slog.w(TAG, "Could not fix AttributionSource UID: " + e.getMessage());
            }
        }
        
        private Object createSafeAttributionSource(int uid, String packageName) {
            try {
                Class<?> attributionSourceClass = Class.forName("android.content.AttributionSource");
                Constructor<?> constructor = attributionSourceClass.getDeclaredConstructor(int.class, String.class, String.class);
                constructor.setAccessible(true);
                return constructor.newInstance(uid, packageName, null);
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create safe AttributionSource fallback: " + e.getMessage());
                return null;
            }
        }
    }
}
