package top.niunaijun.blackboxa.view.xp

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import cbfg.rvadapter.RVHolder
import cbfg.rvadapter.RVHolderFactory
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.XpModuleInfo
import top.niunaijun.blackboxa.databinding.ItemXpBinding

class XpAdapter : RVHolderFactory() {

    override fun createViewHolder(parent: ViewGroup?, viewType: Int, item: Any): RVHolder<out Any> {
        return XpVH(inflate(R.layout.item_xp, parent))
    }

    class XpVH(itemView: View) : RVHolder<XpModuleInfo>(itemView) {
        val binding = ItemXpBinding.bind(itemView)

        override fun setContent(item: XpModuleInfo, isSelected: Boolean, payload: Any?) {
            binding.icon.setImageDrawable(item.icon ?: ColorDrawable(Color.GRAY))
            binding.name.text = item.name
            binding.desc.text = item.desc
            binding.enable.setOnCheckedChangeListener(null)
            binding.enable.isChecked = item.enable
            binding.enable.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) {
                    item.enable = isChecked
                    binding.root.performClick()
                }
            }
        }
    }
}
