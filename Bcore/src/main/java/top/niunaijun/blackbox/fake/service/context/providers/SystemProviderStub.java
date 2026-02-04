package top.niunaijun.blackbox.fake.service.context.providers;

import android.os.IInterface;

import java.lang.reflect.Method;

import black.android.content.BRAttributionSource;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.utils.compat.ContextCompat;


public class SystemProviderStub extends ClassInvocationStub implements BContentProvider {
    private IInterface mBase;

    @Override
    public IInterface wrapper(IInterface contentProviderProxy, String appPkg) {
        mBase = contentProviderProxy;
        injectHook();
        return (IInterface) getProxyInvocation();
    }

    @Override
    protected Object getWho() {
        return mBase;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {

    }

    @Override
    protected void onBindMethod() {

    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("asBinder".equals(method.getName())) {
            return method.invoke(mBase, args);
        }
        
        String methodName = method.getName();
        
        
        
        if ("call".equals(methodName)) {
            
            if (args != null) {
                Class<?> attributionSourceClass = BRAttributionSource.getRealClass();
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    
                    if (arg != null && attributionSourceClass != null && 
                            arg.getClass().getName().equals(attributionSourceClass.getName())) {
                        ContextCompat.fixAttributionSourceState(arg, BlackBoxCore.getHostUid());
                    }
                }
            }
            return method.invoke(mBase, args);
        }
        
        
        if (args != null && args.length > 0) {
            Object arg = args[0];
            if (arg instanceof String) {
                String authority = (String) arg;
                
                if (!isSystemProviderAuthority(authority)) {
                    args[0] = BlackBoxCore.getHostPkg();
                }
            } else if (arg != null) {
                Class<?> attrSourceClass = BRAttributionSource.getRealClass();
                
                if (attrSourceClass != null && arg.getClass().getName().equals(attrSourceClass.getName())) {
                    ContextCompat.fixAttributionSourceState(arg, BlackBoxCore.getHostUid());
                }
            }
        }
        return method.invoke(mBase, args);
    }

    private boolean isSystemProviderAuthority(String authority) {
        if (authority == null) return false;
        
        return authority.equals("settings") || 
               authority.equals("media") || 
               authority.equals("downloads") || 
               authority.equals("contacts") || 
               authority.equals("call_log") || 
               authority.equals("telephony") || 
               authority.equals("calendar") || 
               authority.equals("browser") || 
               authority.equals("user_dictionary") || 
               authority.equals("applications") ||
               authority.startsWith("com.android.") ||
               authority.startsWith("android.");
    }
}
