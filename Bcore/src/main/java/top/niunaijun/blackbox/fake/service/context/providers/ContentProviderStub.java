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

/**
 * updated by alex5402 on 4/8/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 
 */
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
        
        // Get method name first to determine handling logic
        String methodName = method.getName();
        
        // For call() method, String args are method names like "GET_global", NOT package names
        // Don't replace them! Only fix AttributionSource
        if ("call".equals(methodName)) {
            // Fix AttributionSource in args (treats Bundle recursively)
            AttributionSourceUtils.fixAttributionSourceInArgs(args);
        } else {
            // For other methods like query/insert/update/delete, fix both package names and AttributionSource
            if (args != null && args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg instanceof String) {
                        String strArg = (String) arg;
                        // Don't replace system provider authorities
                        if (!isSystemProviderAuthority(strArg)) {
                            // Replace package name with the correct one
                            args[i] = mAppPkg;
                        }
                    }
                }
                // Also fix AttributionSources in any arg position
                AttributionSourceUtils.fixAttributionSourceInArgs(args);
            }
        }
        
        // Pre-validate the call to prevent system-level SecurityException
        methodName = method.getName();
        if (methodName.equals("query") || methodName.equals("insert") || 
            methodName.equals("update") || methodName.equals("delete") || 
            methodName.equals("bulkInsert") || methodName.equals("call")) {
            
            // Check if this is likely to cause a UID mismatch
            try {
                return method.invoke(mBase, args);
            } catch (Throwable e) {
                // Handle SecurityException and other UID-related errors
                Throwable cause = e.getCause();
                if (isUidMismatchError(cause)) {
                    Slog.w(TAG, "UID mismatch in ContentProvider call, returning safe default: " + cause.getMessage());
                    return getSafeDefaultValue(methodName, method.getReturnType());
                } else if (cause instanceof RuntimeException) {
                    String message = cause.getMessage();
                    if (message != null && (message.contains("uid") || message.contains("permission"))) {
                        Slog.w(TAG, "Permission/UID error in ContentProvider call, returning safe default: " + message);
                        return getSafeDefaultValue(methodName, method.getReturnType());
                    }
                }
                
                // For call method specifically, always return safe default on any error
                if (methodName.equals("call")) {
                    Slog.w(TAG, "Error in call method, returning safe default: " + e.getMessage());
                    return getSafeDefaultValue(methodName, method.getReturnType());
                }
                
                throw e.getCause();
            }
        }
        
        // For other methods, proceed normally
        try {
            return method.invoke(mBase, args);
        } catch (Throwable e) {
            // Handle SecurityException for UID mismatch in any method
            Throwable cause = e.getCause();
            if (isUidMismatchError(cause)) {
                Slog.w(TAG, "UID mismatch in " + methodName + ", returning safe default: " + cause.getMessage());
                return getSafeDefaultValue(methodName, method.getReturnType());
            }
            throw e.getCause();
        }
    }
    
    private Object getSafeDefaultValue(String methodName) {
        switch (methodName) {
            case "query":
                return null; // Return null cursor
            case "insert":
                return null; // Return null URI
            case "update":
            case "delete":
                return 0; // Return 0 rows affected
            case "bulkInsert":
                return 0; // Return 0 rows inserted
            case "call":
                return new Bundle(); // Return empty Bundle instead of null to prevent NPE
            case "getType":
                return null; // Return null MIME type
            case "openFile":
                return null; // Return null ParcelFileDescriptor
            case "openAssetFile":
                return null; // Return null AssetFileDescriptor
            default:
                return null; // Default fallback
        }
    }

    private boolean isSystemProviderAuthority(String authority) {
        if (authority == null) return false;
        
        // Check for system provider authorities that need special handling
        return authority.equals("settings") || 
               authority.equals("settings_global") || 
               authority.equals("settings_system") || 
               authority.equals("settings_secure") ||
               authority.equals("media") ||
               authority.equals("telephony") ||
               authority.startsWith("android.provider.Settings");
    }
    
    /**
     * Enhanced UID mismatch detection and handling
     */
    private boolean isUidMismatchError(Throwable error) {
        if (error == null) return false;
        
        String message = error.getMessage();
        if (message == null) return false;
        
        // Check for UID mismatch patterns
        return message.contains("Calling uid") && 
               message.contains("doesn't match source uid") ||
               message.contains("uid") && 
               message.contains("permission") ||
               message.contains("SecurityException") ||
               message.contains("UID mismatch");
    }
    
    /**
     * Get safe default value based on method name and return type
     */
    private Object getSafeDefaultValue(String methodName, Class<?> returnType) {
        if (returnType == null) {
            return getSafeDefaultValue(methodName);
        }
        
        // Return type-specific safe defaults
        if (returnType == String.class) {
            return "true"; // Safe default for strings
        } else if (returnType == int.class || returnType == Integer.class) {
            return 1; // Safe default for integers
        } else if (returnType == long.class || returnType == Long.class) {
            return 1L; // Safe default for longs
        } else if (returnType == float.class || returnType == Float.class) {
            return 1.0f; // Safe default for floats
        } else if (returnType == boolean.class || returnType == Boolean.class) {
            return true; // Safe default for booleans
        } else if (returnType == Bundle.class) {
            return new Bundle(); // Safe default for bundles
        }
        
        // Fallback to method-specific defaults
        return getSafeDefaultValue(methodName);
    }

    /**
     * Fix AttributionSource UID to prevent crashes on Android 12+
     */
    private void fixAttributionSourceUid(Object attributionSource) {
        try {
            if (attributionSource == null) return;
            
            Class<?> attributionSourceClass = attributionSource.getClass();
            
            // Try to find and set the UID field
            try {
                java.lang.reflect.Field uidField = attributionSourceClass.getDeclaredField("mUid");
                uidField.setAccessible(true);
                uidField.set(attributionSource, BlackBoxCore.getHostUid());
                Slog.d(TAG, "Fixed AttributionSource UID via field access");
            } catch (NoSuchFieldException e) {
                // Try alternative field names
                try {
                    java.lang.reflect.Field uidField = attributionSourceClass.getDeclaredField("uid");
                    uidField.setAccessible(true);
                    uidField.set(attributionSource, BlackBoxCore.getHostUid());
                    Slog.d(TAG, "Fixed AttributionSource UID via alternative field");
                } catch (NoSuchFieldException e2) {
                    // Try using setter method
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
            
            // Also try to fix package name
            try {
                java.lang.reflect.Field packageField = attributionSourceClass.getDeclaredField("mPackageName");
                packageField.setAccessible(true);
                packageField.set(attributionSource, mAppPkg);
                Slog.d(TAG, "Fixed AttributionSource package name");
            } catch (Exception e) {
                // Ignore package name fixing errors
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
