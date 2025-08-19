package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Android ID proxy to handle Android ID retrieval issues that cause problems with Google Play Store.
 */
public class AndroidIdProxy extends ClassInvocationStub {
    public static final String TAG = "AndroidIdProxy";

    public AndroidIdProxy() {
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

    @ProxyMethod("getAndroidId")
    public static class GetAndroidId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "AndroidId: Handling getAndroidId call");
                Object result = method.invoke(who, args);
                if (result == null || "0".equals(result.toString()) || "".equals(result.toString())) {
                    Slog.w(TAG, "AndroidId: Invalid Android ID detected, returning mock ID");
                    return generateMockAndroidId();
                }
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "AndroidId: GetAndroidId error, returning mock ID", e);
                return generateMockAndroidId();
            }
        }
    }

    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "AndroidId: Handling getString call");
                Object result = method.invoke(who, args);
                
                // Check if this is an Android ID related string
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    String key = (String) args[0];
                    if (key.contains("android_id") || key.contains("ANDROID_ID") || 
                        key.contains("secure_id") || key.contains("device_id")) {
                        if (result == null || "0".equals(result.toString()) || "".equals(result.toString())) {
                            Slog.w(TAG, "AndroidId: Invalid Android ID string detected, returning mock ID");
                            return generateMockAndroidId();
                        }
                    }
                }
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "AndroidId: GetString error, returning original result", e);
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("getLong")
    public static class GetLong extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "AndroidId: Handling getLong call");
                Object result = method.invoke(who, args);
                
                // Check if this is an Android ID related long value
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    String key = (String) args[0];
                    if (key.contains("android_id") || key.contains("ANDROID_ID") || 
                        key.contains("secure_id") || key.contains("device_id")) {
                        if (result == null || ((Number) result).longValue() == 0) {
                            Slog.w(TAG, "AndroidId: Invalid Android ID long detected, returning mock ID");
                            return generateMockAndroidIdLong();
                        }
                    }
                }
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "AndroidId: GetLong error, returning original result", e);
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("get")
    public static class Get extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "AndroidId: Handling get call");
                Object result = method.invoke(who, args);
                
                // Check if this is an Android ID related get
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    String key = (String) args[0];
                    if (key.contains("android_id") || key.contains("ANDROID_ID") || 
                        key.contains("secure_id") || key.contains("device_id")) {
                        if (result == null || "0".equals(result.toString()) || "".equals(result.toString())) {
                            Slog.w(TAG, "AndroidId: Invalid Android ID get detected, returning mock ID");
                            return generateMockAndroidId();
                        }
                    }
                }
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "AndroidId: Get error, returning original result", e);
                return method.invoke(who, args);
            }
        }
    }

    @ProxyMethod("read")
    public static class Read extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "AndroidId: Handling read call");
                Object result = method.invoke(who, args);
                
                // Check if this is an Android ID related read
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    String key = (String) args[0];
                    if (key.contains("android_id") || key.contains("ANDROID_ID") || 
                        key.contains("secure_id") || key.contains("device_id")) {
                        if (result == null || "0".equals(result.toString()) || "".equals(result.toString())) {
                            Slog.w(TAG, "AndroidId: Invalid Android ID read detected, returning mock ID");
                            return generateMockAndroidId();
                        }
                    }
                }
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "AndroidId: Read error, returning original result", e);
                return method.invoke(who, args);
            }
        }
    }

    // Helper methods to generate mock Android IDs
    private static String generateMockAndroidId() {
        // Generate a 16-character hex string that looks like a real Android ID
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(Integer.toHexString((int) (Math.random() * 16)));
        }
        String mockId = sb.toString().toUpperCase();
        Slog.d(TAG, "AndroidId: Generated mock Android ID: " + mockId);
        return mockId;
    }

    private static Long generateMockAndroidIdLong() {
        // Generate a long value that represents a valid Android ID
        long mockId = (long) (Math.random() * Long.MAX_VALUE);
        Slog.d(TAG, "AndroidId: Generated mock Android ID long: " + mockId);
        return mockId;
    }
}
