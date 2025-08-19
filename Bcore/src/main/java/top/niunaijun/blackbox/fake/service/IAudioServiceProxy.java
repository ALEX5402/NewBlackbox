package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Reflector;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Audio service proxy to avoid focus/mute related issues in virtualized apps.
 */
public class IAudioServiceProxy extends BinderInvocationStub {
    public static final String TAG = "AudioServiceProxy";

    public IAudioServiceProxy() {
        super(BRServiceManager.get().getService(Context.AUDIO_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder binder = BRServiceManager.get().getService(Context.AUDIO_SERVICE);
        if (binder == null) {
            Slog.e(TAG, "Failed to get AUDIO_SERVICE binder");
            return null;
        }
        
        try {
            // Try multiple reflection paths for different Android versions
            Object iface = null;
            
            // Android 16+ path
            try {
                iface = Reflector.on("android.media.IAudioService$Stub").call("asInterface", binder);
            } catch (Exception e1) {
                Slog.d(TAG, "Failed Android 16+ path, trying alternative: " + e1.getMessage());
                
                // Alternative path for older versions
                try {
                    iface = Reflector.on("android.media.IAudioService").call("asInterface", binder);
                } catch (Exception e2) {
                    Slog.d(TAG, "Failed alternative path: " + e2.getMessage());
                    
                    // Last resort: try direct interface casting
                    try {
                        Class<?> stubClass = Class.forName("android.media.IAudioService$Stub");
                        Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
                        iface = asInterfaceMethod.invoke(null, binder);
                    } catch (Exception e3) {
                        Slog.e(TAG, "All reflection paths failed for IAudioService", e3);
                        return null;
                    }
                }
            }
            
            if (iface != null) {
                Slog.d(TAG, "Successfully obtained IAudioService interface");
                return (IInterface) iface;
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
        replaceSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Always allow microphone to be unmuted in the virtual app
    @ProxyMethod("isMicrophoneMuted")
    public static class IsMicrophoneMuted extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioService: isMicrophoneMuted returning false");
            return false;
        }
    }

    // Ignore attempts to force microphone mute via AudioService
    @ProxyMethod("setMicrophoneMute")
    public static class SetMicrophoneMute extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioService: setMicrophoneMute called, forcing unmute");
            // Force microphone to be unmuted
            if (args != null && args.length > 0) {
                args[0] = false; // Force unmute
            }
            return method.invoke(who, args);
        }
    }

    // Allow audio recording operations
    @ProxyMethod("startRecording")
    public static class StartRecording extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioService: startRecording called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio recording operations
    @ProxyMethod("stopRecording")
    public static class StopRecording extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioService: stopRecording called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio recording operations
    @ProxyMethod("isRecordingActive")
    public static class IsRecordingActive extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioService: isRecordingActive called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio recording operations
    @ProxyMethod("getRecordingState")
    public static class GetRecordingState extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioService: getRecordingState called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow microphone access
    @ProxyMethod("isMicrophoneMutedForUser")
    public static class IsMicrophoneMutedForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioService: isMicrophoneMutedForUser returning false");
            return false;
        }
    }

    // Allow microphone access
    @ProxyMethod("setMicrophoneMuteForUser")
    public static class SetMicrophoneMuteForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioService: setMicrophoneMuteForUser called, forcing unmute");
            // Force microphone to be unmuted
            if (args != null && args.length > 1) {
                args[1] = false; // Force unmute
            }
            return method.invoke(who, args);
        }
    }

    // Allow audio recording state queries
    @ProxyMethod("getRecordingStateForUser")
    public static class GetRecordingStateForUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioService: getRecordingStateForUser called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio focus requests
    @ProxyMethod("requestAudioFocus")
    public static class RequestAudioFocus extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioService: requestAudioFocus called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio focus client registration
    @ProxyMethod("registerAudioFocusClient")
    public static class RegisterAudioFocusClient extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioService: registerAudioFocusClient called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio focus client unregistration
    @ProxyMethod("unregisterAudioFocusClient")
    public static class UnregisterAudioFocusClient extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioService: unregisterAudioFocusClient called, allowing");
            return method.invoke(who, args);
        }
    }

    // Allow audio focus abandonment
    @ProxyMethod("abandonAudioFocus")
    public static class AbandonAudioFocus extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Slog.d(TAG, "AudioService: abandonAudioFocus called, allowing");
            return method.invoke(who, args);
        }
    }
}
