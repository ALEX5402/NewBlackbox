package top.niunaijun.blackboxa.view.xp

import androidx.lifecycle.MutableLiveData
import top.niunaijun.blackboxa.bean.XpModuleInfo
import top.niunaijun.blackboxa.data.XpRepository
import top.niunaijun.blackboxa.view.base.BaseViewModel

class XpViewModel(private val repo: XpRepository) : BaseViewModel() {

    val appsLiveData = MutableLiveData<List<XpModuleInfo>>()

    val resultLiveData = MutableLiveData<String>()

    fun getInstalledModule() {
        launchOnUI { repo.getXpModuleList(appsLiveData) }
    }

    fun installModule(source: String) {
        launchOnUI { repo.installModule(source, resultLiveData) }
    }

    fun unInstallModule(packageName: String) {
        launchOnUI { repo.unInstallModule(packageName, resultLiveData) }
    }
}
