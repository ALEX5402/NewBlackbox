package top.niunaijun.blackbox.fake.service;

import android.os.IInterface;
import android.os.LocaleList;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class ILocaleManagerProxy extends BinderInvocationStub {
    public static final String TAG = "LocaleManagerProxy";

    public ILocaleManagerProxy() {
        super(BRServiceManager.get().getService("locale"));
    }

    @Override
    protected Object getWho() {
        try {
            Class<?> stubClass = Class.forName("android.app.ILocaleManager$Stub");
            Method asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder.class);
            return (IInterface) asInterfaceMethod.invoke(null, BRServiceManager.get().getService("locale"));
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get ILocaleManager interface", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("locale");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("setApplicationLocales")
    public static class SetApplicationLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "hook setApplicationLocales: bypassed SecurityException");
            return null;
        }
    }

    @ProxyMethod("getApplicationLocales")
    public static class GetApplicationLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "hook getApplicationLocales");
            return LocaleList.getEmptyLocaleList();
        }
    }

    @ProxyMethod("getSystemLocales")
    public static class GetSystemLocales extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return LocaleList.getDefault();
        }
    }

    @ProxyMethod("getOverrideLocaleConfig")
    public static class GetOverrideLocaleConfig extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

    @ProxyMethod("setOverrideLocaleConfig")
    public static class SetOverrideLocaleConfig extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }
}
