package top.niunaijun.blackboxa.view.xp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import top.niunaijun.blackboxa.data.XpRepository

@Suppress("UNCHECKED_CAST")
class XpFactory(private val xpRepository: XpRepository) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return XpViewModel(xpRepository) as T
    }
}
