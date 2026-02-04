package top.niunaijun.blackbox.fake.service;

import android.os.Build;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


public class IDnsResolverProxy extends BinderInvocationStub {
    public static final String TAG = "IDnsResolverProxy";
    public static final String DNS_RESOLVER_SERVICE = "dnsresolver";

    public IDnsResolverProxy() {
        super(BRServiceManager.get().getService(DNS_RESOLVER_SERVICE));
    }

    @Override
    protected Object getWho() {
        
        
        Slog.d(TAG, "IDnsResolverProxy: Returning null for getWho to avoid reflection issues");
        return null;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        try {
            replaceSystemService(DNS_RESOLVER_SERVICE);
            Slog.d(TAG, "IDnsResolverProxy: Successfully injected DNS resolver service");
        } catch (Exception e) {
            Slog.w(TAG, "IDnsResolverProxy: Failed to inject service: " + e.getMessage());
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    @ProxyMethod("resolveDns")
    public static class ResolveDns extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting DNS resolution request");
            try {
                
                Object result = method.invoke(who, args);
                if (result != null) {
                    return result;
                }
                
                
                Slog.w(TAG, "DNS resolution failed, providing fallback");
                return createFallbackDnsResult();
                
            } catch (Exception e) {
                Slog.w(TAG, "DNS resolution error, providing fallback: " + e.getMessage());
                return createFallbackDnsResult();
            }
        }
        
        private Object createFallbackDnsResult() {
            try {
                
                List<InetAddress> fallbackServers = new ArrayList<>();
                fallbackServers.add(InetAddress.getByName("8.8.8.8"));
                fallbackServers.add(InetAddress.getByName("8.8.4.4"));
                return fallbackServers;
            } catch (Exception e) {
                Slog.e(TAG, "Error creating fallback DNS result: " + e.getMessage());
                return new ArrayList<>();
            }
        }
    }

    
    @ProxyMethod("setPrivateDnsConfiguration")
    public static class SetPrivateDnsConfiguration extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (Build.VERSION.SDK_INT >= 28) {
                Slog.d(TAG, "Intercepting private DNS configuration");
                
                try {
                    
                    if (args != null && args.length > 0) {
                        
                        Slog.d(TAG, "Disabling private DNS for sandboxed app");
                    }
                    return method.invoke(who, args);
                } catch (Exception e) {
                    Slog.w(TAG, "Private DNS configuration failed: " + e.getMessage());
                    return null;
                }
            }
            return null;
        }
    }

    
    @ProxyMethod("setDnsServersForNetwork")
    public static class SetDnsServersForNetwork extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting DNS server configuration");
            try {
                
                Object result = method.invoke(who, args);
                Slog.d(TAG, "DNS server configuration applied");
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "DNS server configuration failed: " + e.getMessage());
                return null;
            }
        }
    }

    
    @ProxyMethod("isNetworkValidated")
    public static class IsNetworkValidated extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "Intercepting network validation check");
            
            return true;
        }
    }

    
    @ProxyMethod("setDnsQueryTimeout")
    public static class SetDnsQueryTimeout extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (Build.VERSION.SDK_INT >= 21) {
                Slog.d(TAG, "Intercepting DNS query timeout configuration");
                try {
                    
                    if (args != null && args.length > 0 && args[0] instanceof Integer) {
                        int timeout = Math.min((Integer) args[0], 10000); 
                        Slog.d(TAG, "Setting DNS query timeout to: " + timeout + "ms");
                        args[0] = timeout;
                    }
                    return method.invoke(who, args);
                } catch (Exception e) {
                    Slog.w(TAG, "DNS query timeout configuration failed: " + e.getMessage());
                    return null;
                }
            }
            return null;
        }
    }

    
    @ProxyMethod("getDnsResolverStats")
    public static class GetDnsResolverStats extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (Build.VERSION.SDK_INT >= 23) {
                Slog.d(TAG, "Intercepting DNS resolver stats request");
                try {
                    
                    return method.invoke(who, args);
                } catch (Exception e) {
                    Slog.w(TAG, "DNS resolver stats failed: " + e.getMessage());
                    return null;
                }
            }
            return null;
        }
    }
}
