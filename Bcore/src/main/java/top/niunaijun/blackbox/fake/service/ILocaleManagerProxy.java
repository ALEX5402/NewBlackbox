package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Swallows locale updates made from inside the virtual space.
 *
 * Google Play Store (and other apps) call
 * LocaleManager.setApplicationLocales() during init. That call lands on the
 * system ILocaleManager service, which enforces the signature-level
 * android.permission.CHANGE_CONFIGURATION. A virtualized process does not
 * hold that permission, so the call throws:
 *
 *   SecurityException: setApplicationLocales: Neither user <uid> nor current
 *   process has android.permission.CHANGE_CONFIGURATION.
 *
 * and takes the whole process down before Play Store finishes starting.
 * Per-app locale is a pure UX preference, so acknowledging the call and
 * discarding it is safe and unblocks Play Store startup.
 */
public class ILocaleManagerProxy extends BinderInvocationStub {

    public static final String TAG = "ILocaleManagerProxy";

    public ILocaleManagerProxy() {
        super(BRServiceManager.get().getService("locale"));
    }

    @Override
    protected Object getWho() {
        return BRServiceManager.get().getService("locale");
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("locale");
    }

    @Override
    public boolean isBadEnv() {
        return BRServiceManager.get().getService("locale") == null;
    }

    @ProxyMethod("setApplicationLocales")
    public static class SetApplicationLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "setApplicationLocales intercepted; ignored for virtual process");
            return null;
        }
    }

    @ProxyMethod("getApplicationLocales")
    public static class GetApplicationLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable t) {
                Slog.w(TAG, "getApplicationLocales failed, returning null", t);
                return null;
            }
        }
    }

    @ProxyMethod("setSystemLocales")
    public static class SetSystemLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "setSystemLocales intercepted; ignored for virtual process");
            return null;
        }
    }
}
