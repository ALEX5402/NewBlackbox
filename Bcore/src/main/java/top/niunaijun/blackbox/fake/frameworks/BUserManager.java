package top.niunaijun.blackbox.fake.frameworks;

import android.os.DeadObjectException;
import android.os.RemoteException;
import android.util.Log;

import java.util.Collections;
import java.util.List;

import top.niunaijun.blackbox.core.system.ServiceManager;
import top.niunaijun.blackbox.core.system.user.BUserInfo;
import top.niunaijun.blackbox.core.system.user.IBUserManagerService;
import top.niunaijun.blackbox.utils.Slog;

/**
 * updated by alex5402 on 4/28/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK (USDT TRC20)
 */
public class BUserManager extends BlackManager<IBUserManagerService> {
    private static final String TAG = "BUserManager";
    private static final BUserManager sUserManager = new BUserManager();

    public static BUserManager get() {
        return sUserManager;
    }

    @Override
    protected String getServiceName() {
        return ServiceManager.USER_MANAGER;
    }

    public BUserInfo createUser(int userId) {
        try {
            IBUserManagerService service = getService();
            if (service != null) {
                return service.createUser(userId);
            } else {
                Slog.w(TAG, "UserManager service is null, cannot create user");
            }
        } catch (DeadObjectException e) {
            Slog.w(TAG, "UserManager service died during createUser, clearing cache and retrying", e);
            clearServiceCache();
            try {
                Thread.sleep(100);
                IBUserManagerService service = getService();
                if (service != null) {
                    return service.createUser(userId);
                }
            } catch (Exception retryException) {
                Slog.e(TAG, "Failed to create user after retry", retryException);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException in createUser", e);
        } catch (Exception e) {
            Slog.e(TAG, "Unexpected error in createUser", e);
        }
        return null;
    }

    public void deleteUser(int userId) {
        try {
            IBUserManagerService service = getService();
            if (service != null) {
                service.deleteUser(userId);
            } else {
                Slog.w(TAG, "UserManager service is null, cannot delete user");
            }
        } catch (DeadObjectException e) {
            Slog.w(TAG, "UserManager service died during deleteUser, clearing cache and retrying", e);
            clearServiceCache();
            try {
                Thread.sleep(100);
                IBUserManagerService service = getService();
                if (service != null) {
                    service.deleteUser(userId);
                }
            } catch (Exception retryException) {
                Slog.e(TAG, "Failed to delete user after retry", retryException);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException in deleteUser", e);
        } catch (Exception e) {
            Slog.e(TAG, "Unexpected error in deleteUser", e);
        }
    }

    public List<BUserInfo> getUsers() {
        try {
            IBUserManagerService service = getService();
            if (service != null) {
                return service.getUsers();
            } else {
                Slog.w(TAG, "UserManager service is null, returning empty list");
            }
        } catch (DeadObjectException e) {
            Slog.w(TAG, "UserManager service died during getUsers, clearing cache and retrying", e);
            clearServiceCache();
            try {
                Thread.sleep(100);
                IBUserManagerService service = getService();
                if (service != null) {
                    return service.getUsers();
                }
            } catch (Exception retryException) {
                Slog.e(TAG, "Failed to get users after retry", retryException);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException in getUsers", e);
        } catch (Exception e) {
            Slog.e(TAG, "Unexpected error in getUsers", e);
        }
        return Collections.emptyList();
    }
}
