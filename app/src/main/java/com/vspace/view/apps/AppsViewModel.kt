package com.vspace.view.apps

import androidx.lifecycle.MutableLiveData
import com.vspace.bean.AppInfo
import com.vspace.data.AppsRepository
import com.vspace.view.base.BaseViewModel

class AppsViewModel(private val repo: AppsRepository) : BaseViewModel() {
    val appsLiveData = MutableLiveData<List<AppInfo>>()
    val resultLiveData = MutableLiveData<String>()
    val launchLiveData = MutableLiveData<Boolean>()
    // 利用LiveData只更新最后一次的特性，用来保存app顺序
    val updateSortLiveData = MutableLiveData<Boolean>()

    fun getInstalledApps(userId: Int) {
        launchOnUI {
            repo.getVmInstallList(userId, appsLiveData)
        }
    }

    fun install(source: String, userID: Int) {
        launchOnUI {
            repo.installApk(source, userID, resultLiveData)
        }
    }

    fun unInstall(packageName: String, userID: Int) {
        launchOnUI {
            repo.unInstall(packageName, userID, resultLiveData)
        }
    }

    fun clearApkData(packageName: String,userID: Int) {
        launchOnUI {
            repo.clearApkData(packageName,userID,resultLiveData)
        }
    }

    fun launchApk(packageName: String, userID: Int) {
        launchOnUI {
            repo.launchApk(packageName, userID, launchLiveData)
        }
    }

    fun updateApkOrder(userID: Int, dataList:List<AppInfo>) {
        launchOnUI {
            repo.updateApkOrder(userID, dataList)
        }
    }
}
