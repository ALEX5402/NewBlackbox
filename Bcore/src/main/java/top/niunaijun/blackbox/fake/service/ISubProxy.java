package top.niunaijun.blackbox.fake.service;

import android.os.IBinder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

import black.android.os.BRServiceManager;
import black.com.android.internal.telephony.BRISubStub;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Slog;


public class ISubProxy extends BinderInvocationStub {
    public static final String TAG = "ISubProxy";
    public static final String SERVICE_NAME = "isub";

    public ISubProxy() {
        super(resolveSubBinder());
    }

    private static IBinder resolveSubBinder() {
        try {
            IBinder binder = BRServiceManager.get().getService(SERVICE_NAME);
            if (binder != null) {
                return binder;
            }
        } catch (Throwable ignored) {
        }
        return new GmsChimeraStub();
    }

    @Override
    protected Object getWho() {
        IBinder binder = resolveSubBinder();
        if (binder instanceof GmsChimeraStub) {
            Slog.w(TAG, "isub binder missing, using Chimera-safe stub");
            return binder;
        }
        try {
            return BRISubStub.get().asInterface(binder);
        } catch (Throwable e) {
            Slog.w(TAG, "Failed to obtain ISub interface, using stub", e);
            return new GmsChimeraStub();
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(SERVICE_NAME);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodParameterUtils.replaceFirstAppPkg(args);
        try {
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            Slog.w(TAG, "SecurityException in " + method.getName() + ", returning safe default");
            return safeResult(method);
        } catch (Throwable e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof SecurityException
                    || cause instanceof android.os.DeadObjectException
                    || cause instanceof android.os.RemoteException) {
                Slog.w(TAG, "Binder error in " + method.getName() + ", returning safe default: " + cause.getMessage());
                return safeResult(method);
            }
            throw e;
        }
    }

    @ProxyMethod("getActiveSubIdList")
    public static class GetActiveSubIdList extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                MethodParameterUtils.replaceFirstAppPkg(args);
                return method.invoke(who, args);
            } catch (Throwable e) {
                Slog.w(TAG, "getActiveSubIdList denied, returning empty list");
                return new int[0];
            }
        }
    }

    @ProxyMethod("getActiveSubscriptionIdList")
    public static class GetActiveSubscriptionIdList extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                MethodParameterUtils.replaceFirstAppPkg(args);
                return method.invoke(who, args);
            } catch (Throwable e) {
                Slog.w(TAG, "getActiveSubscriptionIdList denied, returning empty list");
                return new int[0];
            }
        }
    }

    @ProxyMethod("getSubId")
    public static class GetSubId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return new int[0];
            }
        }
    }

    @ProxyMethod("getDefaultSubId")
    public static class GetDefaultSubId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return -1;
            }
        }
    }

    @ProxyMethod("getDefaultDataSubId")
    public static class GetDefaultDataSubId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return -1;
            }
        }
    }

    @ProxyMethod("getDefaultSmsSubId")
    public static class GetDefaultSmsSubId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return -1;
            }
        }
    }

    @ProxyMethod("getDefaultVoiceSubId")
    public static class GetDefaultVoiceSubId extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Throwable e) {
                return -1;
            }
        }
    }

    @ProxyMethod("getActiveSubscriptionInfoList")
    public static class GetActiveSubscriptionInfoList extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                MethodParameterUtils.replaceFirstAppPkg(args);
                return method.invoke(who, args);
            } catch (Throwable e) {
                return new ArrayList<>();
            }
        }
    }

    @ProxyMethod("getActiveSubscriptionInfoCount")
    public static class GetActiveSubscriptionInfoCount extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                MethodParameterUtils.replaceFirstAppPkg(args);
                return method.invoke(who, args);
            } catch (Throwable e) {
                return 0;
            }
        }
    }

    private static Object safeResult(Method method) {
        Class<?> type = method.getReturnType();
        if (type == void.class || type == Void.class) {
            return null;
        }
        if (type == int.class || type == Integer.class) {
            return -1;
        }
        if (type == long.class || type == Long.class) {
            return -1L;
        }
        if (type == boolean.class || type == Boolean.class) {
            return false;
        }
        if (type == int[].class) {
            return new int[0];
        }
        if (type == String.class) {
            return null;
        }
        if (java.util.List.class.isAssignableFrom(type)) {
            return Collections.emptyList();
        }
        return null;
    }
}
