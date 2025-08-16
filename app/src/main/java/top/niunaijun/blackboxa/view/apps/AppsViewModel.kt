package top.niunaijun.blackboxa.view.apps

import androidx.lifecycle.MutableLiveData
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.data.AppsRepository
import top.niunaijun.blackboxa.view.base.BaseViewModel
import android.util.Log

/**
 *
 * @Description:
 * @Author: wukaicheng
 * @CreateDate: 2021/4/29 22:36
 */
class AppsViewModel(private val repo: AppsRepository) : BaseViewModel() {

    val appsLiveData = MutableLiveData<List<AppInfo>>()

    val resultLiveData = MutableLiveData<String>()

    val launchLiveData = MutableLiveData<Boolean>()

    //利用LiveData只更新最后一次的特性，用来保存app顺序
    val updateSortLiveData = MutableLiveData<Boolean>()

    fun getInstalledApps(userId: Int) {
        launchOnUI {
            repo.getVmInstallList(userId, appsLiveData)
        }
    }
    
    /**
     * Get installed apps with retry mechanism for when services are not ready
     */
    fun getInstalledAppsWithRetry(userId: Int, maxRetries: Int = 3) {
        var retryCount = 0
        
        fun attemptLoad() {
            launchOnUI {
                repo.getVmInstallList(userId, appsLiveData)
                
                // Check if we got any apps, if not and we haven't exceeded retries, try again
                val currentApps = appsLiveData.value
                if ((currentApps == null || currentApps.isEmpty()) && retryCount < maxRetries) {
                    retryCount++
                    Log.d("AppsViewModel", "No apps loaded, retrying... (${retryCount}/${maxRetries})")
                    
                    // Wait a bit before retrying
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        attemptLoad()
                    }, 1000) // Wait 1 second before retry
                }
            }
        }
        
        attemptLoad()
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

    fun clearApkData(packageName: String,userID: Int){
        launchOnUI {
            repo.clearApkData(packageName,userID,resultLiveData)
        }
    }

    fun launchApk(packageName: String, userID: Int) {
        launchOnUI {
            repo.launchApk(packageName, userID, launchLiveData)
        }
    }

    fun updateApkOrder(userID: Int,dataList:List<AppInfo>){
        launchOnUI {
            repo.updateApkOrder(userID,dataList)
        }
    }
}