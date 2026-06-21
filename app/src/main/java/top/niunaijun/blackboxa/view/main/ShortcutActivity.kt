package top.niunaijun.blackboxa.view.main

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import top.niunaijun.blackbox.BlackBoxCore


class ShortcutActivity:AppCompatActivity() {
    companion object {
        private const val TAG = "ShortcutActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pkg = intent.getStringExtra("pkg")
        val userID = intent.getIntExtra("userId",0)
        if (pkg.isNullOrBlank() || userID < 0 || !BlackBoxCore.get().isInstalled(pkg, userID)) {
            Log.w(TAG, "Ignoring invalid shortcut launch request: pkg=$pkg, userId=$userID")
            finish()
            return
        }

        lifecycleScope.launch {
            BlackBoxCore.get().launchApk(pkg,userID)
            finish()
        }
    }
}
