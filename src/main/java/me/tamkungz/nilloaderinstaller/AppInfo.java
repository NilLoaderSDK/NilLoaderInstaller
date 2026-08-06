package me.tamkungz.nilloaderinstaller;

public final class AppInfo {
    public static final String APP_NAME = "NilLoader Installer";
    public static final String APP_VERSION = "0.1.0";
    public static final String NILLOADER_VERSION = "1.3.6";
    public static final String NILLOADER_MAVEN_BASE = "https://repo.sleeping.town";
    public static final String NILLOADER_COORDINATE = "com.unascribed:nilloader:" + NILLOADER_VERSION;
    public static final String NILLOADER_JAR_URL = NILLOADER_MAVEN_BASE
            + "/com/unascribed/nilloader/" + NILLOADER_VERSION
            + "/nilloader-" + NILLOADER_VERSION + ".jar";

    private AppInfo() {}
}
