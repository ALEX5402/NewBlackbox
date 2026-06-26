package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.utils.Slog;

public class NetworkPermissionCompat implements IInjectHook {
    private static final String TAG = "NetworkPermissionCompat";

    private static volatile boolean sInstalled;

    @Override
    public void injectHook() {
        install();
    }

    @Override
    public boolean isBadEnv() {
        return !sInstalled;
    }

    public static void install() {
        if (sInstalled) {
            return;
        }
        synchronized (NetworkPermissionCompat.class) {
            if (sInstalled) {
                return;
            }
            try {
                Class<?> permissionManager = Class.forName("android.permission.PermissionManager");
                Method disablePermissionCache = permissionManager.getDeclaredMethod("disablePermissionCache");
                disablePermissionCache.setAccessible(true);
                disablePermissionCache.invoke(null);
                sInstalled = true;
                Slog.d(TAG, "disabled framework permission cache");
            } catch (Throwable e) {
                Slog.w(TAG, "install failed: " + e.getMessage(), e);
            }
        }
    }
}
