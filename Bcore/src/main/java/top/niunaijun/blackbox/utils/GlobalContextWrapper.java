package top.niunaijun.blackbox.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import top.niunaijun.blackbox.BlackBoxCore;


public class GlobalContextWrapper extends ContextWrapper {
    private static final String TAG = "GlobalContextWrapper";
    
    private final Context fallbackContext;
    private final String packageName;
    
    public GlobalContextWrapper(Context base, String packageName) {
        super(base != null ? base : BlackBoxCore.getContext());
        this.fallbackContext = BlackBoxCore.getContext();
        this.packageName = packageName;
    }
    
    public GlobalContextWrapper(Context base) {
        this(base, base != null ? base.getPackageName() : "unknown");
    }
    
    @Override
    public Context getBaseContext() {
        Context base = super.getBaseContext();
        return base != null ? base : fallbackContext;
    }
    
    @Override
    public String getPackageName() {
        return packageName != null ? packageName : 
               (getBaseContext() != null ? getBaseContext().getPackageName() : "unknown");
    }
    
    @Override
    public Resources getResources() {
        try {
            Context base = getBaseContext();
            if (base != null) {
                return base.getResources();
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error getting resources from base context: " + e.getMessage());
        }
        
        
        try {
            return fallbackContext.getResources();
        } catch (Exception e) {
            Slog.w(TAG, "Error getting fallback resources: " + e.getMessage());
            
            try {
                return new Resources(null, null, null);
            } catch (Exception e2) {
                Slog.w(TAG, "Error creating minimal resources: " + e2.getMessage());
                
                return null;
            }
        }
    }
    
    @Override
    public PackageManager getPackageManager() {
        try {
            Context base = getBaseContext();
            if (base != null) {
                return base.getPackageManager();
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error getting package manager from base context: " + e.getMessage());
        }
        
        
        try {
            return fallbackContext.getPackageManager();
        } catch (Exception e) {
            Slog.w(TAG, "Error getting fallback package manager: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    public Context getApplicationContext() {
        try {
            Context base = getBaseContext();
            if (base != null) {
                return base.getApplicationContext();
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error getting application context from base context: " + e.getMessage());
        }
        
        return fallbackContext.getApplicationContext();
    }
    
    @Override
    public ClassLoader getClassLoader() {
        try {
            Context base = getBaseContext();
            if (base != null) {
                return base.getClassLoader();
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error getting class loader from base context: " + e.getMessage());
        }
        
        return fallbackContext.getClassLoader();
    }
    
    @Override
    public android.content.ContentResolver getContentResolver() {
        try {
            Context base = getBaseContext();
            if (base != null) {
                return base.getContentResolver();
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error getting content resolver from base context: " + e.getMessage());
        }
        
        return fallbackContext.getContentResolver();
    }
    
    @Override
    public AssetManager getAssets() {
        try {
            Context base = getBaseContext();
            if (base != null) {
                return base.getAssets();
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error getting assets from base context: " + e.getMessage());
        }
        
        return fallbackContext.getAssets();
    }
    
    @Override
    public Object getSystemService(String name) {
        try {
            Context base = getBaseContext();
            if (base != null) {
                return base.getSystemService(name);
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error getting system service from base context: " + e.getMessage());
        }
        
        return fallbackContext.getSystemService(name);
    }
    
    
    public static Context createSafeContext(Context context, String packageName) {
        if (context == null) {
            return new GlobalContextWrapper(null, packageName);
        }
        
        if (context instanceof GlobalContextWrapper) {
            return context;
        }
        
        if (context instanceof ContextWrapper) {
            
            try {
                Context baseContext = ((ContextWrapper) context).getBaseContext();
                if (baseContext == null) {
                    return new GlobalContextWrapper(context, packageName);
                }
            } catch (Exception e) {
                Slog.w(TAG, "Error checking base context: " + e.getMessage());
                return new GlobalContextWrapper(context, packageName);
            }
        }
        
        return new GlobalContextWrapper(context, packageName);
    }
    
    
    public static Context createSafeContext(Context context) {
        String packageName = context != null ? context.getPackageName() : "unknown";
        return createSafeContext(context, packageName);
    }
}
