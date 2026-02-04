package top.niunaijun.blackbox.fake.service;

import android.media.MediaRecorder;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


public class MediaRecorderClassProxy extends ClassInvocationStub {
    public static final String TAG = "MediaRecorderClassProxy";

    public MediaRecorderClassProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        return null; 
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    
    @ProxyMethod("<init>")
    public static class Constructor extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: Constructor called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("setAudioSource")
    public static class SetAudioSource extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setAudioSource called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("setOutputFormat")
    public static class SetOutputFormat extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setOutputFormat called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("setAudioEncoder")
    public static class SetAudioEncoder extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setAudioEncoder called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("setOutputFile")
    public static class SetOutputFile extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setOutputFile called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("prepare")
    public static class Prepare extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: prepare called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("start")
    public static class Start extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: start called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("stop")
    public static class Stop extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: stop called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("release")
    public static class Release extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: release called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("reset")
    public static class Reset extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: reset called, allowing");
            return method.invoke(who, args);
        }
    }
}
