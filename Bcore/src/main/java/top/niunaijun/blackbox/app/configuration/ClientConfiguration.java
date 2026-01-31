package top.niunaijun.blackbox.app.configuration;

import java.io.File;

/**
 * updated by alex5402 on 5/4/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 
 */
public abstract class ClientConfiguration {

    public boolean isHideRoot() {
        return false;
    }



    public abstract String getHostPackageName();

    public boolean isEnableDaemonService() {
        return true;
    }

    public boolean isEnableLauncherActivity() {
        return true;
    }

    /**
     * Whether to use VPN network mode for sandboxed apps.
     * When enabled, network traffic is routed through the VPN service.
     * Default is false (normal network mode).
     */
    public boolean isUseVpnNetwork() {
        return false;
    }

    /**
     * This method is called when an internal application requests to install a new application.
     *
     * @return Is it handled?
     */
    public boolean requestInstallPackage(File file, int userId) {
        return false;
    }
}
