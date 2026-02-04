package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;


public class AuthenticationProxy extends ClassInvocationStub {
    public static final String TAG = "AuthenticationProxy";

    public AuthenticationProxy() {
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

    @ProxyMethod("signIn")
    public static class SignIn extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "Authentication: Handling signIn call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Authentication: SignIn error, returning success", e);
                return createMockSignInResult();
            }
        }
    }

    @ProxyMethod("signOut")
    public static class SignOut extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "Authentication: Handling signOut call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Authentication: SignOut error, returning success", e);
                return null;
            }
        }
    }

    @ProxyMethod("isSignedIn")
    public static class IsSignedIn extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "Authentication: Handling isSignedIn call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Authentication: IsSignedIn error, returning true", e);
                return true;
            }
        }
    }

    @ProxyMethod("getCurrentUser")
    public static class GetCurrentUser extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "Authentication: Handling getCurrentUser call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Authentication: GetCurrentUser error, returning mock user", e);
                return createMockUser();
            }
        }
    }

    @ProxyMethod("getAccessToken")
    public static class GetAccessToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "Authentication: Handling getAccessToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Authentication: GetAccessToken error, returning mock token", e);
                return "mock_access_token_" + System.currentTimeMillis();
            }
        }
    }

    @ProxyMethod("refreshToken")
    public static class RefreshToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "Authentication: Handling refreshToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Authentication: RefreshToken error, returning mock token", e);
                return "mock_refresh_token_" + System.currentTimeMillis();
            }
        }
    }

    @ProxyMethod("validateToken")
    public static class ValidateToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "Authentication: Handling validateToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Authentication: ValidateToken error, returning true", e);
                return true;
            }
        }
    }

    @ProxyMethod("login")
    public static class Login extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "Authentication: Handling login call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Authentication: Login error, returning success", e);
                return createMockLoginResult();
            }
        }
    }

    @ProxyMethod("logout")
    public static class Logout extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "Authentication: Handling logout call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Authentication: Logout error, returning success", e);
                return null;
            }
        }
    }

    @ProxyMethod("isLoggedIn")
    public static class IsLoggedIn extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "Authentication: Handling isLoggedIn call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "Authentication: IsLoggedIn error, returning true", e);
                return true;
            }
        }
    }

    
    private static Object createMockSignInResult() {
        try {
            Class<?> bundleClass = Class.forName("android.os.Bundle");
            return bundleClass.newInstance();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create mock sign in result", e);
            return null;
        }
    }

    private static Object createMockLoginResult() {
        try {
            Class<?> bundleClass = Class.forName("android.os.Bundle");
            return bundleClass.newInstance();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create mock login result", e);
            return null;
        }
    }

    private static Object createMockUser() {
        try {
            
            Class<?> userClass = Class.forName("android.os.Bundle");
            return userClass.newInstance();
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create mock user", e);
            return null;
        }
    }
}
