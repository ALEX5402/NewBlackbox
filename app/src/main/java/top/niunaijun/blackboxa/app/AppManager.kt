package top.niunaijun.blackboxa.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import top.niunaijun.blackboxa.view.main.BlackBoxLoader


object AppManager {
    private const val TAG = "AppManager"

    @JvmStatic
    val mBlackBoxLoader by lazy {
        try {
            BlackBoxLoader()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating BlackBoxLoader: ${e.message}")

            BlackBoxLoader() 
        }
    }

    @JvmStatic
    val mBlackBoxCore by lazy {
        try {
            mBlackBoxLoader.getBlackBoxCore()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting BlackBoxCore: ${e.message}")
            throw e 
        }
    }

    @JvmStatic
    val mRemarkSharedPreferences: SharedPreferences by lazy {
        try {
            App.getContext().getSharedPreferences("UserRemark", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating SharedPreferences: ${e.message}")
            throw e 
        }
    }

    fun doAttachBaseContext(context: Context) {
        try {
            mBlackBoxLoader.attachBaseContext(context)
            mBlackBoxLoader.addLifecycleCallback()
        } catch (e: Exception) {
            Log.e(TAG, "Error in doAttachBaseContext: ${e.message}")
            
        }
    }

    fun doOnCreate(context: Context) {
        try {
            mBlackBoxLoader.doOnCreate(context)
            initThirdService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error in doOnCreate: ${e.message}")
            
        }
    }

    private fun initThirdService(context: Context) {
        try {
            
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in initThirdService: ${e.message}")
        }
    }
}
