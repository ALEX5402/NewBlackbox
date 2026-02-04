package top.niunaijun.blackboxa.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import top.niunaijun.blackboxa.app.App


var toastImpl:Toast? = null

fun Context.toast(msg:String){
    toastImpl?.cancel()
    toastImpl = Toast.makeText(this,msg,Toast.LENGTH_SHORT)
    toastImpl?.show()
}

fun toast(msg: String){
    App.getContext().toast(msg)
}

fun toast(@StringRes msgID:Int){
    toast(getString(msgID))
}