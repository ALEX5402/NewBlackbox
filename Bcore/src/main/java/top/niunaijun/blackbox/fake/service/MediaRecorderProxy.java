package top.niunaijun.blackbox.fake.service;

import android.media.MediaRecorder;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


public class MediaRecorderProxy extends ClassInvocationStub {
    public static final String TAG = "MediaRecorderProxy";

    public MediaRecorderProxy() {
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

    
    @ProxyMethod("setAudioSamplingRate")
    public static class SetAudioSamplingRate extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setAudioSamplingRate called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("setAudioChannels")
    public static class SetAudioChannels extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setAudioChannels called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("setAudioEncodingBitRate")
    public static class SetAudioEncodingBitRate extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setAudioEncodingBitRate called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("setMaxDuration")
    public static class SetMaxDuration extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setMaxDuration called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("setMaxFileSize")
    public static class SetMaxFileSize extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setMaxFileSize called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("setOnErrorListener")
    public static class SetOnErrorListener extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setOnErrorListener called, allowing");
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("setOnInfoListener")
    public static class SetOnInfoListener extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setOnInfoListener called, allowing");
            return method.invoke(who, args);
        }
    }
}
