package top.niunaijun.blackbox.fake.service;

import android.net.Uri;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;


public class ContentResolverProxy extends ClassInvocationStub {
    public static final String TAG = "ContentResolverProxy";

    public ContentResolverProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        
        return BlackBoxCore.getContext().getContentResolver();
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    @ProxyMethod("query")
    public static class Query extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0 && args[0] instanceof Uri) {
                Uri uri = (Uri) args[0];
                String uriString = uri.toString();
                
                
                if (uriString.contains("audio") || uriString.contains("media") || 
                    uriString.contains("content://media/external/audio") ||
                    uriString.contains("content://media/internal/audio") ||
                    uriString.contains("content://media/external/file") ||
                    uriString.contains("content://media/internal/file")) {
                    
                    Slog.d(TAG, "ContentResolver: Allowing audio query: " + uriString);
                    
                    
                    return method.invoke(who, args);
                }
            }
            
            
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("query")
    public static class QueryWithProjection extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 1 && args[0] instanceof Uri) {
                Uri uri = (Uri) args[0];
                String uriString = uri.toString();
                
                
                if (uriString.contains("audio") || uriString.contains("media") || 
                    uriString.contains("content://media/external/audio") ||
                    uriString.contains("content://media/internal/audio") ||
                    uriString.contains("content://media/external/file") ||
                    uriString.contains("content://media/internal/file")) {
                    
                    Slog.d(TAG, "ContentResolver: Allowing audio query with projection: " + uriString);
                    
                    
                    return method.invoke(who, args);
                }
            }
            
            
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("insert")
    public static class Insert extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0 && args[0] instanceof Uri) {
                Uri uri = (Uri) args[0];
                String uriString = uri.toString();
                
                if (uriString.contains("audio") || uriString.contains("media")) {
                    Slog.d(TAG, "ContentResolver: insert called for audio URI: " + uriString);
                }
            }
            
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("update")
    public static class Update extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0 && args[0] instanceof Uri) {
                Uri uri = (Uri) args[0];
                String uriString = uri.toString();
                
                if (uriString.contains("audio") || uriString.contains("media")) {
                    Slog.d(TAG, "ContentResolver: update called for audio URI: " + uriString);
                }
            }
            
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("delete")
    public static class Delete extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0 && args[0] instanceof Uri) {
                Uri uri = (Uri) args[0];
                String uriString = uri.toString();
                
                if (uriString.contains("audio") || uriString.contains("media")) {
                    Slog.d(TAG, "ContentResolver: delete called for audio URI: " + uriString);
                }
            }
            
            return method.invoke(who, args);
        }
    }
}
