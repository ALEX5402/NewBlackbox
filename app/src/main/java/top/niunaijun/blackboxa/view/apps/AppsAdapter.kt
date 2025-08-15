package top.niunaijun.blackboxa.view.apps

import android.view.View
import android.view.ViewGroup
import cbfg.rvadapter.RVHolder
import cbfg.rvadapter.RVHolderFactory
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.databinding.ItemAppBinding

/**
 *
 * @Description: 软件显示界面适配器
 * @Author: wukaicheng
 * @CreateDate: 2021/4/29 21:52
 */

class AppsAdapter : RVHolderFactory() {

    override fun createViewHolder(parent: ViewGroup?, viewType: Int, item: Any): RVHolder<out Any> {
        return AppsVH(inflate(R.layout.item_app,parent))
    }


    class AppsVH(itemView:View):RVHolder<AppInfo>(itemView){

        val binding = ItemAppBinding.bind(itemView)

        override fun setContent(item: AppInfo, isSelected: Boolean, payload: Any?) {
            try {
                // Safely set the icon with null check
                if (item.icon != null) {
                    binding.icon.setImageDrawable(item.icon)
                } else {
                    // Set a default icon or clear the image view
                    binding.icon.setImageDrawable(null)
                }
                
                binding.name.text = item.name
                if(item.isXpModule){
                    binding.cornerLabel.visibility = View.VISIBLE
                }else{
                    binding.cornerLabel.visibility = View.INVISIBLE
                }
            } catch (e: Exception) {
                // Log error and set safe defaults
                android.util.Log.e("AppsAdapter", "Error setting content for ${item.packageName}: ${e.message}")
                binding.icon.setImageDrawable(null)
                binding.name.text = item.name ?: "Unknown App"
                binding.cornerLabel.visibility = View.INVISIBLE
            }
        }

    }
}