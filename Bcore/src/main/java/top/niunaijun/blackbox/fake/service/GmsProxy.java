package top.niunaijun.blackbox.fake.service;

import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


public class GmsProxy extends BinderInvocationStub {
    public static final String TAG = "GmsProxy";
    public static final String SERVICE_NAME = "gms";

    public GmsProxy() {
        super(resolveGmsBinder());
    }

    private static IBinder resolveGmsBinder() {
        try {
            IBinder binder = BRServiceManager.get().getService(SERVICE_NAME);
            if (binder != null) {
                return binder;
            }
        } catch (Throwable e) {
            Slog.w(TAG, "Unable to query gms service binder", e);
        }
        Slog.d(TAG, "No system gms binder, using Chimera stub");
        return new GmsChimeraStub();
    }

    @Override
    protected Object getWho() {
        IBinder binder = resolveGmsBinder();
        Object broker = asGmsBroker(binder);
        if (broker != null) {
            return broker;
        }
        if (binder instanceof IInterface) {
            return binder;
        }
        return new GmsChimeraStub();
    }

    private static Object asGmsBroker(IBinder binder) {
        if (binder == null) {
            return null;
        }
        String[] stubNames = new String[]{
                "com.google.android.gms.common.internal.IGmsServiceBroker$Stub",
                "com.google.android.gms.common.api.internal.IGmsServiceBroker$Stub"
        };
        for (String stubName : stubNames) {
            try {
                Class<?> stubClass = Class.forName(stubName);
                Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
                Object iface = asInterfaceMethod.invoke(null, binder);
                if (iface != null) {
                    Slog.d(TAG, "Successfully obtained IGmsServiceBroker via " + stubName);
                    return iface;
                }
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable e) {
                Slog.w(TAG, "Failed to get IGmsServiceBroker via " + stubName, e);
            }
        }
        return null;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(SERVICE_NAME);
        Slog.d(TAG, "GmsProxy injected");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return super.invoke(proxy, method, args);
        } catch (Throwable e) {
            Slog.w(TAG, "GmsProxy invoke failed for " + method.getName() + ", returning stub");
            Class<?> type = method.getReturnType();
            if (type == IBinder.class || IBinder.class.isAssignableFrom(type) || IInterface.class.isAssignableFrom(type)) {
                return new GmsChimeraStub();
            }
            if (type == boolean.class || type == Boolean.class) {
                return true;
            }
            if (type == int.class || type == Integer.class) {
                return 0;
            }
            return null;
        }
    }

    @ProxyMethod("getService")
    public static class GetService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (args != null && args.length > 0 && args[0] instanceof String) {
                    String callingPackage = (String) args[0];
                    if ("com.google.android.gms".equals(callingPackage)
                            || "com.google.android.gsf".equals(callingPackage)) {
                        args[0] = BlackBoxCore.getHostPkg();
                        Slog.d(TAG, "GmsProxy: Fixed calling package from " + callingPackage + " to " + BlackBoxCore.getHostPkg());
                    }
                }
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: Error in getService, returning Chimera stub", e);
            }
            return new GmsChimeraStub();
        }
    }

    @ProxyMethod("getServiceBroker")
    public static class GetServiceBroker extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: Error in getServiceBroker, returning Chimera stub", e);
            }
            return new GmsChimeraStub();
        }
    }

    @ProxyMethod("authenticate")
    public static class Authenticate extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling authenticate call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: Authentication error, returning success", e);
                return createMockAuthResult();
            }
        }
    }

    @ProxyMethod("getAccount")
    public static class GetAccount extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling getAccount call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: GetAccount error, returning null", e);
                return null;
            }
        }
    }

    @ProxyMethod("getToken")
    public static class GetToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling getToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: GetToken error, returning mock token", e);
                return "mock_gms_token_" + System.currentTimeMillis();
            }
        }
    }

    @ProxyMethod("invalidateToken")
    public static class InvalidateToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling invalidateToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: InvalidateToken error, ignoring", e);
                return null;
            }
        }
    }

    @ProxyMethod("clearToken")
    public static class ClearToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling clearToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: ClearToken error, ignoring", e);
                return null;
            }
        }
    }

    private static Object createMockAuthResult() {
        try {
            Class<?> bundleClass = Class.forName("android.os.Bundle");
            return bundleClass.newInstance();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create mock auth result", e);
            return null;
        }
    }
}
