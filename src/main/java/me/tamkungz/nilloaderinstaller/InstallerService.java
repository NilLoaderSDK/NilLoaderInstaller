package me.tamkungz.nilloaderinstaller;

import me.tamkungz.nilloaderinstaller.install.OfficialLauncherInstaller;
import me.tamkungz.nilloaderinstaller.install.PrismInstaller;
import me.tamkungz.nilloaderinstaller.model.InstallResult;
import me.tamkungz.nilloaderinstaller.model.InstallTarget;
import me.tamkungz.nilloaderinstaller.model.LauncherType;

public final class InstallerService {
    private InstallerService() {}

    public static InstallResult install(InstallTarget target) throws Exception {
        if (target.launcherType() == LauncherType.OFFICIAL) {
            return OfficialLauncherInstaller.install(target);
        }
        return PrismInstaller.install(target);
    }
}
