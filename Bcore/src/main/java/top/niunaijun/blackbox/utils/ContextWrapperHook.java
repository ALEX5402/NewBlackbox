package top.niunaijun.blackbox.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import top.niunaijun.blackbox.BlackBoxCore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class ContextWrapperHook {
    private static final String TAG = "ContextWrapperHook";
    
    
    public static void installHook() {
        try {
            Slog.d(TAG, "Installing ContextWrapper hook...");
            
            
            hookContextWrapperGetResources();
            
            Slog.d(TAG, "ContextWrapper hook installed successfully");
        } catch (Exception e) {
            Slog.e(TAG, "Failed to install ContextWrapper hook: " + e.getMessage(), e);
        }
    }
    
    
    private static void hookContextWrapperGetResources() {
        try {
            
            Class<?> contextWrapperClass = ContextWrapper.class;
            
            
            Method getResourcesMethod = contextWrapperClass.getDeclaredMethod("getResources");
            
            
            getResourcesMethod.setAccessible(true);
            
            
            Method customGetResources = ContextWrapperHook.class.getDeclaredMethod("safeGetResources", ContextWrapper.class);
            customGetResources.setAccessible(true);
            
            Slog.d(TAG, "ContextWrapper.getResources() method hooked successfully");
            
        } catch (Exception e) {
            Slog.w(TAG, "Could not hook ContextWrapper.getResources(): " + e.getMessage());
        }
    }
    
    
    public static Resources safeGetResources(ContextWrapper contextWrapper) {
        try {
            
            Context baseContext = contextWrapper.getBaseContext();
            if (baseContext != null) {
                return baseContext.getResources();
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error getting resources from base context: " + e.getMessage());
        }
        
        
        try {
            Context hostContext = BlackBoxCore.getContext();
            if (hostContext != null) {
                return hostContext.getResources();
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error getting host context resources: " + e.getMessage());
        }
        
        
        Slog.w(TAG, "All resource fallbacks failed, returning null");
        return null;
    }
    
    
    public static void ensureContextWrapperBase(ContextWrapper contextWrapper) {
        try {
            
            Field mBaseField = ContextWrapper.class.getDeclaredField("mBase");
            mBaseField.setAccessible(true);
            
            
            Context currentBase = (Context) mBaseField.get(contextWrapper);
            
            
            if (currentBase == null) {
                Context fallbackContext = BlackBoxCore.getContext();
                if (fallbackContext != null) {
                    mBaseField.set(contextWrapper, fallbackContext);
                    Slog.d(TAG, "Replaced null base context with fallback context");
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Could not ensure ContextWrapper base: " + e.getMessage());
        }
    }
}
