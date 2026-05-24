package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.Build;
import android.webkit.WebView;

import java.io.File;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.app.BActivityThread;


public class WebViewProxy implements IInjectHook {
    public static final String TAG = "WebViewProxy";
    private static final Object WEBVIEW_DATA_DIR_LOCK = new Object();
    private static String sInstalledDataDirectorySuffix;

    public WebViewProxy() {
    }

    @Override
    public void injectHook() {
        ensureDataDirectorySuffix(BActivityThread.getAppPackageName(), BActivityThread.getAppProcessName());
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    public static void ensureDataDirectorySuffix(String packageName, String processName) {
        String suffix = buildDataDirectorySuffix(packageName, processName);
        if (suffix == null) {
            Slog.d(TAG, "WebView: skip data directory suffix because app package is not ready");
            return;
        }

        synchronized (WEBVIEW_DATA_DIR_LOCK) {
            if (suffix.equals(sInstalledDataDirectorySuffix)) {
                return;
            }

            File dataDir = ensureHostWebViewDir(suffix);
            if (dataDir != null) {
                System.setProperty("webview.data.dir", dataDir.getAbsolutePath());
                System.setProperty("webview.cache.dir", new File(dataDir, "cache").getAbsolutePath());
                System.setProperty("webview.cookies.dir", new File(dataDir, "cookies").getAbsolutePath());
            }

            boolean installed = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    WebView.setDataDirectorySuffix(suffix);
                    Slog.d(TAG, "WebView: installed data directory suffix: " + suffix);
                } catch (IllegalStateException e) {
                    Slog.w(TAG, "WebView: data directory suffix was already locked by WebView", e);
                } catch (Throwable e) {
                    Slog.w(TAG, "WebView: failed to install data directory suffix", e);
                    installed = false;
                }
            }
            if (installed) {
                sInstalledDataDirectorySuffix = suffix;
            }
        }
    }

    private static String buildDataDirectorySuffix(String packageName, String processName) {
        if (packageName == null || packageName.length() == 0) {
            return null;
        }
        String currentProcess = processName;
        if (currentProcess == null || currentProcess.length() == 0) {
            currentProcess = packageName;
        }
        return sanitizeSuffix(BActivityThread.getUserId() + "_" + packageName + "_" + currentProcess);
    }

    private static String sanitizeSuffix(String suffix) {
        StringBuilder builder = new StringBuilder(suffix.length());
        for (int i = 0; i < suffix.length(); i++) {
            char c = suffix.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-') {
                builder.append(c);
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
    }

    private static File ensureHostWebViewDir(String suffix) {
        try {
            Context context = BlackBoxCore.getContext();
            if (context == null || context.getApplicationInfo() == null) {
                return null;
            }
            File dataDir = new File(new File(context.getApplicationInfo().dataDir, "webview"), suffix);
            File cacheDir = new File(dataDir, "cache");
            File cookiesDir = new File(dataDir, "cookies");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            if (!cookiesDir.exists()) {
                cookiesDir.mkdirs();
            }
            return dataDir;
        } catch (Throwable e) {
            Slog.w(TAG, "WebView: failed to prepare host data directory", e);
            return null;
        }
    }
}
