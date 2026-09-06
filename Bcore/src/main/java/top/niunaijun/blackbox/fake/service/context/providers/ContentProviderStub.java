package top.niunaijun.blackbox.fake.service.context.providers;

import android.os.IInterface;

import java.lang.reflect.Method;

import black.android.content.BRAttributionSource;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.utils.compat.ContextCompat;
import top.niunaijun.blackbox.utils.Slog;
import android.os.Bundle;
import top.niunaijun.blackbox.utils.AttributionSourceUtils;


public class ContentProviderStub extends ClassInvocationStub implements BContentProvider {
    public static final String TAG = "ContentProviderStub";
    private IInterface mBase;
    private String mAppPkg;

    public IInterface wrapper(final IInterface contentProviderProxy, final String appPkg) {
        mBase = contentProviderProxy;
        mAppPkg = appPkg;
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
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if ("asBinder".equals(method.getName())) {
        return method.invoke(mBase, args);
    }

    String methodName = method.getName();

    if ("call".equals(methodName)) {
        AttributionSourceUtils.fixAttributionSourceInArgs(args);
    } else {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof String) {
                    String strArg = (String) arg;
                    if (!isSystemProviderAuthority(strArg)) {
                        args[i] = mAppPkg;
                    }
                }
            }
            AttributionSourceUtils.fixAttributionSourceInArgs(args);
        }
    }

    try {
        if (methodName.equals("query") || methodName.equals("insert")
                || methodName.equals("update") || methodName.equals("delete")
                || methodName.equals("bulkInsert") || methodName.equals("call")) {

            try {
                return method.invoke(mBase, args);
            } catch (Throwable e) {
                Throwable cause = unwrap(e);
                if (isIgnorableProviderError(cause)) {
                    Slog.w(TAG, "Provider " + methodName + " denied, returning safe default: " + cause.getMessage());
                    return getSafeDefaultValue(methodName, method.getReturnType());
                }
                if (methodName.equals("call") || methodName.equals("query")) {
                    Slog.w(TAG, "Error in " + methodName + ", returning safe default: " + e.getMessage());
                    return getSafeDefaultValue(methodName, method.getReturnType());
                }
                if (cause != null) {
                    throw cause;
                }
                throw e;
            }
        }

        return method.invoke(mBase, args);
    } catch (Throwable e) {
        Throwable cause = unwrap(e);
        if (isIgnorableProviderError(cause)) {
            Slog.w(TAG, "Provider " + methodName + " denied, returning safe default: " + cause.getMessage());
            return getSafeDefaultValue(methodName, method.getReturnType());
        }
        if (cause != null) {
            throw cause;
        }
        throw e;
    }
}
    
    private Object getSafeDefaultValue(String methodName) {
        switch (methodName) {
            case "query":
                return null; 
            case "insert":
                return null; 
            case "update":
            case "delete":
                return 0; 
            case "bulkInsert":
                return 0; 
            case "call":
                return new Bundle(); 
            case "getType":
                return null; 
            case "openFile":
                return null; 
            case "openAssetFile":
                return null; 
            default:
                return null; 
        }
    }

    private boolean isSystemProviderAuthority(String authority) {
        if (authority == null) return false;
        
        
        return authority.equals("settings") || 
               authority.equals("settings_global") || 
               authority.equals("settings_system") || 
               authority.equals("settings_secure") ||
               authority.equals("media") ||
               authority.equals("telephony") ||
               authority.startsWith("android.provider.Settings");
    }
    
    
    private static Throwable unwrap(Throwable error) {
        Throwable cause = error;
        while (cause instanceof java.lang.reflect.InvocationTargetException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        if (cause != null && cause.getCause() != null
                && (cause instanceof RuntimeException || cause instanceof java.lang.reflect.UndeclaredThrowableException)) {
            return cause.getCause();
        }
        return cause != null ? cause : error;
    }

    private boolean isIgnorableProviderError(Throwable error) {
        if (error == null) {
            return false;
        }
        if (error instanceof SecurityException) {
            return true;
        }
        String message = error.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("permission denial")
                || lower.contains("uid mismatch")
                || (lower.contains("calling uid") && lower.contains("doesn't match source uid"))
                || (lower.contains("uid") && lower.contains("permission"));
    }
    
    
    private Object getSafeDefaultValue(String methodName, Class<?> returnType) {
        if (returnType == null) {
            return getSafeDefaultValue(methodName);
        }
        
        
        if (returnType == String.class) {
            return "true"; 
        } else if (returnType == int.class || returnType == Integer.class) {
            return 1; 
        } else if (returnType == long.class || returnType == Long.class) {
            return 1L; 
        } else if (returnType == float.class || returnType == Float.class) {
            return 1.0f; 
        } else if (returnType == boolean.class || returnType == Boolean.class) {
            return true; 
        } else if (returnType == Bundle.class) {
            return new Bundle(); 
        }
        
        
        return getSafeDefaultValue(methodName);
    }

    
    private void fixAttributionSourceUid(Object attributionSource) {
        try {
            if (attributionSource == null) return;
            
            Class<?> attributionSourceClass = attributionSource.getClass();
            
            
            try {
                java.lang.reflect.Field uidField = attributionSourceClass.getDeclaredField("mUid");
                uidField.setAccessible(true);
                uidField.set(attributionSource, BlackBoxCore.getHostUid());
                Slog.d(TAG, "Fixed AttributionSource UID via field access");
            } catch (NoSuchFieldException e) {
                
                try {
                    java.lang.reflect.Field uidField = attributionSourceClass.getDeclaredField("uid");
                    uidField.setAccessible(true);
                    uidField.set(attributionSource, BlackBoxCore.getHostUid());
                    Slog.d(TAG, "Fixed AttributionSource UID via alternative field");
                } catch (NoSuchFieldException e2) {
                    
                    try {
                        java.lang.reflect.Method setUidMethod = attributionSourceClass.getDeclaredMethod("setUid", int.class);
                        setUidMethod.setAccessible(true);
                        setUidMethod.invoke(attributionSource, BlackBoxCore.getHostUid());
                        Slog.d(TAG, "Fixed AttributionSource UID via setter method");
                    } catch (Exception e3) {
                        Slog.w(TAG, "Could not fix AttributionSource UID: " + e3.getMessage());
                    }
                }
            }
            
            
            try {
                java.lang.reflect.Field packageField = attributionSourceClass.getDeclaredField("mPackageName");
                packageField.setAccessible(true);
                packageField.set(attributionSource, mAppPkg);
                Slog.d(TAG, "Fixed AttributionSource package name");
            } catch (Exception e) {
                
            }
            
        } catch (Exception e) {
            Slog.w(TAG, "Error fixing AttributionSource UID: " + e.getMessage());
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }
}
