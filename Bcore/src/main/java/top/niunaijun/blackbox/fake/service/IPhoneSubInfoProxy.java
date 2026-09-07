package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import black.android.telephony.BRTelephonyManager;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.MethodParameterUtils;


public class IPhoneSubInfoProxy extends ClassInvocationStub {
    public static final String TAG = "IPhoneSubInfoProxy";

    public IPhoneSubInfoProxy() {
        if (BRTelephonyManager.get()._check_sServiceHandleCacheEnabled() != null) {
            BRTelephonyManager.get()._set_sServiceHandleCacheEnabled(true);
        }
        if (BRTelephonyManager.get()._check_getSubscriberInfoService() != null) {
            BRTelephonyManager.get().getSubscriberInfoService();
        }
    }

    @Override
    protected Object getWho() {
        return BRTelephonyManager.get().sIPhoneSubInfo();
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        BRTelephonyManager.get()._set_sIPhoneSubInfo(proxyInvocation);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodParameterUtils.replaceFirstAppPkg(args);
        try {
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            return phoneSubSafeResult(method);
        } catch (Throwable e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof SecurityException || cause instanceof android.os.RemoteException) {
                return phoneSubSafeResult(method);
            }
            throw e;
        }
    }

    private static Object phoneSubSafeResult(Method method) {
        Class<?> type = method.getReturnType();
        if (type == void.class || type == Void.class) {
            return null;
        }
        if (type == int.class || type == Integer.class) {
            return 0;
        }
        if (type == boolean.class || type == Boolean.class) {
            return false;
        }
        if (type == int[].class) {
            return new int[0];
        }
        return null;
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }


    @ProxyMethod("getLine1NumberForSubscriber")
    public static class getLine1NumberForSubscriber extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return null;
        }
    }
}
