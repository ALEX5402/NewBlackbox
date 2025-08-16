package top.niunaijun.blackboxa.app.rocker

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 *
 * @Description:
 * @Author: alex5402
 * @CreateDate: Saturday, August 16, 2025
 */
interface BaseActivityLifecycleCallback : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }
    override fun onActivityStarted(activity: Activity) {

    }
    override fun onActivityResumed(activity: Activity) {

    }
    override fun onActivityPaused(activity: Activity) {

    }
    override fun onActivityStopped(activity: Activity) {

    }
    override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {

    }
    override fun onActivityDestroyed(activity: Activity) {

    }
}