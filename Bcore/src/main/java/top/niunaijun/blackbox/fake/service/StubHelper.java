package top.niunaijun.blackbox.fake.service;

import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;


public class StubHelper {
    private static final String TAG = "StubHelper";

    
    public static Object getServiceInterface(String serviceName, String stubClassName, String realStubClassName) {
        try {
            
            Class<?> stubClass = Class.forName(stubClassName);
            Method getMethod = stubClass.getMethod("get");
            Object stub = getMethod.invoke(null);
            
            
            Method asInterfaceMethod = null;
            try {
                
                asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            } catch (NoSuchMethodException e1) {
                try {
                    
                    asInterfaceMethod = stubClass.getMethod("asInterface", Object.class);
                } catch (NoSuchMethodException e2) {
                    
                    Method[] methods = stubClass.getMethods();
                    for (Method method : methods) {
                        if (method.getName().equals("asInterface") && method.getParameterCount() == 1) {
                            asInterfaceMethod = method;
                            break;
                        }
                    }
                }
            }
            
            if (asInterfaceMethod != null) {
                IBinder binder = BRServiceManager.get().getService(serviceName);
                return asInterfaceMethod.invoke(stub, binder);
            } else {
                Log.w(TAG, "Could not find asInterface method in " + stubClassName);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to use generated stub class: " + stubClassName, e);
        }
        
        
        try {
            IBinder binder = BRServiceManager.get().getService(serviceName);
            Class<?> stubClass = Class.forName(realStubClassName);
            Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            return asInterfaceMethod.invoke(null, binder);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to get service interface for: " + serviceName, ex);
            return null;
        }
    }

    
    public static Object getServiceInterface(String serviceName, String stubClassName, String realStubClassName, IBinder binder) {
        try {
            
            Class<?> stubClass = Class.forName(stubClassName);
            Method getMethod = stubClass.getMethod("get");
            Object stub = getMethod.invoke(null);
            
            
            Method asInterfaceMethod = null;
            try {
                
                asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            } catch (NoSuchMethodException e1) {
                try {
                    
                    asInterfaceMethod = stubClass.getMethod("asInterface", Object.class);
                } catch (NoSuchMethodException e2) {
                    
                    Method[] methods = stubClass.getMethods();
                    for (Method method : methods) {
                        if (method.getName().equals("asInterface") && method.getParameterCount() == 1) {
                            asInterfaceMethod = method;
                            break;
                        }
                    }
                }
            }
            
            if (asInterfaceMethod != null) {
                IBinder serviceBinder = binder != null ? binder : BRServiceManager.get().getService(serviceName);
                return asInterfaceMethod.invoke(stub, serviceBinder);
            } else {
                Log.w(TAG, "Could not find asInterface method in " + stubClassName);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to use generated stub class: " + stubClassName, e);
        }
        
        
        try {
            IBinder serviceBinder = binder != null ? binder : BRServiceManager.get().getService(serviceName);
            Class<?> stubClass = Class.forName(realStubClassName);
            Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            return asInterfaceMethod.invoke(null, serviceBinder);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to get service interface for: " + serviceName, ex);
            return null;
        }
    }
}
