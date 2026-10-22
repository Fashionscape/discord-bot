package dev.salmonllama.fsbot.utilities;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {
    public static final String CONFIG_NAME = "bot.config";

    public static final Path BOT_FOLDER = Paths.get(System.getProperty("user.home"), ".fsbot");

    public static final String DB_NAME = "fsbot";

    public static final String OUTFIT_TABLE = "outfits";

    public static final String SCONF_TABLE = "server_conf";
}
