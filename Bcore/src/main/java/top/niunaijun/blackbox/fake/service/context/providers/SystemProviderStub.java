package top.niunaijun.blackbox.fake.service.context.providers;

import android.os.Bundle;
import android.os.IInterface;

import java.lang.reflect.Method;

import black.android.content.BRAttributionSource;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.ContextCompat;


public class SystemProviderStub extends ClassInvocationStub implements BContentProvider {
    private IInterface mBase;

    @Override
    public IInterface wrapper(IInterface contentProviderProxy, String appPkg) {
        mBase = contentProviderProxy;
        injectHook();
        return (IInterface) getProxyInvocation();
    }

    @Override
    protected Object getWho() {
        return mBase;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {

    }

    @Override
    protected void onBindMethod() {

    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if ("asBinder".equals(methodName)) {
            return method.invoke(mBase, args);
        }

        if ("call".equals(methodName)) {
            if (args != null) {
                Class<?> attributionSourceClass = BRAttributionSource.getRealClass();
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg != null && attributionSourceClass != null &&
                            arg.getClass().getName().equals(attributionSourceClass.getName())) {
                        ContextCompat.fixAttributionSourceState(arg, BlackBoxCore.getHostUid());
                    }
                }
            }
            try {
                return method.invoke(mBase, args);
            } catch (Throwable e) {
                return safeResult(methodName, method.getReturnType(), e);
            }
        }
        
        if (args != null && args.length > 0) {
            Object arg = args[0];
            if (arg instanceof String) {
                String authority = (String) arg;
                
                if (!isSystemProviderAuthority(authority)) {
                    args[0] = BlackBoxCore.getHostPkg();
                }
            } else if (arg != null) {
                Class<?> attrSourceClass = BRAttributionSource.getRealClass();
                
                if (attrSourceClass != null && arg.getClass().getName().equals(attrSourceClass.getName())) {
                    ContextCompat.fixAttributionSourceState(arg, BlackBoxCore.getHostUid());
                }
            }
        }
        try {
            return method.invoke(mBase, args);
        } catch (Throwable e) {
            return safeResult(methodName, method.getReturnType(), e);
        }
    }

    private Object safeResult(String methodName, Class<?> returnType, Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause != cause.getCause()) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        boolean ignorable = cause instanceof SecurityException
                || (message != null && (message.toLowerCase().contains("permission denial")
                || message.toLowerCase().contains("cannot write to google settings")
                || message.toLowerCase().contains("google settings table")
                || message.toLowerCase().contains("uid mismatch")));
        if (!ignorable) {
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        }
        Slog.w("SystemProviderStub", "Provider " + methodName + " denied, returning default: " + message);
        if (returnType == int.class || returnType == Integer.class) {
            return 1;
        }
        if (returnType == boolean.class || returnType == Boolean.class) {
            return true;
        }
        if (returnType == Bundle.class) {
            return new Bundle();
        }
        if ("insert".equals(methodName)) {
            return android.net.Uri.EMPTY;
        }
        if ("update".equals(methodName) || "delete".equals(methodName) || "bulkInsert".equals(methodName)) {
            return 0;
        }
        return null;
    }

    private boolean isSystemProviderAuthority(String authority) {
        if (authority == null) return false;
        
        return authority.equals("settings") || 
               authority.equals("media") || 
               authority.equals("downloads") || 
               authority.equals("contacts") || 
               authority.equals("call_log") || 
               authority.equals("telephony") || 
               authority.equals("calendar") || 
               authority.equals("browser") || 
               authority.equals("user_dictionary") || 
               authority.equals("applications") ||
               authority.contains("gservices") ||
               authority.contains("googlesettings") ||
               authority.contains("com.google.settings") ||
               authority.contains("appdoctor") ||
               authority.startsWith("com.google.") ||
               authority.startsWith("com.android.") ||
               authority.startsWith("android.");
    }
}
