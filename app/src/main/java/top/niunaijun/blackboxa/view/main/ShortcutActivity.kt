package top.niunaijun.blackboxa.view.main

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import top.niunaijun.blackbox.BlackBoxCore


class ShortcutActivity:AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: android.content.Intent) {
        val pkg = intent.getStringExtra("pkg")
        val installPkg = intent.getStringExtra("installPkg")
        val installFile = intent.getStringExtra("installFile")
        val userID = intent.getIntExtra("userId", 0)

        lifecycleScope.launch {
            try {
                if (!installPkg.isNullOrEmpty()) {
                    val res = BlackBoxCore.get().installPackageAsUser(installPkg, userID)
                    Log.d("ShortcutActivity", "Install $installPkg result: ${res.success} - ${res.msg}")
                }
                if (!installFile.isNullOrEmpty()) {
                    val file = java.io.File(installFile)
                    val res = BlackBoxCore.get().installPackageAsUser(file, userID)
                    Log.d("ShortcutActivity", "Install file $installFile result: ${res.success} - ${res.msg}")
                }
                if (!pkg.isNullOrEmpty()) {
                    BlackBoxCore.get().launchApk(pkg, userID)
                }
            } catch (e: Exception) {
                Log.e("ShortcutActivity", "Error in handleIntent: ${e.message}", e)
            }
            finish()
        }
    }
}