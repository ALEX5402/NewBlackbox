package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.util.Log;

import java.lang.reflect.Method;

import black.android.net.wifi.BRIWifiManagerStub;
import black.android.net.wifi.BRWifiInfo;
import black.android.net.wifi.BRWifiSsid;
import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;


public class IWifiManagerProxy extends BinderInvocationStub {
    public static final String TAG = "IWifiManagerProxy";

    public IWifiManagerProxy() {
        super(BRServiceManager.get().getService(Context.WIFI_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRIWifiManagerStub.get().asInterface(BRServiceManager.get().getService(Context.WIFI_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getConnectionInfo")
    public static class GetConnectionInfo extends MethodHook {
        
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            WifiInfo wifiInfo = (WifiInfo) method.invoke(who, args);
            BRWifiInfo.get(wifiInfo)._set_mBSSID("ac:62:5a:82:65:c4");
            BRWifiInfo.get(wifiInfo)._set_mMacAddress("ac:62:5a:82:65:c4");
            BRWifiInfo.get(wifiInfo)._set_mWifiSsid(BRWifiSsid.get().createFromAsciiEncoded("BlackBox_Wifi"));
            return wifiInfo;
        }

        public static String intIP2StringIP(int ip) {
            return (ip & 0xFF) + "." +
                    ((ip >> 8) & 0xFF) + "." +
                    ((ip >> 16) & 0xFF) + "." +
                    (ip >> 24 & 0xFF);
        }

        public static int ip2Int(String ipString) {
            
            String[] ipSlices = ipString.split("\\.");
            int rs = 0;
            for (int i = 0; i < ipSlices.length; i++) {
                
                int intSlice = Integer.parseInt(ipSlices[i]) << 8 * i;
                
                rs = rs | intSlice;
            }
            return rs;
        }
    }
}
