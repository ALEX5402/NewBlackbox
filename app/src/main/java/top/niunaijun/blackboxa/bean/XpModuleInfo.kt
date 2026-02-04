package top.niunaijun.blackboxa.bean

import android.graphics.drawable.Drawable


data class XpModuleInfo(
        val name: String,
        val desc: String,
        val packageName: String,
        val version: String,
        var enable:Boolean,
        val icon: Drawable
)
