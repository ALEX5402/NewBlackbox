package top.niunaijun.blackbox.fake.service;

import android.provider.Settings;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Md5Utils;
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

    @ProxyMethod("getStringForUser")
    public static class GetStringForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (args != null && args.length > 1 && args[1] instanceof String) {
                    String key = (String) args[1];
                    if (Settings.Secure.ANDROID_ID.equals(key)) {
                        int userId = BActivityThread.getUserId();
                        String generatedId = generateConsistentId("ANDROID_ID", userId);
                        Slog.d(TAG, "AndroidId: Spoofing ANDROID_ID for user " + userId + " -> " + generatedId);
                        return generatedId;
                    }
                    if ("android_id".equals(key) || "advertising_id".equals(key) || "gsf_id".equals(key)) {
                        int userId = BActivityThread.getUserId();
                        String generatedId = generateConsistentId(key, userId);
                        Slog.d(TAG, "AndroidId: Spoofing " + key + " for user " + userId + " -> " + generatedId);
                        return generatedId;
                    }
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() != null) throw e.getCause();
                throw e;
            }
        }
    }

    @ProxyMethod("getString")
    public static class GetString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (args != null && args.length > 1 && args[1] instanceof String) {
                    String key = (String) args[1];
                    if (Settings.Secure.ANDROID_ID.equals(key)) {
                        int userId = BActivityThread.getUserId();
                        String generatedId = generateConsistentId("ANDROID_ID", userId);
                        Slog.d(TAG, "AndroidId: Spoofing ANDROID_ID for user " + userId + " -> " + generatedId);
                        return generatedId;
                    }
                    if ("android_id".equals(key) || "advertising_id".equals(key) || "gsf_id".equals(key)) {
                        int userId = BActivityThread.getUserId();
                        String generatedId = generateConsistentId(key, userId);
                        Slog.d(TAG, "AndroidId: Spoofing " + key + " for user " + userId + " -> " + generatedId);
                        return generatedId;
                    }
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() != null) throw e.getCause();
                throw e;
            }
        }
    }

    @ProxyMethod("getAndroidId")
    public static class GetAndroidId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            int userId = BActivityThread.getUserId();
            return generateConsistentId("ANDROID_ID", userId);
        }
    }

    public static String generateConsistentId(String type, int userId) {
        // Deterministically generate identity unique to the user space profile (User 0, User 1, User 2...)
        // This ensures identity separation and prevents duplicate nulls/dashes
        String raw = type + "_userid_" + userId + "_blackbox_secure_salt_v3";
        String md5 = Md5Utils.md5(raw);
        if (md5 != null && md5.length() >= 16) {
            if ("advertising_id".equals(type) || "gsf_id".equals(type)) {
                // Generate a UUID-like structure for ad id / gsf id
                return md5.substring(0, 8) + "-" + md5.substring(8, 12) + "-" + md5.substring(12, 16) + "-" + md5.substring(16, 20) + "-" + md5.substring(20, 32);
            }
            return md5.substring(0, 16).toLowerCase();
        }
        return "0000000000000000";
    }
}
