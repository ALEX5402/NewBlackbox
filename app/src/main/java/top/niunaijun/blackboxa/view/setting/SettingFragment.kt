package top.niunaijun.blackboxa.view.setting

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.launch
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.util.SnapshotManager
import top.niunaijun.blackboxa.util.toast
import top.niunaijun.blackboxa.view.gms.GmsManagerActivity

class SettingFragment : PreferenceFragmentCompat() {

    private val backupSnapshotLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            if (uri != null) {
                toast(R.string.snapshot_creating)
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = SnapshotManager.createSnapshot(requireContext(), uri)
                    if (result.isSuccess) {
                        toast(R.string.snapshot_created)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.snapshot_failed, result.exceptionOrNull()?.message ?: "Unknown error"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    private val restoreSnapshotLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                toast(R.string.snapshot_restoring)
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = SnapshotManager.restoreSnapshot(requireContext(), uri)
                    if (result.isSuccess) {
                        toast(R.string.snapshot_restored)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.snapshot_failed, result.exceptionOrNull()?.message ?: "Unknown error"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.setting, rootKey)

        initThemePreference()
        initDnsPreferences()
        initSnapshotPreferences()
        initGms()

        invalidHideState {
            val rootHidePreference: Preference = (findPreference("root_hide")!!)
            val hideRoot = AppManager.mBlackBoxLoader.hideRoot()
            rootHidePreference.setDefaultValue(hideRoot)
            rootHidePreference
        }

        invalidHideState {
            val daemonPreference: Preference = (findPreference("daemon_enable")!!)
            val mDaemonEnable = AppManager.mBlackBoxLoader.daemonEnable()
            daemonPreference.setDefaultValue(mDaemonEnable)
            daemonPreference
        }

        invalidHideState {
            val disableFlagSecurePreference: Preference = (findPreference("disable_flag_secure")!!)
            val mDisableFlagSecure = AppManager.mBlackBoxLoader.disableFlagSecure()
            disableFlagSecurePreference.setDefaultValue(mDisableFlagSecure)
            disableFlagSecurePreference
        }

        initSendLogs()
    }

    private fun initThemePreference() {
        val themePreference: ListPreference? = findPreference("app_theme")
        themePreference?.value = AppManager.mBlackBoxLoader.getThemeMode()
        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            val modeStr = newValue as? String ?: "system"
            AppManager.mBlackBoxLoader.setThemeMode(modeStr)
            when (modeStr) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            true
        }
    }

    private fun initDnsPreferences() {
        val dnsModePreference: ListPreference? = findPreference("dns_mode")
        val dnsCustomPreference: EditTextPreference? = findPreference("dns_custom_ip")

        val currentDnsMode = AppManager.mBlackBoxLoader.dnsMode()
        dnsModePreference?.value = currentDnsMode
        dnsCustomPreference?.text = AppManager.mBlackBoxLoader.customDns()
        dnsCustomPreference?.isVisible = (currentDnsMode == "custom")

        dnsModePreference?.setOnPreferenceChangeListener { _, newValue ->
            val mode = newValue as? String ?: "system"
            AppManager.mBlackBoxLoader.invalidDnsMode(mode)
            dnsCustomPreference?.isVisible = (mode == "custom")
            toast(R.string.restart_module)
            true
        }

        dnsCustomPreference?.setOnPreferenceChangeListener { _, newValue ->
            val custom = newValue as? String ?: ""
            AppManager.mBlackBoxLoader.invalidCustomDns(custom)
            toast(R.string.restart_module)
            true
        }
    }

    private fun initSnapshotPreferences() {
        val backupPreference: Preference? = findPreference("snapshot_backup")
        backupPreference?.setOnPreferenceClickListener {
            val defaultName = "BlackBox_Snapshot_${System.currentTimeMillis()}.zip"
            backupSnapshotLauncher.launch(defaultName)
            true
        }

        val restorePreference: Preference? = findPreference("snapshot_restore")
        restorePreference?.setOnPreferenceClickListener {
            restoreSnapshotLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
            true
        }
    }

    private fun initGms() {
        val gmsManagerPreference: Preference = (findPreference("gms_manager")!!)

        if (BlackBoxCore.get().isSupportGms) {
            gmsManagerPreference.setOnPreferenceClickListener {
                GmsManagerActivity.start(requireContext())
                true
            }
        } else {
            gmsManagerPreference.summary = getString(R.string.no_gms)
            gmsManagerPreference.isEnabled = false
        }
    }

    private fun invalidHideState(block: () -> Preference) {
        val pref = block()
        pref.setOnPreferenceChangeListener { preference, newValue ->
            val tmpHide = (newValue == true)
            when (preference.key) {
                "root_hide" -> {
                    AppManager.mBlackBoxLoader.invalidHideRoot(tmpHide)
                }
                "daemon_enable" -> {
                    AppManager.mBlackBoxLoader.invalidDaemonEnable(tmpHide)
                }
                "disable_flag_secure" -> {
                    AppManager.mBlackBoxLoader.invalidDisableFlagSecure(tmpHide)
                }
            }

            toast(R.string.restart_module)
            return@setOnPreferenceChangeListener true
        }
    }

    private fun initSendLogs() {
        val sendLogsPreference: Preference? = findPreference("send_logs")
        sendLogsPreference?.setOnPreferenceClickListener {
            it.isEnabled = false
            BlackBoxCore.get()
                    .sendLogs(
                            "Manual Log Upload from Settings",
                            true,
                            object : BlackBoxCore.LogSendListener {
                                override fun onSuccess() {
                                    activity?.runOnUiThread { sendLogsPreference.isEnabled = true }
                                }

                                override fun onFailure(error: String?) {
                                    activity?.runOnUiThread { sendLogsPreference.isEnabled = true }
                                }
                            }
                    )
            toast("Sending logs... (Check notifications for status)")
            true
        }
    }
}
