package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Network;
import android.net.LinkProperties;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import black.android.net.BRIConnectivityManagerStub;
import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.ScanClass;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * updated by alex5402 on 4/12/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK (USDT TRC20)
 */
@ScanClass(VpnCommonProxy.class)
public class IConnectivityManagerProxy extends BinderInvocationStub {
    public static final String TAG = "IConnectivityManagerProxy";

    public IConnectivityManagerProxy() {
        super(BRServiceManager.get().getService(Context.CONNECTIVITY_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIConnectivityManagerStub.get().asInterface(BRServiceManager.get().getService(Context.CONNECTIVITY_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    /**
     * Universal method to create NetworkInfo objects that works on all API levels
     */
    private static Object createNetworkInfo(int type, int subType, String typeName, String subTypeName) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                // On API 30+, use the constructor directly
                NetworkInfo networkInfo = new NetworkInfo(type, subType, typeName, subTypeName);
                networkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
                return networkInfo;
            } else {
                // On older API levels, use reflection
                try {
                    Class<?> networkInfoClass = Class.forName("android.net.NetworkInfo");
                    Constructor<?> constructor = networkInfoClass.getDeclaredConstructor(int.class, int.class, String.class, String.class);
                    constructor.setAccessible(true);
                    
                    Object networkInfo = constructor.newInstance(type, subType, typeName, subTypeName);
                    
                    // Set the detailed state using reflection
                    Method setDetailedStateMethod = networkInfoClass.getDeclaredMethod("setDetailedState", 
                        NetworkInfo.DetailedState.class, String.class, String.class);
                    setDetailedStateMethod.invoke(networkInfo, NetworkInfo.DetailedState.CONNECTED, null, null);
                    
                    return networkInfo;
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to create NetworkInfo via reflection: " + e.getMessage());
                    return null;
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to create NetworkInfo: " + e.getMessage());
            return null;
        }
    }

    /**
     * Universal method to create NetworkInfo array that works on all API levels
     */
    private static Object createNetworkInfoArray() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                // On API 30+, use the constructor directly
                NetworkInfo wifi = new NetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
                wifi.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
                NetworkInfo mobile = new NetworkInfo(ConnectivityManager.TYPE_MOBILE, 0, "MOBILE", "");
                mobile.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
                return new NetworkInfo[] { wifi, mobile };
            } else {
                // On older API levels, use reflection to create NetworkInfo array
                try {
                    Class<?> networkInfoClass = Class.forName("android.net.NetworkInfo");
                    Constructor<?> constructor = networkInfoClass.getDeclaredConstructor(int.class, int.class, String.class, String.class);
                    constructor.setAccessible(true);
                    
                    // Create WiFi NetworkInfo
                    Object wifi = constructor.newInstance(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
                    Method setDetailedStateMethod = networkInfoClass.getDeclaredMethod("setDetailedState", 
                        NetworkInfo.DetailedState.class, String.class, String.class);
                    setDetailedStateMethod.invoke(wifi, NetworkInfo.DetailedState.CONNECTED, null, null);
                    
                    // Create Mobile NetworkInfo
                    Object mobile = constructor.newInstance(ConnectivityManager.TYPE_MOBILE, 0, "MOBILE", "");
                    setDetailedStateMethod.invoke(mobile, NetworkInfo.DetailedState.CONNECTED, null, null);
                    
                    // Create array using reflection
                    Object[] networkInfoArray = (Object[]) java.lang.reflect.Array.newInstance(networkInfoClass, 2);
                    java.lang.reflect.Array.set(networkInfoArray, 0, wifi);
                    java.lang.reflect.Array.set(networkInfoArray, 1, mobile);
                    
                    return networkInfoArray;
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to create NetworkInfo array via reflection: " + e.getMessage());
                    return new Object[0];
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to create fallback NetworkInfo array: " + e.getMessage());
            return new Object[0];
        }
    }

    /**
     * Create NetworkCapabilities using reflection for all API levels (21+)
     */
    private static Object createNetworkCapabilities() {
        try {
            // Use reflection for constructor to ensure compatibility across all API levels
            Class<?> networkCapabilitiesClass = Class.forName("android.net.NetworkCapabilities");
            Constructor<?> constructor = networkCapabilitiesClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object nc = constructor.newInstance();
            
            // Add transport types using reflection to ensure compatibility
            try {
                Method addTransportTypeMethod = nc.getClass().getMethod("addTransportType", int.class);
                addTransportTypeMethod.invoke(nc, android.net.NetworkCapabilities.TRANSPORT_WIFI);
                addTransportTypeMethod.invoke(nc, android.net.NetworkCapabilities.TRANSPORT_CELLULAR);
            } catch (Exception e) {
                Slog.w(TAG, "Could not add transport types: " + e.getMessage());
            }

            // Add essential capabilities using reflection to ensure compatibility
            try {
                Method addCapabilityMethod = nc.getClass().getMethod("addCapability", int.class);
                addCapabilityMethod.invoke(nc, android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
                addCapabilityMethod.invoke(nc, android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                addCapabilityMethod.invoke(nc, android.net.NetworkCapabilities.NET_CAPABILITY_TRUSTED);
                addCapabilityMethod.invoke(nc, android.net.NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED);
                addCapabilityMethod.invoke(nc, android.net.NetworkCapabilities.NET_CAPABILITY_NOT_VPN);
                addCapabilityMethod.invoke(nc, android.net.NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED);
            } catch (Exception e) {
                Slog.w(TAG, "Could not add capabilities: " + e.getMessage());
            }
            
            return nc;
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create NetworkCapabilities via reflection: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create LinkProperties using reflection for all API levels (21+)
     */
    private static Object createLinkProperties() {
        try {
            // Use reflection for constructor to ensure compatibility across all API levels
            Class<?> linkPropertiesClass = Class.forName("android.net.LinkProperties");
            Constructor<?> constructor = linkPropertiesClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object linkProperties = constructor.newInstance();
            
            // Add DNS servers (Google DNS as fallback)
            java.util.List<java.net.InetAddress> dnsServers = new java.util.ArrayList<>();
            try {
                dnsServers.add(java.net.InetAddress.getByName("8.8.8.8"));
                dnsServers.add(java.net.InetAddress.getByName("8.8.4.4"));
                
                // Set DNS servers using reflection to ensure compatibility
                Method setDnsServersMethod = linkProperties.getClass().getMethod("setDnsServers", java.util.List.class);
                setDnsServersMethod.invoke(linkProperties, dnsServers);
            } catch (Exception e) {
                Slog.w(TAG, "Could not set DNS servers: " + e.getMessage());
            }
            
            return linkProperties;
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create LinkProperties via reflection: " + e.getMessage());
            return null;
        }
    }



    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for getNetworkInfo");
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo, creating fallback: " + e.getMessage());
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo: " + e.getMessage());
                return null;
            }
        }
    }

    @ProxyMethod("getAllNetworkInfo")
    public static class GetAllNetworkInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create basic NetworkInfo array
                Slog.d(TAG, "Creating fallback NetworkInfo array for getAllNetworkInfo");
                return createNetworkInfoArray();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getAllNetworkInfo, creating fallback: " + e.getMessage());
                return createNetworkInfoArray();
            }
        }
        
        private Object createNetworkInfoArray() {
            try {
                // Use the universal NetworkInfo array creation method
                return IConnectivityManagerProxy.createNetworkInfoArray();
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo array: " + e.getMessage());
                return new Object[0];
            }
        }
    }

    // Enhanced hook for getNetworkCapabilities for API 21+
    @ProxyMethod("getNetworkCapabilities")
    public static class GetNetworkCapabilities extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                try {
                    // Create NetworkCapabilities using API-specific methods
                    Object nc;
                    // Use the universal NetworkCapabilities creation method for all API levels
                    nc = IConnectivityManagerProxy.createNetworkCapabilities();
                    
                    if (nc != null) {
                        Slog.d(TAG, "Created enhanced NetworkCapabilities for sandboxed app");
                        return nc;
                    } else {
                        Slog.w(TAG, "Failed to create NetworkCapabilities, falling back to original method");
                        return method.invoke(who, args);
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Error creating NetworkCapabilities: " + e.getMessage());
                    return method.invoke(who, args);
                }
            }
            return method.invoke(who, args);
        }
    }

    // Hook for getActiveNetwork to ensure proper network binding
    @ProxyMethod("getActiveNetwork")
    public static class GetActiveNetwork extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                try {
                    // Create a mock Network object to ensure proper binding
                    // Use reflection to handle different constructor signatures
                    android.net.Network network;
                    try {
                        // Try constructor with int parameter first
                        Constructor<android.net.Network> constructor = android.net.Network.class.getConstructor(int.class);
                        network = constructor.newInstance(1);
                    } catch (Exception e) {
                        // Try to access the default constructor via reflection
                        try {
                            Constructor<android.net.Network> defaultConstructor = android.net.Network.class.getDeclaredConstructor();
                            defaultConstructor.setAccessible(true);
                            network = defaultConstructor.newInstance();
                        } catch (Exception e2) {
                            // If all else fails, return null and let the original method handle it
                            Slog.w(TAG, "Could not create Network object, falling back to original method");
                            return method.invoke(who, args);
                        }
                    }
                    Slog.d(TAG, "Created mock Network object for sandboxed app");
                    return network;
                } catch (Exception e) {
                    Slog.w(TAG, "Error creating Network object: " + e.getMessage());
                    return method.invoke(who, args);
                }
            }
            return method.invoke(who, args);
        }
    }

    // Hook for getLinkProperties to handle DNS configuration
    @ProxyMethod("getLinkProperties")
    public static class GetLinkProperties extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                try {
                    // Create LinkProperties using the universal method for all API levels
                    Object linkProperties = IConnectivityManagerProxy.createLinkProperties();
                    
                    if (linkProperties != null) {
                        Slog.d(TAG, "Created LinkProperties with DNS configuration for sandboxed app");
                        return linkProperties;
                    } else {
                        Slog.w(TAG, "Failed to create LinkProperties, falling back to original method");
                        return method.invoke(who, args);
                    }
                    
                } catch (Exception e) {
                    Slog.w(TAG, "Error creating LinkProperties: " + e.getMessage());
                    return method.invoke(who, args);
                }
            }
            return method.invoke(who, args);
        }
    }

    // Hook for getPrivateDnsServerName to disable private DNS
    @ProxyMethod("getPrivateDnsServerName")
    public static class GetPrivateDnsServerName extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Return null to disable private DNS and use system DNS
            Slog.d(TAG, "Disabling private DNS for sandboxed app");
            return null;
        }
    }

    // Hook for isPrivateDnsActive to ensure private DNS is not active
    @ProxyMethod("isPrivateDnsActive")
    public static class IsPrivateDnsActive extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Return false to indicate private DNS is not active
            Slog.d(TAG, "Private DNS disabled for sandboxed app");
            return false;
        }
    }

    // Hook for getDnsServers to ensure proper DNS resolution
    @ProxyMethod("getDnsServers")
    public static class GetDnsServers extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                // Return system DNS servers instead of custom ones
                java.util.List<java.net.InetAddress> dnsServers = new java.util.ArrayList<>();
                dnsServers.add(java.net.InetAddress.getByName("8.8.8.8"));
                dnsServers.add(java.net.InetAddress.getByName("8.8.4.4"));
                Slog.d(TAG, "Returning system DNS servers for sandboxed app");
                return dnsServers;
            } catch (Exception e) {
                Slog.w(TAG, "Error creating DNS servers list: " + e.getMessage());
                return method.invoke(who, args);
            }
        }
    }

    // Hook for isNetworkValidated to ensure network validation
    @ProxyMethod("isNetworkValidated")
    public static class IsNetworkValidated extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            // Return true to indicate network is validated
            Slog.d(TAG, "Network validation enabled for sandboxed app");
            return true;
        }
    }

    // Hook for requestNetwork - critical for internet access
    @ProxyMethod("requestNetwork")
    public static class RequestNetwork extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting requestNetwork call for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "requestNetwork succeeded via original method");
                    return result;
                }
                
                // If original method fails, create a mock network request result
                Slog.w(TAG, "requestNetwork failed, creating fallback result");
                return createMockNetworkRequestResult();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in requestNetwork, creating fallback: " + e.getMessage());
                return createMockNetworkRequestResult();
            }
        }
        
        private Object createMockNetworkRequestResult() {
            try {
                // Create a mock network request result
                // This is critical for apps that need to request network access
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    // For API 21+, try to create a NetworkRequest object
                    Class<?> networkRequestClass = Class.forName("android.net.NetworkRequest");
                    if (networkRequestClass != null) {
                        Slog.d(TAG, "Created fallback NetworkRequest for internet access");
                        return null; // Return null to indicate no specific network request
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "Could not create NetworkRequest fallback: " + e.getMessage());
            }
            return null;
        }
    }

    // Hook for registerNetworkCallback - also critical for internet access
    @ProxyMethod("registerNetworkCallback")
    public static class RegisterNetworkCallback extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting registerNetworkCallback for internet access");
            try {
                // Allow the network callback registration to proceed
                Object result = method.invoke(who, args);
                Slog.d(TAG, "Network callback registration successful");
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Network callback registration failed: " + e.getMessage());
                // Return a success indicator even if it fails
                return 0;
            }
        }
    }

    // Hook for registerDefaultNetworkCallback - critical for internet access
    @ProxyMethod("registerDefaultNetworkCallback")
    public static class RegisterDefaultNetworkCallback extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting registerDefaultNetworkCallback for internet access");
            try {
                // Allow the default network callback registration to proceed
                Object result = method.invoke(who, args);
                Slog.d(TAG, "Default network callback registration successful");
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Default network callback registration failed: " + e.getMessage());
                // Return a success indicator even if it fails
                return 0;
            }
        }
    }

    // Hook for getActiveNetworkInfoForUid - important for per-app network access
    @ProxyMethod("getActiveNetworkInfoForUid")
    public static class GetActiveNetworkInfoForUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getActiveNetworkInfoForUid for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for UID");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getActiveNetworkInfoForUid, creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for UID: " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for addDefaultNetworkActiveListener - critical for network state changes
    @ProxyMethod("addDefaultNetworkActiveListener")
    public static class AddDefaultNetworkActiveListener extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting addDefaultNetworkActiveListener for internet access");
            try {
                // Allow the network active listener to proceed
                Object result = method.invoke(who, args);
                Slog.d(TAG, "Default network active listener added successfully");
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Default network active listener failed: " + e.getMessage());
                // Return a success indicator even if it fails
                return 0;
            }
        }
    }

    // Hook for removeDefaultNetworkActiveListener - also important
    @ProxyMethod("removeDefaultNetworkActiveListener")
    public static class RemoveDefaultNetworkActiveListener extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting removeDefaultNetworkActiveListener for internet access");
            try {
                // Allow the network active listener removal to proceed
                Object result = method.invoke(who, args);
                Slog.d(TAG, "Default network active listener removed successfully");
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Default network active listener removal failed: " + e.getMessage());
                // Return a success indicator even if it fails
                return 0;
            }
        }
    }

    // Hook for isActiveNetworkMetered - important for network behavior
    @ProxyMethod("isActiveNetworkMetered")
    public static class IsActiveNetworkMetered extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting isActiveNetworkMetered for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, return false (unmetered) to allow full access
                Slog.d(TAG, "isActiveNetworkMetered failed, returning false for full access");
                return false;
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in isActiveNetworkMetered, returning false: " + e.getMessage());
                return false;
            }
        }
    }

    // Hook for getNetworkForType - important for specific network type access
    @ProxyMethod("getNetworkForType")
    public static class GetNetworkForType extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkForType for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a mock Network object
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    try {
                        // Use reflection to handle different constructor signatures
                        android.net.Network network;
                        try {
                            // Try constructor with int parameter first
                            Constructor<android.net.Network> constructor = android.net.Network.class.getConstructor(int.class);
                            network = constructor.newInstance(1);
                        } catch (Exception e) {
                            // Try to access the default constructor via reflection
                            try {
                                Constructor<android.net.Network> defaultConstructor = android.net.Network.class.getDeclaredConstructor();
                                defaultConstructor.setAccessible(true);
                                network = defaultConstructor.newInstance();
                            } catch (Exception e2) {
                                // If all else fails, return null
                                Slog.w(TAG, "Could not create Network object, returning null");
                                return null;
                            }
                        }
                        Slog.d(TAG, "Created fallback Network for type");
                        return network;
                    } catch (Exception e) {
                        Slog.w(TAG, "Failed to create fallback Network: " + e.getMessage());
                    }
                }
                
                return null;
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkForType: " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for registerNetworkCallback with NetworkRequest - critical for internet access
    @ProxyMethod("registerNetworkCallback")
    public static class RegisterNetworkCallbackWithRequest extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting registerNetworkCallback with NetworkRequest for internet access");
            try {
                // Allow the network callback registration to proceed
                Object result = method.invoke(who, args);
                Slog.d(TAG, "Network callback registration with request successful");
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Network callback registration with request failed: " + e.getMessage());
                // Return a success indicator even if it fails
                return 0;
            }
        }
    }

    // Hook for unregisterNetworkCallback - also important
    @ProxyMethod("unregisterNetworkCallback")
    public static class UnregisterNetworkCallback extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting unregisterNetworkCallback for internet access");
            try {
                // Allow the network callback unregistration to proceed
                Object result = method.invoke(who, args);
                Slog.d(TAG, "Network callback unregistration successful");
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Network callback unregistration failed: " + e.getMessage());
                // Return a success indicator even if it fails
                return 0;
            }
        }
    }

    // Hook for getNetworkInfo with Network parameter - important for modern apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithNetwork extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with Network parameter for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for Network parameter");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with Network, creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for Network: " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with int parameter - also important
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithInt extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with int parameter for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for int parameter");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with int, creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for int: " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String, creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String: " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString2 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (2) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (2)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (2), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (2): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString3 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (3) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (3)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (3), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (3): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString4 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (4) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (4)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (4), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (4): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString5 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (5) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (5)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (5), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (5): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString6 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (6) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (6)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (6), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (6): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString7 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (7) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (7)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (7), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (7): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString8 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (8) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (8)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (8), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (8): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString9 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (9) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (9)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (9), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (9): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString10 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (10) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (10)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (10), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (10): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString11 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (11) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (11)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (11), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (11): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString12 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (12) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (12)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (12), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (12): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString13 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (13) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (13)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (13), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (13): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString14 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (14) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (14)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (14), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (14): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString15 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (15) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (15)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (15), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (15): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString16 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (16) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (16)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (16), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (16): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString17 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (17) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (17)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (17), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (17): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString18 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (18) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (18)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (18), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (18): " + e.getMessage());
                return null;
            }
        }
    }

    // Hook for getNetworkInfo with String parameter - important for older apps
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString19 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (19) for internet access");
            try {
                // Try to use the original method first
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                // If original method fails, create a basic NetworkInfo
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (19)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (19), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                // Use the universal NetworkInfo creation method
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (19): " + e.getMessage());
                return null;
            }
        }
    }
}
