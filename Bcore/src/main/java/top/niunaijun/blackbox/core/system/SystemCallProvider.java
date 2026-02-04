package top.niunaijun.blackbox.core.system;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BundleCompat;


public class SystemCallProvider extends ContentProvider {
    public static final String TAG = "SystemCallProvider";

    @Override
    public boolean onCreate() {
        try {
            Slog.d(TAG, "SystemCallProvider onCreate called");
            return initSystem();
        } catch (Exception e) {
            Slog.e(TAG, "Error in SystemCallProvider onCreate", e);
            return false;
        }
    }

    private boolean initSystem() {
        try {
            Slog.d(TAG, "Initializing BlackBox system...");
            BlackBoxSystem.getSystem().startup();
            Slog.d(TAG, "BlackBox system initialized successfully");
            return true;
        } catch (Exception e) {
            Slog.e(TAG, "Failed to initialize BlackBox system", e);
            return false;
        }
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        try {
            Slog.d(TAG, "call: " + method + ", " + extras);
            if ("VM".equals(method)) {
                Bundle bundle = new Bundle();
                if (extras != null) {
                    String name = extras.getString("_B_|_server_name_");
                    Slog.d(TAG, "Requesting service: " + name);
                    IBinder service = ServiceManager.getService(name);
                    if (service != null) {
                        BundleCompat.putBinder(bundle, "_B_|_server_", service);
                        Slog.d(TAG, "Service " + name + " provided successfully");
                    } else {
                        Slog.w(TAG, "Service " + name + " not found");
                    }
                }
                return bundle;
            }
            return super.call(method, arg, extras);
        } catch (Exception e) {
            Slog.e(TAG, "Error in SystemCallProvider call method: " + method, e);
            
            Bundle errorBundle = new Bundle();
            errorBundle.putString("error", e.getMessage());
            return errorBundle;
        }
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
