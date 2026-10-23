package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


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

    
    private static String sMockAndroidId = null;
    private static String generateMockAndroidId() {
        if (sMockAndroidId != null) return sMockAndroidId;
        try {
            android.content.Context ctx = top.niunaijun.blackbox.BlackBoxCore.getContext();
            if (ctx != null) {
                String realId = android.provider.Settings.Secure.getString(ctx.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                if (realId != null && !realId.isEmpty()) {
                    sMockAndroidId = realId;
                    return sMockAndroidId;
                }
            }
        } catch (Throwable ignored) {
        }
        sMockAndroidId = "eaba332128121734";
        return sMockAndroidId;
    }

    private static Long generateMockAndroidIdLong() {
        try {
            return Long.parseUnsignedLong(generateMockAndroidId(), 16);
        } catch (Throwable t) {
            return 0x1234567890abcdefL;
        }
    }
}
