package top.niunaijun.blackboxa.data

import android.content.pm.ApplicationInfo
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.BlackBoxCore.getPackageManager
import top.niunaijun.blackbox.utils.AbiUtils
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.bean.InstalledAppBean
import top.niunaijun.blackboxa.util.getString
import top.niunaijun.blackbox.fake.frameworks.BResourcesManager
import java.io.File


/**
 *
 * @Description:
 * @Author: wukaicheng
 * @CreateDate: 2021/4/29 23:05
 */

class AppsRepository {
    val TAG: String = "AppsRepository"
    private var mInstalledList = mutableListOf<AppInfo>()
    
    /**
     * Safely load app label with fallback to package name
     */
    private fun safeLoadAppLabel(applicationInfo: ApplicationInfo): String {
        return try {
            getPackageManager().getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load label for ${applicationInfo.packageName}: ${e.message}")
            applicationInfo.packageName // Fallback to package name
        }
    }
    
    /**
     * Safely load app icon with fallback to null
     */
    private fun safeLoadAppIcon(applicationInfo: ApplicationInfo): android.graphics.drawable.Drawable? {
        return try {
            getPackageManager().getApplicationIcon(applicationInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load icon for ${applicationInfo.packageName}: ${e.message}")
            null // Fallback to null icon
        }
    }

    fun previewInstallList() {
        try {
            synchronized(mInstalledList) {
                val installedApplications: List<ApplicationInfo> =
                    getPackageManager().getInstalledApplications(0)
                val installedList = mutableListOf<AppInfo>()

                for (installedApplication in installedApplications) {
                    try {
                        val file = File(installedApplication.sourceDir)

                        if ((installedApplication.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue

                        if (!AbiUtils.isSupport(file)) continue

                        val isXpModule = BlackBoxCore.get().isXposedModule(file)

                        val info = AppInfo(
                            safeLoadAppLabel(installedApplication),
                            safeLoadAppIcon(installedApplication), // Remove the !! operator to allow null icons
                            installedApplication.packageName,
                            installedApplication.sourceDir,
                            isXpModule
                        )
                        installedList.add(info)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing app ${installedApplication.packageName}: ${e.message}")
                    }
                }
                this.mInstalledList.clear()
                this.mInstalledList.addAll(installedList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in previewInstallList: ${e.message}")
        }
    }

    fun getInstalledAppList(
        userID: Int,
        loadingLiveData: MutableLiveData<Boolean>,
        appsLiveData: MutableLiveData<List<InstalledAppBean>>
    ) {
        try {
            loadingLiveData.postValue(true)
            synchronized(mInstalledList) {
                val blackBoxCore = BlackBoxCore.get()
                Log.d(TAG, mInstalledList.joinToString(","))
                val newInstalledList = mInstalledList.map {
                    InstalledAppBean(
                        it.name,
                        it.icon, // Remove the !! operator to allow null icons
                        it.packageName,
                        it.sourceDir,
                        blackBoxCore.isInstalled(it.packageName, userID)
                    )
                }
                appsLiveData.postValue(newInstalledList)
                loadingLiveData.postValue(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getInstalledAppList: ${e.message}")
            loadingLiveData.postValue(false)
            appsLiveData.postValue(emptyList())
        }
    }

    fun getInstalledModuleList(
        loadingLiveData: MutableLiveData<Boolean>,
        appsLiveData: MutableLiveData<List<InstalledAppBean>>
    ) {
        try {
            loadingLiveData.postValue(true)
            synchronized(mInstalledList) {
                val blackBoxCore = BlackBoxCore.get()
                val moduleList = mInstalledList.filter {
                    it.isXpModule
                }.map {
                    InstalledAppBean(
                        it.name,
                        it.icon, // Remove the !! operator to allow null icons
                        it.packageName,
                        it.sourceDir,
                        blackBoxCore.isInstalledXposedModule(it.packageName)
                    )
                }
                appsLiveData.postValue(moduleList)
                loadingLiveData.postValue(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getInstalledModuleList: ${e.message}")
            loadingLiveData.postValue(false)
            appsLiveData.postValue(emptyList())
        }
    }

    fun getVmInstallList(userId: Int, appsLiveData: MutableLiveData<List<AppInfo>>) {
        try {
            val blackBoxCore = BlackBoxCore.get()
            
            // Add debugging for users
            val users = blackBoxCore.users
            Log.d(TAG, "getVmInstallList: userId=$userId, total users=${users.size}")
            users.forEach { user ->
                Log.d(TAG, "User: id=${user.id}, name=${user.name}")
            }
            
            val sortListData =
                AppManager.mRemarkSharedPreferences.getString("AppList$userId", "")
            val sortList = sortListData?.split(",")

            val applicationList = blackBoxCore.getInstalledApplications(0, userId)
            
            // Add null check for applicationList
            if (applicationList == null) {
                Log.e(TAG, "getVmInstallList: applicationList is null for userId=$userId")
                appsLiveData.postValue(emptyList())
                return
            }
            
            // Add debugging
            Log.d(TAG, "getVmInstallList: userId=$userId, applicationList.size=${applicationList.size}")
            if (applicationList.isNotEmpty()) {
                Log.d(TAG, "First app: ${applicationList.first().packageName}")
            } else {
                Log.w(TAG, "getVmInstallList: No applications found for userId=$userId")
            }

            val appInfoList = mutableListOf<AppInfo>()
            
            // Sort the application list if sort data exists
            val sortedApplicationList = if (!sortList.isNullOrEmpty()) {
                applicationList.sortedWith(AppsSortComparator(sortList))
            } else {
                applicationList
            }
            
            // Process each application
            sortedApplicationList.forEach { applicationInfo ->
                try {
                    // Add null check for applicationInfo
                    if (applicationInfo == null) {
                        Log.w(TAG, "getVmInstallList: Skipping null applicationInfo")
                        return@forEach
                    }
                    
                    val info = AppInfo(
                        safeLoadAppLabel(applicationInfo),
                        safeLoadAppIcon(applicationInfo), // Remove the !! operator to allow null icons
                        applicationInfo.packageName,
                        applicationInfo.sourceDir,
                        isInstalledXpModule(applicationInfo.packageName)
                    )

                    appInfoList.add(info)
                } catch (e: Exception) {
                    Log.e(TAG, "getVmInstallList: Error processing app ${applicationInfo?.packageName}: ${e.message}")
                }
            }

            Log.d(TAG, "getVmInstallList: processed ${appInfoList.size} apps")
            appsLiveData.postValue(appInfoList)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getVmInstallList: ${e.message}")
            appsLiveData.postValue(emptyList())
        }
    }

    private fun isInstalledXpModule(packageName: String): Boolean {
        return try {
            BlackBoxCore.get().installedXPModules.forEach {
                if (packageName == it.packageName) {
                    return@isInstalledXpModule true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Xposed module: ${e.message}")
            false
        }
    }

    fun installApk(source: String, userId: Int, resultLiveData: MutableLiveData<String>) {
        try {
            val blackBoxCore = BlackBoxCore.get()
            val installResult = if (URLUtil.isValidUrl(source)) {
                val uri = Uri.parse(source)
                blackBoxCore.installPackageAsUser(uri, userId)
            } else {
                blackBoxCore.installPackageAsUser(source, userId)
            }

            if (installResult.success) {
                updateAppSortList(userId, installResult.packageName, true)
                resultLiveData.postValue(getString(R.string.install_success))
            } else {
                resultLiveData.postValue(getString(R.string.install_fail, installResult.msg))
            }
            scanUser()
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK: ${e.message}")
            resultLiveData.postValue("Installation failed: ${e.message}")
        }
    }

    fun unInstall(packageName: String, userID: Int, resultLiveData: MutableLiveData<String>) {
        try {
            BlackBoxCore.get().uninstallPackageAsUser(packageName, userID)
            updateAppSortList(userID, packageName, false)
            scanUser()
            resultLiveData.postValue(getString(R.string.uninstall_success))
        } catch (e: Exception) {
            Log.e(TAG, "Error uninstalling APK: ${e.message}")
            resultLiveData.postValue("Uninstallation failed: ${e.message}")
        }
    }

    fun launchApk(packageName: String, userId: Int, launchLiveData: MutableLiveData<Boolean>) {
        try {
            val result = BlackBoxCore.get().launchApk(packageName, userId)
            launchLiveData.postValue(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching APK: ${e.message}")
            launchLiveData.postValue(false)
        }
    }

    fun clearApkData(packageName: String, userID: Int, resultLiveData: MutableLiveData<String>) {
        try {
            BlackBoxCore.get().clearPackage(packageName, userID)
            resultLiveData.postValue(getString(R.string.clear_success))
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing APK data: ${e.message}")
            resultLiveData.postValue("Clear failed: ${e.message}")
        }
    }

    /**
     * 倒序递归扫描用户，
     * 如果用户是空的，就删除用户，删除用户备注，删除应用排序列表
     */
    private fun scanUser() {
        try {
            val blackBoxCore = BlackBoxCore.get()
            val userList = blackBoxCore.users

            if (userList.isEmpty()) {
                return
            }

            val id = userList.last().id

            if (blackBoxCore.getInstalledApplications(0, id).isEmpty()) {
                blackBoxCore.deleteUser(id)
                AppManager.mRemarkSharedPreferences.edit {
                    remove("Remark$id")
                    remove("AppList$id")
                }
                scanUser()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in scanUser: ${e.message}")
        }
    }

    /**
     * 更新排序列表
     * @param userID Int
     * @param pkg String
     * @param isAdd Boolean true是添加，false是移除
     */
    private fun updateAppSortList(userID: Int, pkg: String, isAdd: Boolean) {
        try {
            val savedSortList =
                AppManager.mRemarkSharedPreferences.getString("AppList$userID", "")

            val sortList = linkedSetOf<String>()
            if (savedSortList != null) {
                sortList.addAll(savedSortList.split(","))
            }

            if (isAdd) {
                sortList.add(pkg)
            } else {
                sortList.remove(pkg)
            }

            AppManager.mRemarkSharedPreferences.edit {
                putString("AppList$userID", sortList.joinToString(","))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating app sort list: ${e.message}")
        }
    }

    /**
     * 保存排序后的apk顺序
     */
    fun updateApkOrder(userID: Int, dataList: List<AppInfo>) {
        try {
            AppManager.mRemarkSharedPreferences.edit {
                putString("AppList$userID",
                    dataList.joinToString(",") { it.packageName })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating APK order: ${e.message}")
        }
    }
}
