package com.vspace.view.xp

import android.view.View
import android.view.ViewGroup
import cbfg.rvadapter.RVHolder
import cbfg.rvadapter.RVHolderFactory
import com.vspace.R
import com.vspace.bean.XpModuleInfo
import com.vspace.databinding.ItemXpBinding

class XpAdapter : RVHolderFactory() {
    override fun createViewHolder(parent: ViewGroup?, viewType: Int, item: Any): RVHolder<out Any> {
        return XpVH(inflate(R.layout.item_xp, parent))
    }

    class XpVH(itemView: View) : RVHolder<XpModuleInfo>(itemView) {
        private val binding = ItemXpBinding.bind(itemView)

        override fun setContent(item: XpModuleInfo, isSelected: Boolean, payload: Any?) {
            binding.icon.setImageDrawable(item.icon)
            binding.name.text = item.name
            binding.desc.text = item.desc
            binding.enable.isChecked = item.enable
            binding.enable.setOnCheckedChangeListener { buttonView, _ ->
                if (buttonView.isPressed) {
                    binding.root.performClick()
                }
            }
        }
    }
}
