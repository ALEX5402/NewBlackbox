package top.niunaijun.blackbox.fake.service;

import android.media.MediaRecorder;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * MediaRecorder proxy to handle audio recording in sandboxed apps.
 */
public class MediaRecorderProxy extends ClassInvocationStub {
    public static final String TAG = "MediaRecorderProxy";

    public MediaRecorderProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        return null; // Not needed for class method hooks
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // Hook MediaRecorder class methods directly
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Hook MediaRecorder constructor
    @ProxyMethod("<init>")
    public static class Constructor extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: Constructor called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook setAudioSource
    @ProxyMethod("setAudioSource")
    public static class SetAudioSource extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setAudioSource called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook setOutputFormat
    @ProxyMethod("setOutputFormat")
    public static class SetOutputFormat extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setOutputFormat called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook setAudioEncoder
    @ProxyMethod("setAudioEncoder")
    public static class SetAudioEncoder extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setAudioEncoder called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook setOutputFile
    @ProxyMethod("setOutputFile")
    public static class SetOutputFile extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setOutputFile called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook prepare
    @ProxyMethod("prepare")
    public static class Prepare extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: prepare called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook start
    @ProxyMethod("start")
    public static class Start extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: start called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook stop
    @ProxyMethod("stop")
    public static class Stop extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: stop called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook release
    @ProxyMethod("release")
    public static class Release extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: release called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook reset
    @ProxyMethod("reset")
    public static class Reset extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: reset called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook setAudioSamplingRate
    @ProxyMethod("setAudioSamplingRate")
    public static class SetAudioSamplingRate extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setAudioSamplingRate called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook setAudioChannels
    @ProxyMethod("setAudioChannels")
    public static class SetAudioChannels extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setAudioChannels called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook setAudioEncodingBitRate
    @ProxyMethod("setAudioEncodingBitRate")
    public static class SetAudioEncodingBitRate extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setAudioEncodingBitRate called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook setMaxDuration
    @ProxyMethod("setMaxDuration")
    public static class SetMaxDuration extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setMaxDuration called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook setMaxFileSize
    @ProxyMethod("setMaxFileSize")
    public static class SetMaxFileSize extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setMaxFileSize called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook setOnErrorListener
    @ProxyMethod("setOnErrorListener")
    public static class SetOnErrorListener extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setOnErrorListener called, allowing");
            return method.invoke(who, args);
        }
    }

    // Hook setOnInfoListener
    @ProxyMethod("setOnInfoListener")
    public static class SetOnInfoListener extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "MediaRecorder: setOnInfoListener called, allowing");
            return method.invoke(who, args);
        }
    }
}
