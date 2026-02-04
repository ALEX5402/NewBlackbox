package top.niunaijun.blackbox.utils;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.utils.Slog;


public class AttributionSourceUtils {
    private static final String TAG = "AttributionSourceUtils";

    
    public static void fixAttributionSourceInArgs(Object[] args) {
        if (args == null) return;
        
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg != null && arg.getClass().getName().contains("AttributionSource")) {
                try {
                    fixAttributionSourceUid(arg);
                    Slog.d(TAG, "Fixed AttributionSource UID in method arguments");
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to fix AttributionSource in args: " + e.getMessage());
                }
            }
        }
        
        
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg != null && arg.getClass().getName().contains("Bundle")) {
                try {
                    fixAttributionSourceInBundle(arg);
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to fix AttributionSource in Bundle: " + e.getMessage());
                }
            }
        }
    }

    
    public static void fixAttributionSourceUid(Object attributionSource) {
        try {
            if (attributionSource == null) return;
            
            Class<?> attributionSourceClass = attributionSource.getClass();
            
            
            String[] uidFieldNames = {"mUid", "uid", "mCallingUid", "callingUid", "mSourceUid", "sourceUid"};
            
            for (String fieldName : uidFieldNames) {
                try {
                    java.lang.reflect.Field uidField = attributionSourceClass.getDeclaredField(fieldName);
                    uidField.setAccessible(true);
                    uidField.set(attributionSource, BlackBoxCore.getHostUid());
                    Slog.d(TAG, "Fixed AttributionSource UID via field: " + fieldName);
                    break;
                } catch (NoSuchFieldException e) {
                    
                }
            }
            
            
            try {
                java.lang.reflect.Method setUidMethod = attributionSourceClass.getDeclaredMethod("setUid", int.class);
                setUidMethod.setAccessible(true);
                setUidMethod.invoke(attributionSource, BlackBoxCore.getHostUid());
                Slog.d(TAG, "Fixed AttributionSource UID via setter method");
            } catch (Exception e) {
                
            }
            
            
            String[] packageFieldNames = {"mPackageName", "packageName", "mSourcePackage", "sourcePackage"};
            
            for (String fieldName : packageFieldNames) {
                try {
                    java.lang.reflect.Field packageField = attributionSourceClass.getDeclaredField(fieldName);
                    packageField.setAccessible(true);
                    packageField.set(attributionSource, BlackBoxCore.getHostPkg());
                    Slog.d(TAG, "Fixed AttributionSource package name via field: " + fieldName);
                    break;
                } catch (NoSuchFieldException e) {
                    
                }
            }
            
        } catch (Exception e) {
            Slog.w(TAG, "Error fixing AttributionSource UID: " + e.getMessage());
        }
    }

    
    public static void fixAttributionSourceInBundle(Object bundle) {
        try {
            if (bundle == null) return;
            
            
            java.lang.reflect.Method keySetMethod = bundle.getClass().getMethod("keySet");
            java.util.Set<String> keys = (java.util.Set<String>) keySetMethod.invoke(bundle);
            
            for (String key : keys) {
                try {
                    java.lang.reflect.Method getMethod = bundle.getClass().getMethod("get", String.class);
                    Object value = getMethod.invoke(bundle, key);
                    
                    if (value != null && value.getClass().getName().contains("AttributionSource")) {
                        fixAttributionSourceUid(value);
                        Slog.d(TAG, "Fixed AttributionSource UID in Bundle key: " + key);
                    }
                } catch (Exception e) {
                    
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error fixing AttributionSource in Bundle: " + e.getMessage());
        }
    }

    
    public static Object createSafeAttributionSource() {
        try {
            
            Class<?> attributionSourceClass = Class.forName("android.content.AttributionSource");
            
            
            Object attributionSource = null;
            
            try {
                
                java.lang.reflect.Constructor<?> constructor = attributionSourceClass.getDeclaredConstructor(int.class, String.class);
                constructor.setAccessible(true);
                attributionSource = constructor.newInstance(BlackBoxCore.getHostUid(), BlackBoxCore.getHostPkg());
            } catch (Exception e) {
                try {
                    
                    java.lang.reflect.Constructor<?> constructor = attributionSourceClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    attributionSource = constructor.newInstance();
                    
                    
                    fixAttributionSourceUid(attributionSource);
                } catch (Exception e2) {
                    Slog.w(TAG, "Could not create safe AttributionSource: " + e2.getMessage());
                    return null;
                }
            }
            
            return attributionSource;
        } catch (Exception e) {
            Slog.w(TAG, "Error creating safe AttributionSource: " + e.getMessage());
            return null;
        }
    }

    
    public static boolean validateAttributionSource(Object attributionSource) {
        try {
            if (attributionSource == null) return false;
            
            
            Class<?> attributionSourceClass = attributionSource.getClass();
            String[] uidFieldNames = {"mUid", "uid", "mCallingUid", "callingUid", "mSourceUid", "sourceUid"};
            
            for (String fieldName : uidFieldNames) {
                try {
                    java.lang.reflect.Field uidField = attributionSourceClass.getDeclaredField(fieldName);
                    uidField.setAccessible(true);
                    Object uidValue = uidField.get(attributionSource);
                    if (uidValue instanceof Integer) {
                        int uid = (Integer) uidValue;
                        if (uid > 0) {
                            Slog.d(TAG, "AttributionSource UID validation passed: " + uid);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    
                }
            }
            
            
            Slog.w(TAG, "AttributionSource validation failed, attempting to fix");
            fixAttributionSourceUid(attributionSource);
            return true;
            
        } catch (Exception e) {
            Slog.w(TAG, "Error validating AttributionSource: " + e.getMessage());
            return false;
        }
    }
}
