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


public class SystemHookManager {
    private static final String TAG = "SystemHookManager";
    
    
    public static void installAllHooks() {
        try {
            Log.d(TAG, "Installing system hooks...");
            
            
            hookClientTransactionListenerController();
            
            
            hookConfigurationController();
            
            
            hookActivityThread();
            
            Log.d(TAG, "System hooks installed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to install system hooks: " + e.getMessage(), e);
        }
    }
    
    
    private static void hookClientTransactionListenerController() {
        try {
            
            Class<?> controllerClass = Class.forName("android.app.servertransaction.ClientTransactionListenerController");
            if (controllerClass != null) {
                Log.d(TAG, "Found ClientTransactionListenerController class");
                
                
                Object proxy = Proxy.newProxyInstance(
                    controllerClass.getClassLoader(),
                    new Class<?>[] { controllerClass },
                    (proxyObj, method, args) -> {
                        if ("onContextConfigurationPreChanged".equals(method.getName())) {
                            try {
                                
                                ensureAllActivitiesHaveContext();
                                
                                
                                if (args != null && args.length > 0) {
                                    return method.invoke(proxyObj, args);
                                } else {
                                    Log.w(TAG, "ClientTransactionListenerController called with null args, skipping");
                                    return null;
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Error in ClientTransactionListenerController proxy: " + e.getMessage());
                                
                                return null;
                            }
                        }
                        return method.invoke(proxyObj, args);
                    }
                );
                
                
                replaceControllerInstance(controllerClass, proxy, "ClientTransactionListenerController");
                
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not hook ClientTransactionListenerController: " + e.getMessage());
        }
    }
    
    
    private static void hookConfigurationController() {
        try {
            
            Class<?> controllerClass = Class.forName("android.app.ConfigurationController");
            if (controllerClass != null) {
                Log.d(TAG, "Found ConfigurationController class");
                
                
                Object proxy = Proxy.newProxyInstance(
                    controllerClass.getClassLoader(),
                    new Class<?>[] { controllerClass },
                    (proxyObj, method, args) -> {
                        if ("handleConfigurationChanged".equals(method.getName())) {
                            try {
                                
                                ensureAllActivitiesHaveContext();
                                
                                
                                if (args != null && args.length > 0) {
                                    return method.invoke(proxyObj, args);
                                } else {
                                    Log.w(TAG, "ConfigurationController called with null args, skipping");
                                    return null;
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "Error in ConfigurationController proxy: " + e.getMessage());
                                
                                return null;
                            }
                        }
                        return method.invoke(proxyObj, args);
                    }
                );
                
                
                replaceControllerInstance(controllerClass, proxy, "ConfigurationController");
                
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not hook ConfigurationController: " + e.getMessage());
        }
    }
    
    
    private static void hookActivityThread() {
        try {
            
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            if (activityThreadClass != null) {
                Log.d(TAG, "Found ActivityThread class");
                
                
                Object activityThread = BlackBoxCore.mainThread();
                if (activityThread != null) {
                    Log.d(TAG, "Found ActivityThread instance");
                    
                    
                    try {
                        Method handleLaunchActivity = activityThreadClass.getDeclaredMethod(
                            "handleLaunchActivity", 
                            Object.class, 
                            Intent.class,
                            Object.class, 
                            Object.class, 
                            Object.class, 
                            String.class, 
                            Object.class, 
                            Object.class, 
                            Object.class, 
                            List.class,   
                            List.class,   
                            boolean.class, 
                            boolean.class, 
                            Object.class  
                        );
                        
                        if (handleLaunchActivity != null) {
                            Log.d(TAG, "Found handleLaunchActivity method");
                            
                            
                            handleLaunchActivity.setAccessible(true);
                            
                            
                            
                            
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
    
    
    private static void replaceControllerInstance(Class<?> controllerClass, Object proxy, String controllerName) {
        try {
            
            Object activityThread = BlackBoxCore.mainThread();
            if (activityThread != null) {
                
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
                
                
                Log.w(TAG, "Could not find " + controllerName + " field, trying to create new instance");
                try {
                    
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
    
    
    private static void ensureAllActivitiesHaveContext() {
        try {
            
            Object activityThread = BlackBoxCore.mainThread();
            if (activityThread != null) {
                
                try {
                    Field[] fields = activityThread.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        if (field.getType().getName().contains("ArrayMap") || 
                            field.getType().getName().contains("HashMap")) {
                            field.setAccessible(true);
                            Object activityRecords = field.get(activityThread);
                            if (activityRecords != null) {
                                
                                try {
                                    Method valuesMethod = activityRecords.getClass().getMethod("values");
                                    Object values = valuesMethod.invoke(activityRecords);
                                    if (values instanceof java.util.Collection) {
                                        for (Object record : (java.util.Collection<?>) values) {
                                            if (record != null) {
                                                
                                                try {
                                                    Field activityField = record.getClass().getDeclaredField("activity");
                                                    activityField.setAccessible(true);
                                                    Object activity = activityField.get(record);
                                                    if (activity instanceof android.app.Activity) {
                                                        BlackBoxCore.ensureActivityContext((android.app.Activity) activity);
                                                    }
                                                } catch (Exception e) {
                                                    
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
