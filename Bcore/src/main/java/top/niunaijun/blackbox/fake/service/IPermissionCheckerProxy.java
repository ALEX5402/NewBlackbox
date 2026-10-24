package top.niunaijun.blackbox.fake.service;

import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

public class IPermissionCheckerProxy extends BinderInvocationStub {
    public static final String TAG = "PermissionCheckerProxy";
    private static final String SERVICE_NAME = "permission_checker";

    public IPermissionCheckerProxy() {
        super(BRServiceManager.get().getService(SERVICE_NAME));
    }

    @Override
    protected Object getWho() {
        try {
            IBinder binder = BRServiceManager.get().getService(SERVICE_NAME);
            if (binder == null) return null;
            Class<?> stubClass = Class.forName("android.permission.IPermissionChecker$Stub");
            Method asInterface = stubClass.getMethod("asInterface", IBinder.class);
            return (IInterface) asInterface.invoke(null, binder);
        } catch (Throwable e) {
            Slog.e(TAG, "Failed to get IPermissionChecker", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(SERVICE_NAME);
        try {
            Class<?> pcClass = Class.forName("android.content.PermissionChecker");
            Field sServiceField = pcClass.getDeclaredField("sService");
            sServiceField.setAccessible(true);
            sServiceField.set(null, proxyInvocation);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("checkPermission")
    public static class CheckPermission extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0; // PERMISSION_GRANTED
        }
    }

    @ProxyMethod("checkOp")
    public static class CheckOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0; // PERMISSION_GRANTED
        }
    }

    @ProxyMethod("finishDataDelivery")
    public static class FinishDataDelivery extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }
}
