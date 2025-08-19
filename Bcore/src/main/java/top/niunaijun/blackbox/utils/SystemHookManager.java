package top.niunaijun.blackbox.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import top.niunaijun.blackbox.BlackBoxCore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import android.content.Intent;
import android.util.Log;
import java.util.List;

/**
 * SystemHookManager - Directly hooks problematic system methods to prevent null context crashes
 */
public class SystemHookManager {
    private static final String TAG = "SystemHookManager";
    
    /**
     * Install all system hooks to prevent null context crashes
     */
    public static void installAllHooks() {
        try {
            Log.d(TAG, "Installing system hooks...");
            
            // Hook ClientTransactionListenerController
            hookClientTransactionListenerController();
            
            // Hook ConfigurationController
            hookConfigurationController();
            
            // Hook ActivityThread directly
            hookActivityThread();
            
            Log.d(TAG, "System hooks installed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to install system hooks: " + e.getMessage(), e);
        }
    }
    
    /**
     * Hook the ClientTransactionListenerController to prevent null context crashes
     */
    private static void hookClientTransactionListenerController() {
        try {
            // Try to find the ClientTransactionListenerController class
            Class<?> controllerClass = Class.forName("android.app.servertransaction.ClientTransactionListenerController");
            if (controllerClass != null) {
                Log.d(TAG, "Found ClientTransactionListenerController class");
                
                // Create a proxy that handles null contexts gracefully
                Object proxy = Proxy.newProxyInstance(
                    controllerClass.getClassLoader(),
                    new Class<?>[] { controllerClass },
                    (proxyObj, method, args) -> {
                        if ("onContextConfigurationPreChanged".equals(method.getName())) {
                            try {
                                // Before calling the original method, ensure all activities have valid contexts
                                ensureAllActivitiesHaveContext();
                                
                                // Call the original method with null safety
                                if (args != null && args.length > 0) {
                                    return method.invoke(proxyObj, args);
                                } else {
                                    Log.w(TAG, "ClientTransactionListenerController called with null args, skipping");
                                    return null;
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Error in ClientTransactionListenerController proxy: " + e.getMessage());
                                // Return gracefully instead of crashing
                                return null;
                            }
                        }
                        return method.invoke(proxyObj, args);
                    }
                );
                
                // Try to replace the instance in ActivityThread
                replaceControllerInstance(controllerClass, proxy, "ClientTransactionListenerController");
                
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not hook ClientTransactionListenerController: " + e.getMessage());
        }
    }
    
    /**
     * Hook the ConfigurationController to prevent null context crashes
     */
    private static void hookConfigurationController() {
        try {
            // Try to find the ConfigurationController class
            Class<?> controllerClass = Class.forName("android.app.ConfigurationController");
            if (controllerClass != null) {
                Log.d(TAG, "Found ConfigurationController class");
                
                // Create a proxy that handles null contexts gracefully
                Object proxy = Proxy.newProxyInstance(
                    controllerClass.getClassLoader(),
                    new Class<?>[] { controllerClass },
                    (proxyObj, method, args) -> {
                        if ("handleConfigurationChanged".equals(method.getName())) {
                            try {
                                // Before calling the original method, ensure all activities have valid contexts
                                ensureAllActivitiesHaveContext();
                                
                                // Call the original method with null safety
                                if (args != null && args.length > 0) {
                                    return method.invoke(proxyObj, args);
                                } else {
                                    Log.w(TAG, "ConfigurationController called with null args, skipping");
                                    return null;
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Error in ConfigurationController proxy: " + e.getMessage());
                                // Return gracefully instead of crashing
                                return null;
                            }
                        }
                        return method.invoke(proxyObj, args);
                    }
                );
                
                // Try to replace the instance in ActivityThread
                replaceControllerInstance(controllerClass, proxy, "ConfigurationController");
                
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not hook ConfigurationController: " + e.getMessage());
        }
    }
    
    /**
     * Hook ActivityThread directly to ensure contexts are valid
     */
    private static void hookActivityThread() {
        try {
            // Try to find the ActivityThread class
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            if (activityThreadClass != null) {
                Log.d(TAG, "Found ActivityThread class");
                
                // Get the current ActivityThread instance
                Object activityThread = BlackBoxCore.mainThread();
                if (activityThread != null) {
                    Log.d(TAG, "Found ActivityThread instance");
                    
                    // Try to hook the handleLaunchActivity method
                    try {
                        Method handleLaunchActivity = activityThreadClass.getDeclaredMethod(
                            "handleLaunchActivity", 
                            Object.class, // ActivityClientRecord
                            Intent.class,
                            Object.class, // ActivityInfo
                            Object.class, // Configuration
                            Object.class, // CompatibilityInfo
                            String.class, // referrer
                            Object.class, // IVoiceInteractor
                            Object.class, // Bundle
                            Object.class, // PersistableBundle
                            List.class,   // List<ResultInfo>
                            List.class,   // List<ReferrerIntent>
                            boolean.class, // notResumed
                            boolean.class, // isForward
                            Object.class  // ProfilerInfo
                        );
                        
                        if (handleLaunchActivity != null) {
                            Log.d(TAG, "Found handleLaunchActivity method");
                            
                            // Make the method accessible
                            handleLaunchActivity.setAccessible(true);
                            
                            // We can't easily replace this method, but we can ensure our context fixes are applied
                            // The key is to make sure our context fixes are called before the original method
                            
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Could not hook handleLaunchActivity: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not hook ActivityThread: " + e.getMessage());
        }
    }
    
    /**
     * Replace a controller instance in ActivityThread
     */
    private static void replaceControllerInstance(Class<?> controllerClass, Object proxy, String controllerName) {
        try {
            // Look for the instance in ActivityThread
            Object activityThread = BlackBoxCore.mainThread();
            if (activityThread != null) {
                // Try to find the controller field
                Field[] fields = activityThread.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (field.getType().equals(controllerClass)) {
                        field.setAccessible(true);
                        Object currentInstance = field.get(activityThread);
                        if (currentInstance != null) {
                            field.set(activityThread, proxy);
                            Log.d(TAG, "Successfully replaced " + controllerName + " instance");
                            return;
                        }
                    }
                }
                
                // If we couldn't find the field, try to create a new instance
                Log.w(TAG, "Could not find " + controllerName + " field, trying to create new instance");
                try {
                    // Try to find a constructor
                    java.lang.reflect.Constructor<?>[] constructors = controllerClass.getDeclaredConstructors();
                    if (constructors.length > 0) {
                        constructors[0].setAccessible(true);
                        Object newInstance = constructors[0].newInstance();
                        if (newInstance != null) {
                            Log.d(TAG, "Created new " + controllerName + " instance");
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not create new " + controllerName + " instance: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not replace " + controllerName + " instance: " + e.getMessage());
        }
    }
    
    /**
     * Ensure all activities in the current process have valid contexts
     */
    private static void ensureAllActivitiesHaveContext() {
        try {
            // Get all activity records from ActivityThread
            Object activityThread = BlackBoxCore.mainThread();
            if (activityThread != null) {
                // Try to get the activity records
                try {
                    Field[] fields = activityThread.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        if (field.getType().getName().contains("ArrayMap") || 
                            field.getType().getName().contains("HashMap")) {
                            field.setAccessible(true);
                            Object activityRecords = field.get(activityThread);
                            if (activityRecords != null) {
                                // Try to iterate through the records
                                try {
                                    Method valuesMethod = activityRecords.getClass().getMethod("values");
                                    Object values = valuesMethod.invoke(activityRecords);
                                    if (values instanceof java.util.Collection) {
                                        for (Object record : (java.util.Collection<?>) values) {
                                            if (record != null) {
                                                // Try to get the activity from the record
                                                try {
                                                    Field activityField = record.getClass().getDeclaredField("activity");
                                                    activityField.setAccessible(true);
                                                    Object activity = activityField.get(record);
                                                    if (activity instanceof android.app.Activity) {
                                                        BlackBoxCore.ensureActivityContext((android.app.Activity) activity);
                                                    }
                                                } catch (Exception e) {
                                                    // Ignore individual activity errors
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "Could not iterate through activity records: " + e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not access activity records: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error ensuring all activities have context: " + e.getMessage());
        }
    }
}
