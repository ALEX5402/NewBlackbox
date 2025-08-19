package top.niunaijun.blackbox.app.configuration;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * updated by alex5402 on 5/5/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK (USDT TRC20)
 */
public class AppLifecycleCallback implements Application.ActivityLifecycleCallbacks {
    public static AppLifecycleCallback EMPTY = new AppLifecycleCallback() {

    };

    public void beforeMainLaunchApk(String packageName, int userid) {

    }

    public void beforeMainApplicationAttach(Application app, Context context) {

    }

    public void afterMainApplicationAttach(Application app, Context context) {

    }

    public void beforeMainActivityOnCreate(Activity activity) {

    }

    public void afterMainActivityOnCreate(Activity activity) {

    }

    public void beforeCreateApplication(String packageName, String processName, Context context, int userId) {

    }

    public void beforeApplicationOnCreate(String packageName, String processName, Application application, int userId) {

    }

    public void afterApplicationOnCreate(String packageName, String processName, Application application, int userId) {

    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
