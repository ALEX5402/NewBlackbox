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
        Object googleSettings = handleGoogleSettings(method.getName(), args);
        if (googleSettings != null) {
            return googleSettings;
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
        
        
        methodName = method.getName();
        if (methodName.equals("query") || methodName.equals("insert") || 
            methodName.equals("update") || methodName.equals("delete") || 
            methodName.equals("bulkInsert") || methodName.equals("call")) {
            
            
            try {
                return method.invoke(mBase, args);
            } catch (Throwable e) {
                
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
                
                
                if (methodName.equals("call")) {
                    Slog.w(TAG, "Error in call method, returning safe default: " + e.getMessage());
                    return getSafeDefaultValue(methodName, method.getReturnType());
                }
                
                throw e.getCause();
            }
        }
        
        
        try {
            return method.invoke(mBase, args);
        } catch (Throwable e) {
            
            Throwable cause = e.getCause();
            if (isUidMismatchError(cause)) {
                Slog.w(TAG, "UID mismatch in " + methodName + ", returning safe default: " + cause.getMessage());
                return getSafeDefaultValue(methodName, method.getReturnType());
            }
            throw e.getCause();
        }
    }
    
    private static final String GOOGLE_SETTINGS_AUTHORITY = "com.google.settings";

    private static Object handleGoogleSettings(String methodName, Object[] args) {
        if (args == null) return null;
        android.net.Uri uri = null;
        for (Object arg : args) {
            if (arg instanceof android.net.Uri) {
                uri = (android.net.Uri) arg;
                break;
            }
        }
        if (uri == null || !GOOGLE_SETTINGS_AUTHORITY.equals(uri.getAuthority())
                || !"partner".equals(uri.getPathSegments().isEmpty() ? null : uri.getPathSegments().get(0))) {
            return null;
        }
        try {
            if ("insert".equals(methodName)) {
                for (Object arg : args) {
                    if (arg instanceof android.content.ContentValues) {
                        android.content.ContentValues values = (android.content.ContentValues) arg;
                        String name = values.getAsString("name");
                        if (name == null) return null;
                        storeGoogleSetting(name, values.getAsString("value"));
                        return android.net.Uri.withAppendedPath(uri, name);
                    }
                }
            } else if ("query".equals(methodName)) {
                String name = selectedGoogleSetting(args);
                if (name == null) return null;
                java.util.Properties settings = loadGoogleSettings();
                if (!settings.containsKey(name)) return null;
                String[] projection = null;
                for (Object arg : args) {
                    if (arg instanceof String[]) {
                        projection = (String[]) arg;
                        break;
                    }
                }
                if (projection == null) projection = new String[]{"_id", "name", "value"};
                Object[] row = new Object[projection.length];
                for (int i = 0; i < projection.length; i++) {
                    if ("name".equals(projection[i])) row[i] = name;
                    else if ("value".equals(projection[i])) row[i] = settings.getProperty(name);
                    else if ("_id".equals(projection[i])) row[i] = 1;
                }
                android.database.MatrixCursor cursor = new android.database.MatrixCursor(projection, 1);
                cursor.addRow(row);
                return cursor;
            }
        } catch (Throwable e) {
            Slog.w(TAG, "Google settings virtualization failed", e);
        }
        return null;
    }

    private static String selectedGoogleSetting(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Bundle) {
                String[] selectionArgs = ((Bundle) arg).getStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS);
                if (selectionArgs != null && selectionArgs.length > 0) return selectionArgs[0];
            }
        }
        return null;
    }

    private static java.io.File googleSettingsFile() {
        return new java.io.File(BlackBoxCore.getContext().getFilesDir(), "google_settings_" + BActivityThread.getUserId() + ".properties");
    }

    private static synchronized java.util.Properties loadGoogleSettings() {
        java.util.Properties settings = new java.util.Properties();
        java.io.File file = googleSettingsFile();
        if (file.exists()) {
            try (java.io.InputStream in = new java.io.FileInputStream(file)) {
                settings.load(in);
            } catch (java.io.IOException e) {
                Slog.w(TAG, "Cannot read " + file, e);
            }
        }
        return settings;
    }

    private static synchronized void storeGoogleSetting(String name, String value) throws java.io.IOException {
        java.util.Properties settings = loadGoogleSettings();
        if (value == null) settings.remove(name);
        else settings.setProperty(name, value);
        try (java.io.OutputStream out = new java.io.FileOutputStream(googleSettingsFile())) {
            settings.store(out, null);
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
    
    
    private boolean isUidMismatchError(Throwable error) {
        if (error == null) return false;
        
        String message = error.getMessage();
        if (message == null) return false;
        
        
        return message.contains("Calling uid") && 
               message.contains("doesn't match source uid") ||
               message.contains("uid") && 
               message.contains("permission") ||
               message.contains("SecurityException") ||
               message.contains("UID mismatch");
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
