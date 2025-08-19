package top.niunaijun.blackbox.fake.service;

import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;

/**
 * Helper class to handle missing generated stub classes
 * Created to fix crashes caused by missing BR*Stub classes
 */
public class StubHelper {
    private static final String TAG = "StubHelper";

    /**
     * Get a service interface using the generated stub class or fallback to reflection
     * @param serviceName The name of the service
     * @param stubClassName The expected generated stub class name
     * @param realStubClassName The real Android stub class name
     * @return The service interface or null if all attempts fail
     */
    public static Object getServiceInterface(String serviceName, String stubClassName, String realStubClassName) {
        try {
            // Try to use the generated class first
            Class<?> stubClass = Class.forName(stubClassName);
            Method getMethod = stubClass.getMethod("get");
            Object stub = getMethod.invoke(null);
            
            // Try different method signatures for asInterface
            Method asInterfaceMethod = null;
            try {
                // Try with IBinder parameter
                asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            } catch (NoSuchMethodException e1) {
                try {
                    // Try with Object parameter (some generated classes use Object)
                    asInterfaceMethod = stubClass.getMethod("asInterface", Object.class);
                } catch (NoSuchMethodException e2) {
                    // Try with any parameter type
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
        
        // Fallback: use reflection to get the real service
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

    /**
     * Get a service interface using the generated stub class or fallback to reflection
     * @param serviceName The name of the service
     * @param stubClassName The expected generated stub class name
     * @param realStubClassName The real Android stub class name
     * @param binder The IBinder to use (if not null, serviceName is ignored)
     * @return The service interface or null if all attempts fail
     */
    public static Object getServiceInterface(String serviceName, String stubClassName, String realStubClassName, IBinder binder) {
        try {
            // Try to use the generated class first
            Class<?> stubClass = Class.forName(stubClassName);
            Method getMethod = stubClass.getMethod("get");
            Object stub = getMethod.invoke(null);
            
            // Try different method signatures for asInterface
            Method asInterfaceMethod = null;
            try {
                // Try with IBinder parameter
                asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            } catch (NoSuchMethodException e1) {
                try {
                    // Try with Object parameter (some generated classes use Object)
                    asInterfaceMethod = stubClass.getMethod("asInterface", Object.class);
                } catch (NoSuchMethodException e2) {
                    // Try with any parameter type
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
        
        // Fallback: use reflection to get the real service
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
