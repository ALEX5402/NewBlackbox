package top.niunaijun.blackbox.fake.service;

import android.media.MediaRecorder;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * MediaRecorder class proxy to directly hook MediaRecorder instance methods.
 */
public class MediaRecorderClassProxy extends ClassInvocationStub {
    public static final String TAG = "MediaRecorderClassProxy";

    public MediaRecorderClassProxy() {
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
}
