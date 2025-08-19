package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.media.AudioManager;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Comprehensive audio permission proxy to handle all audio-related permission issues.
 */
public class AudioPermissionProxy extends BinderInvocationStub {
    public static final String TAG = "AudioPermissionProxy";

    public AudioPermissionProxy() {
        super(BRServiceManager.get().getService("audio"));
    }

    @Override
    protected Object getWho() {
        IBinder binder = BRServiceManager.get().getService("audio");
        if (binder == null) {
            Slog.e(TAG, "Failed to get audio service binder");
            return null;
        }
        
        try {
            // Try to get the audio service interface
            Class<?> stubClass = Class.forName("android.media.IAudioService$Stub");
            Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            Object iface = asInterfaceMethod.invoke(null, binder);
            
            if (iface != null) {
                Slog.d(TAG, "Successfully obtained IAudioService interface");
                return iface;
            } else {
                Slog.e(TAG, "Reflection succeeded but returned null interface");
                return null;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get IAudioService interface", e);
            return null;
        }
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("audio");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Force microphone to be unmuted
    @ProxyMethod("isMicrophoneMuted")
    public static class IsMicrophoneMuted extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: isMicrophoneMuted returning false");
            return false;
        }
    }

    // Force microphone to be unmuted
    @ProxyMethod("setMicrophoneMute")
    public static class SetMicrophoneMute extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: setMicrophoneMute called, forcing unmute");
            // Force microphone to be unmuted
            if (args != null && args.length > 0) {
                args[0] = false; // Force unmute
            }
            return method.invoke(who, args);
        }
    }

    // Force microphone to be unmuted for user
    @ProxyMethod("isMicrophoneMutedForUser")
    public static class IsMicrophoneMutedForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: isMicrophoneMutedForUser returning false");
            return false;
        }
    }

    // Force microphone to be unmuted for user
    @ProxyMethod("setMicrophoneMuteForUser")
    public static class SetMicrophoneMuteForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: setMicrophoneMuteForUser called, forcing unmute");
            // Force microphone to be unmuted
            if (args != null && args.length > 1) {
                args[1] = false; // Force unmute
            }
            return method.invoke(who, args);
        }
    }

    // Allow audio recording operations
    @ProxyMethod("startRecording")
    public static class StartRecording extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: startRecording called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio recording operations
    @ProxyMethod("stopRecording")
    public static class StopRecording extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: stopRecording called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio recording operations
    @ProxyMethod("isRecordingActive")
    public static class IsRecordingActive extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: isRecordingActive called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio recording operations
    @ProxyMethod("getRecordingState")
    public static class GetRecordingState extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: getRecordingState called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio recording state queries
    @ProxyMethod("getRecordingStateForUser")
    public static class GetRecordingStateForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: getRecordingStateForUser called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio focus requests
    @ProxyMethod("requestAudioFocus")
    public static class RequestAudioFocus extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: requestAudioFocus called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio focus client registration
    @ProxyMethod("registerAudioFocusClient")
    public static class RegisterAudioFocusClient extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: registerAudioFocusClient called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio focus client unregistration
    @ProxyMethod("unregisterAudioFocusClient")
    public static class UnregisterAudioFocusClient extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: unregisterAudioFocusClient called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio focus abandonment
    @ProxyMethod("abandonAudioFocus")
    public static class AbandonAudioFocus extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: abandonAudioFocus called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio mode changes
    @ProxyMethod("setMode")
    public static class SetMode extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: setMode called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio mode queries
    @ProxyMethod("getMode")
    public static class GetMode extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: getMode called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio stream volume changes
    @ProxyMethod("setStreamVolume")
    public static class SetStreamVolume extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: setStreamVolume called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio stream volume queries
    @ProxyMethod("getStreamVolume")
    public static class GetStreamVolume extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: getStreamVolume called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio stream max volume queries
    @ProxyMethod("getStreamMaxVolume")
    public static class GetStreamMaxVolume extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: getStreamMaxVolume called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio stream mute operations
    @ProxyMethod("setStreamMute")
    public static class SetStreamMute extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: setStreamMute called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio stream mute queries
    @ProxyMethod("isStreamMute")
    public static class IsStreamMute extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: isStreamMute called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio routing operations
    @ProxyMethod("setSpeakerphoneOn")
    public static class SetSpeakerphoneOn extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: setSpeakerphoneOn called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio routing queries
    @ProxyMethod("isSpeakerphoneOn")
    public static class IsSpeakerphoneOn extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: isSpeakerphoneOn called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow bluetooth audio operations
    @ProxyMethod("setBluetoothScoOn")
    public static class SetBluetoothScoOn extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: setBluetoothScoOn called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow bluetooth audio queries
    @ProxyMethod("isBluetoothScoOn")
    public static class IsBluetoothScoOn extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioPermission: isBluetoothScoOn called, allowing");
            return method.invoke(who, args);
        }
    }
}
