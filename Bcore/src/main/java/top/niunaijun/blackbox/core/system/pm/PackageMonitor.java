package top.niunaijun.blackbox.core.system.pm;

/**
 * updated by alex5402 on 5/2/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK (USDT TRC20)
 */
public interface PackageMonitor {
    void onPackageUninstalled(String packageName, boolean isRemove, int userId);

    void onPackageInstalled(String packageName, int userId);
}
