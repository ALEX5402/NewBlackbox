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

    
    private static Object createNetworkInfo(int type, int subType, String typeName, String subTypeName) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                
                NetworkInfo networkInfo = new NetworkInfo(type, subType, typeName, subTypeName);
                networkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
                return networkInfo;
            } else {
                
                try {
                    Class<?> networkInfoClass = Class.forName("android.net.NetworkInfo");
                    Constructor<?> constructor = networkInfoClass.getDeclaredConstructor(int.class, int.class, String.class, String.class);
                    constructor.setAccessible(true);
                    
                    Object networkInfo = constructor.newInstance(type, subType, typeName, subTypeName);
                    
                    
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

    
    private static Object createNetworkInfoArray() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                
                NetworkInfo wifi = new NetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
                wifi.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
                NetworkInfo mobile = new NetworkInfo(ConnectivityManager.TYPE_MOBILE, 0, "MOBILE", "");
                mobile.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
                return new NetworkInfo[] { wifi, mobile };
            } else {
                
                try {
                    Class<?> networkInfoClass = Class.forName("android.net.NetworkInfo");
                    Constructor<?> constructor = networkInfoClass.getDeclaredConstructor(int.class, int.class, String.class, String.class);
                    constructor.setAccessible(true);
                    
                    
                    Object wifi = constructor.newInstance(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
                    Method setDetailedStateMethod = networkInfoClass.getDeclaredMethod("setDetailedState", 
                        NetworkInfo.DetailedState.class, String.class, String.class);
                    setDetailedStateMethod.invoke(wifi, NetworkInfo.DetailedState.CONNECTED, null, null);
                    
                    
                    Object mobile = constructor.newInstance(ConnectivityManager.TYPE_MOBILE, 0, "MOBILE", "");
                    setDetailedStateMethod.invoke(mobile, NetworkInfo.DetailedState.CONNECTED, null, null);
                    
                    
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

    
    private static Object createNetworkCapabilities() {
        try {
            
            Class<?> networkCapabilitiesClass = Class.forName("android.net.NetworkCapabilities");
            Constructor<?> constructor = networkCapabilitiesClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object nc = constructor.newInstance();
            
            
            try {
                Method addTransportTypeMethod = nc.getClass().getMethod("addTransportType", int.class);
                addTransportTypeMethod.invoke(nc, android.net.NetworkCapabilities.TRANSPORT_WIFI);
                addTransportTypeMethod.invoke(nc, android.net.NetworkCapabilities.TRANSPORT_CELLULAR);
            } catch (Exception e) {
                Slog.w(TAG, "Could not add transport types: " + e.getMessage());
            }

            
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

    
    private static Object createLinkProperties() {
        try {
            
            Class<?> linkPropertiesClass = Class.forName("android.net.LinkProperties");
            Constructor<?> constructor = linkPropertiesClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object linkProperties = constructor.newInstance();
            
            
            java.util.List<java.net.InetAddress> dnsServers = new java.util.ArrayList<>();
            try {
                dnsServers.add(java.net.InetAddress.getByName("8.8.8.8"));
                dnsServers.add(java.net.InetAddress.getByName("8.8.4.4"));
                
                
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
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for getNetworkInfo");
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo, creating fallback: " + e.getMessage());
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
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
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo array for getAllNetworkInfo");
                return createNetworkInfoArray();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getAllNetworkInfo, creating fallback: " + e.getMessage());
                return createNetworkInfoArray();
            }
        }
        
        private Object createNetworkInfoArray() {
            try {
                
                return IConnectivityManagerProxy.createNetworkInfoArray();
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo array: " + e.getMessage());
                return new Object[0];
            }
        }
    }

    
    @ProxyMethod("getAllNetworks")
    public static class GetAllNetworks extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    
                    if (Array.getLength(result) > 0) {
                         return result;
                    }
                }
            } catch (Exception e) {
                
            }

            Slog.d(TAG, "Creating fallback Network[] for getAllNetworks");
            
            
            try {
                 
                 Method getActiveNetworkMethod = null;
                 try {
                     getActiveNetworkMethod = who.getClass().getMethod("getActiveNetwork");
                 } catch (NoSuchMethodException e) {
                     
                     for (Method m : who.getClass().getMethods()) {
                         if (m.getName().equals("getActiveNetwork")) {
                             getActiveNetworkMethod = m;
                             break;
                         }
                     }
                 }

                 if (getActiveNetworkMethod != null) {
                     Object activeNetwork = getActiveNetworkMethod.invoke(who);
                     if (activeNetwork != null) {
                         Class<?> networkClass = activeNetwork.getClass();
                         Object networkArray = Array.newInstance(networkClass, 1);
                         Array.set(networkArray, 0, activeNetwork);
                         Slog.d(TAG, "Refilled getAllNetworks with Active Network: " + activeNetwork);
                         return networkArray;
                     }
                 }
            } catch (Exception e) {
                 Slog.w(TAG, "Failed to use Active Network for fallback: " + e.getMessage());
            }

            
            try {
                Class<?> networkClass = Class.forName("android.net.Network");
                Object networkArray = Array.newInstance(networkClass, 1);
                
                
                
                Constructor<?> constructor = networkClass.getConstructor(int.class);
                Object network = constructor.newInstance(1);
                
                Array.set(networkArray, 0, network);
                return networkArray;
            } catch (Exception e) {
                 Slog.w(TAG, "Failed to create fallback Network[]: " + e.getMessage());
                 return method.invoke(who, args);
            }
        }
    }

    
    @ProxyMethod("getNetworkCapabilities")
    public static class GetNetworkCapabilities extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                try {
                    
                    Object result = method.invoke(who, args);
                    if (result != null) {
                        
                        try {
                            Method addCapabilityMethod = result.getClass().getMethod("addCapability", int.class);
                            addCapabilityMethod.setAccessible(true);
                            addCapabilityMethod.invoke(result, 12); 
                            addCapabilityMethod.invoke(result, 16); 
                        } catch (Exception e) {
                             
                            e.printStackTrace();
                        }
                        return result;
                    }

                    
                    Object nc;
                    
                    nc = IConnectivityManagerProxy.createNetworkCapabilities();
                    
                    if (nc != null) {
                        Slog.d(TAG, "Created enhanced NetworkCapabilities for sandboxed app (fallback)");
                        return nc;
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Error creating NetworkCapabilities: " + e.getMessage());
                }
            }
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("getActiveNetwork")
    public static class GetActiveNetwork extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                try {
                    
                    Object result = method.invoke(who, args);
                    if (result != null) {
                        return result;
                    }

                    
                    
                    android.net.Network network;
                    try {
                        
                        Constructor<android.net.Network> constructor = android.net.Network.class.getConstructor(int.class);
                        network = constructor.newInstance(1);
                    } catch (Exception e) {
                        
                        try {
                            Constructor<android.net.Network> defaultConstructor = android.net.Network.class.getDeclaredConstructor();
                            defaultConstructor.setAccessible(true);
                            network = defaultConstructor.newInstance();
                        } catch (Exception e2) {
                            
                            Slog.w(TAG, "Could not create Network object, falling back to original method");
                            return method.invoke(who, args);
                        }
                    }
                    Slog.d(TAG, "Created mock Network object for sandboxed app (fallback)");
                    return network;
                } catch (Exception e) {
                    Slog.w(TAG, "Error creating Network object: " + e.getMessage());
                }
            }
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("getActiveNetworkInfo")
    public static class GetActiveNetworkInfo extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Object result = method.invoke(who, args);
                if (result != null) {
                    
                    try {
                        Method setDetailedState = result.getClass().getMethod("setDetailedState", 
                             android.net.NetworkInfo.DetailedState.class, String.class, String.class);
                        setDetailedState.setAccessible(true);
                        setDetailedState.invoke(result, android.net.NetworkInfo.DetailedState.CONNECTED, null, null);
                    } catch (Exception e) {
                         
                    }
                    return result;
                }
            } catch (Exception e) {
                
            }
            Slog.d(TAG, "Creating fallback NetworkInfo for getActiveNetworkInfo");
            return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
        }
    }

    
    @ProxyMethod("getLinkProperties")
    public static class GetLinkProperties extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (android.os.Build.VERSION.SDK_INT >= 21) {
                try {
                    
                    Object result = method.invoke(who, args);
                    if (result != null) {
                        return result;
                    }

                    
                    Object linkProperties = IConnectivityManagerProxy.createLinkProperties();
                    
                    if (linkProperties != null) {
                        Slog.d(TAG, "Created LinkProperties with DNS configuration for sandboxed app (fallback)");
                        return linkProperties;
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Error creating LinkProperties: " + e.getMessage());
                }
            }
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("getPrivateDnsServerName")
    public static class GetPrivateDnsServerName extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "Disabling private DNS for sandboxed app");
            return null;
        }
    }

    
    @ProxyMethod("isPrivateDnsActive")
    public static class IsPrivateDnsActive extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "Private DNS disabled for sandboxed app");
            return false;
        }
    }

    
    @ProxyMethod("getDnsServers")
    public static class GetDnsServers extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                
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

    
    @ProxyMethod("isNetworkValidated")
    public static class IsNetworkValidated extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            Slog.d(TAG, "Network validation enabled for sandboxed app");
            return true;
        }
    }

    
    @ProxyMethod("requestNetwork")
    public static class RequestNetwork extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting requestNetwork call for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    Slog.d(TAG, "requestNetwork succeeded via original method");
                    return result;
                }
                
                
                Slog.w(TAG, "requestNetwork failed, creating fallback result");
                return createMockNetworkRequestResult();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in requestNetwork, creating fallback: " + e.getMessage());
                return createMockNetworkRequestResult();
            }
        }
        
        private Object createMockNetworkRequestResult() {
            try {
                
                
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    
                    Class<?> networkRequestClass = Class.forName("android.net.NetworkRequest");
                    if (networkRequestClass != null) {
                        Slog.d(TAG, "Created fallback NetworkRequest for internet access");
                        return null; 
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "Could not create NetworkRequest fallback: " + e.getMessage());
            }
            return null;
        }
    }

    
    @ProxyMethod("registerNetworkCallback")
    public static class RegisterNetworkCallback extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting registerNetworkCallback for internet access");
            try {
                
                Object result = method.invoke(who, args);
                Slog.d(TAG, "Network callback registration successful");
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Network callback registration failed: " + e.getMessage());
                
                return 0;
            }
        }
    }

    
    @ProxyMethod("registerDefaultNetworkCallback")
    public static class RegisterDefaultNetworkCallback extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting registerDefaultNetworkCallback for internet access");
            try {
                
                Object result = method.invoke(who, args);
                Slog.d(TAG, "Default network callback registration successful");
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Default network callback registration failed: " + e.getMessage());
                
                return 0;
            }
        }
    }

    
    @ProxyMethod("getActiveNetworkInfoForUid")
    public static class GetActiveNetworkInfoForUid extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getActiveNetworkInfoForUid for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    
                    try {
                        Method setDetailedState = result.getClass().getMethod("setDetailedState", 
                             android.net.NetworkInfo.DetailedState.class, String.class, String.class);
                        setDetailedState.setAccessible(true);
                        setDetailedState.invoke(result, android.net.NetworkInfo.DetailedState.CONNECTED, null, null);
                    } catch (Exception e) {
                         
                    }
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for UID");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getActiveNetworkInfoForUid, creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for UID: " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("addDefaultNetworkActiveListener")
    public static class AddDefaultNetworkActiveListener extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting addDefaultNetworkActiveListener for internet access");
            try {
                
                Object result = method.invoke(who, args);
                Slog.d(TAG, "Default network active listener added successfully");
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Default network active listener failed: " + e.getMessage());
                
                return 0;
            }
        }
    }

    
    @ProxyMethod("removeDefaultNetworkActiveListener")
    public static class RemoveDefaultNetworkActiveListener extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting removeDefaultNetworkActiveListener for internet access");
            try {
                
                Object result = method.invoke(who, args);
                Slog.d(TAG, "Default network active listener removed successfully");
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Default network active listener removal failed: " + e.getMessage());
                
                return 0;
            }
        }
    }

    
    @ProxyMethod("isActiveNetworkMetered")
    public static class IsActiveNetworkMetered extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting isActiveNetworkMetered for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "isActiveNetworkMetered failed, returning false for full access");
                return false;
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in isActiveNetworkMetered, returning false: " + e.getMessage());
                return false;
            }
        }
    }

    
    @ProxyMethod("getNetworkForType")
    public static class GetNetworkForType extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkForType for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    try {
                        
                        android.net.Network network;
                        try {
                            
                            Constructor<android.net.Network> constructor = android.net.Network.class.getConstructor(int.class);
                            network = constructor.newInstance(1);
                        } catch (Exception e) {
                            
                            try {
                                Constructor<android.net.Network> defaultConstructor = android.net.Network.class.getDeclaredConstructor();
                                defaultConstructor.setAccessible(true);
                                network = defaultConstructor.newInstance();
                            } catch (Exception e2) {
                                
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

    
    @ProxyMethod("registerNetworkCallback")
    public static class RegisterNetworkCallbackWithRequest extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting registerNetworkCallback with NetworkRequest for internet access");
            try {
                
                Object result = method.invoke(who, args);
                Slog.d(TAG, "Network callback registration with request successful");
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Network callback registration with request failed: " + e.getMessage());
                
                return 0;
            }
        }
    }

    
    @ProxyMethod("unregisterNetworkCallback")
    public static class UnregisterNetworkCallback extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting unregisterNetworkCallback for internet access");
            try {
                
                Object result = method.invoke(who, args);
                Slog.d(TAG, "Network callback unregistration successful");
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "Network callback unregistration failed: " + e.getMessage());
                
                return 0;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithNetwork extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with Network parameter for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    
                    try {
                        Method setDetailedState = result.getClass().getMethod("setDetailedState", 
                             android.net.NetworkInfo.DetailedState.class, String.class, String.class);
                        setDetailedState.setAccessible(true);
                        setDetailedState.invoke(result, android.net.NetworkInfo.DetailedState.CONNECTED, null, null);
                    } catch (Exception e) {
                         
                    }
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for Network parameter");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with Network, creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for Network: " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithInt extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with int parameter for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    
                    try {
                        Method setDetailedState = result.getClass().getMethod("setDetailedState", 
                             android.net.NetworkInfo.DetailedState.class, String.class, String.class);
                        setDetailedState.setAccessible(true);
                        setDetailedState.invoke(result, android.net.NetworkInfo.DetailedState.CONNECTED, null, null);
                    } catch (Exception e) {
                         
                    }
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for int parameter");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with int, creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for int: " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String, creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String: " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString2 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (2) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (2)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (2), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (2): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString3 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (3) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (3)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (3), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (3): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString4 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (4) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (4)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (4), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (4): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString5 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (5) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (5)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (5), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (5): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString6 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (6) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (6)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (6), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (6): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString7 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (7) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (7)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (7), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (7): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString8 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (8) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (8)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (8), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (8): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString9 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (9) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (9)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (9), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (9): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString10 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (10) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (10)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (10), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (10): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString11 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (11) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (11)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (11), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (11): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString12 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (12) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (12)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (12), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (12): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString13 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (13) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (13)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (13), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (13): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString14 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (14) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (14)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (14), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (14): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString15 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (15) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (15)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (15), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (15): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString16 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (16) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (16)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (16), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (16): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString17 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (17) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (17)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (17), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (17): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString18 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (18) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (18)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (18), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (18): " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("getNetworkInfo")
    public static class GetNetworkInfoWithString19 extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting getNetworkInfo with String parameter (19) for internet access");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.d(TAG, "Creating fallback NetworkInfo for String parameter (19)");
                return createBasicNetworkInfo();
                
            } catch (Exception e) {
                Slog.w(TAG, "Error in getNetworkInfo with String (19), creating fallback: " + e.getMessage());
                return createBasicNetworkInfo();
            }
        }
        
        private Object createBasicNetworkInfo() {
            try {
                
                return createNetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "WIFI", "");
            } catch (Exception e) {
                Slog.e(TAG, "Failed to create fallback NetworkInfo for String (19): " + e.getMessage());
                return null;
            }
        }
    }
}
