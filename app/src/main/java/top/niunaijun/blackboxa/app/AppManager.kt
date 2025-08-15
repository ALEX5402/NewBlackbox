package top.niunaijun.blackboxa.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.view.main.BlackBoxLoader

object AppManager {
    private const val TAG = "AppManager"
    
    @JvmStatic
    val mBlackBoxLoader by lazy {
        try {
            BlackBoxLoader()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating BlackBoxLoader: ${e.message}")

            BlackBoxLoader() // Try again, but this might fail
        }
    }

    @JvmStatic
    val mBlackBoxCore by lazy {
        try {
            mBlackBoxLoader.getBlackBoxCore()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting BlackBoxCore: ${e.message}")
            throw e // Re-throw as this is critical
        }
    }

    @JvmStatic
    val mRemarkSharedPreferences: SharedPreferences by lazy {
        try {
            App.getContext().getSharedPreferences("UserRemark", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating SharedPreferences: ${e.message}")
            throw e // Re-throw as this is critical
        }
    }

    fun doAttachBaseContext(context: Context) {
        try {
            mBlackBoxLoader.attachBaseContext(context)
            mBlackBoxLoader.addLifecycleCallback()
        } catch (e: Exception) {
            Log.e(TAG, "Error in doAttachBaseContext: ${e.message}")
            // Don't re-throw as this might not be critical for app startup
        }
    }

    fun doOnCreate(context: Context) {
        try {
            mBlackBoxLoader.doOnCreate(context)
            initThirdService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error in doOnCreate: ${e.message}")
            // Don't re-throw as this might not be critical for app startup
        }
    }

    private fun initThirdService(context: Context) {
        try {
            // Initialize any third-party services here
            // Currently empty, but with error handling for future additions
        } catch (e: Exception) {
            Log.e(TAG, "Error in initThirdService: ${e.message}")
        }
    }
}
