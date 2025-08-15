package top.niunaijun.blackboxa.view.main

import android.app.Application
import android.content.Context
import android.util.Log
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.BActivityThread
import top.niunaijun.blackbox.app.configuration.AppLifecycleCallback
import top.niunaijun.blackbox.app.configuration.ClientConfiguration
import top.niunaijun.blackboxa.app.App
import top.niunaijun.blackboxa.biz.cache.AppSharedPreferenceDelegate
import java.io.File

/**
 *
 * @Description:
 * @Author: wukaicheng
 * @CreateDate: 2021/5/6 23:38
 */
class BlackBoxLoader {

    private var mHideRoot by AppSharedPreferenceDelegate(App.getContext(), false)
    private var mHideXposed by AppSharedPreferenceDelegate(App.getContext(), false)
    private var mDaemonEnable by AppSharedPreferenceDelegate(App.getContext(), false)
    private var mShowShortcutPermissionDialog by AppSharedPreferenceDelegate(App.getContext(), true)

    fun hideRoot(): Boolean {
        return try {
            mHideRoot
        } catch (e: Exception) {
            Log.e(TAG, "Error getting hideRoot: ${e.message}")
            false
        }
    }

    fun invalidHideRoot(hideRoot: Boolean) {
        try {
            this.mHideRoot = hideRoot
        } catch (e: Exception) {
            Log.e(TAG, "Error setting hideRoot: ${e.message}")
        }
    }

    fun hideXposed(): Boolean {
        return try {
            mHideXposed
        } catch (e: Exception) {
            Log.e(TAG, "Error getting hideXposed: ${e.message}")
            false
        }
    }

    fun invalidHideXposed(hideXposed: Boolean) {
        try {
            this.mHideXposed = hideXposed
        } catch (e: Exception) {
            Log.e(TAG, "Error setting hideXposed: ${e.message}")
        }
    }

    fun daemonEnable(): Boolean {
        return try {
            mDaemonEnable
        } catch (e: Exception) {
            Log.e(TAG, "Error getting daemonEnable: ${e.message}")
            false
        }
    }

    fun invalidDaemonEnable(enable: Boolean) {
        try {
            this.mDaemonEnable = enable
        } catch (e: Exception) {
            Log.e(TAG, "Error setting daemonEnable: ${e.message}")
        }
    }

    fun showShortcutPermissionDialog(): Boolean {
        return try {
            mShowShortcutPermissionDialog
        } catch (e: Exception) {
            Log.e(TAG, "Error getting showShortcutPermissionDialog: ${e.message}")
            true
        }
    }

    fun invalidShortcutPermissionDialog(show: Boolean) {
        try {
            this.mShowShortcutPermissionDialog = show
        } catch (e: Exception) {
            Log.e(TAG, "Error setting showShortcutPermissionDialog: ${e.message}")
        }
    }

    fun getBlackBoxCore(): BlackBoxCore {
        return try {
            BlackBoxCore.get()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting BlackBoxCore: ${e.message}")
            throw e
        }
    }

    fun addLifecycleCallback() {
        try {
            BlackBoxCore.get().addAppLifecycleCallback(object : AppLifecycleCallback() {
                override fun beforeCreateApplication(
                    packageName: String?,
                    processName: String?,
                    context: Context?,
                    userId: Int
                ) {
                    try {
                        Log.d(
                            TAG,
                            "beforeCreateApplication: pkg $packageName, processName $processName,userID:${BActivityThread.getUserId()}"
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in beforeCreateApplication: ${e.message}")
                    }
                }

                override fun beforeApplicationOnCreate(
                    packageName: String?,
                    processName: String?,
                    application: Application?,
                    userId: Int
                ) {
                    try {
                        Log.d(TAG, "beforeApplicationOnCreate: pkg $packageName, processName $processName")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in beforeApplicationOnCreate: ${e.message}")
                    }
                }

                override fun afterApplicationOnCreate(
                    packageName: String?,
                    processName: String?,
                    application: Application?,
                    userId: Int
                ) {
                    try {
                        Log.d(TAG, "afterApplicationOnCreate: pkg $packageName, processName $processName")
                        // RockerManager.init(application,userId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in afterApplicationOnCreate: ${e.message}")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error adding lifecycle callback: ${e.message}")
        }
    }

    fun attachBaseContext(context: Context) {
        try {
            BlackBoxCore.get().doAttachBaseContext(context, object : ClientConfiguration() {
                override fun getHostPackageName(): String {
                    return try {
                        context.packageName
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting package name: ${e.message}")
                        "unknown"
                    }
                }

                override fun isHideRoot(): Boolean {
                    return try {
                        mHideRoot
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking hideRoot: ${e.message}")
                        false
                    }
                }

                override fun isHideXposed(): Boolean {
                    return try {
                        mHideXposed
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking hideXposed: ${e.message}")
                        false
                    }
                }

                override fun isEnableDaemonService(): Boolean {
                    return try {
                        mDaemonEnable
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking daemonEnable: ${e.message}")
                        false
                    }
                }

                override fun requestInstallPackage(file: File?, userId: Int): Boolean {
                    return try {
                        if (file == null) {
                            Log.w(TAG, "requestInstallPackage: file is null")
                            return false
                        }
                        val packageInfo =
                            context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                        false
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in requestInstallPackage: ${e.message}")
                        false
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error in attachBaseContext: ${e.message}")
        }
    }

    fun doOnCreate(context: Context) {
        try {
            BlackBoxCore.get().doCreate()
        } catch (e: Exception) {
            Log.e(TAG, "Error in doOnCreate: ${e.message}")
        }
    }

    companion object {
        val TAG: String = BlackBoxLoader::class.java.simpleName
    }
}