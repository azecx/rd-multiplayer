package client.singleplayer;

import client.Minecraft;
import client.gui.screen.impl.ServerSelectScreen;
import server.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public final class Singleplayer {

    private static final Path SAVES_DIR = Paths.get("saves");

    private static volatile boolean active = false;
    private static String currentWorldName;

    private Singleplayer() {}

    public static boolean isActive() {
        return active;
    }

    public static String currentWorldName() {
        return currentWorldName;
    }

    public static String sanitize(String worldName) {
        String safe = worldName == null ? "" : worldName.trim().replaceAll("[^a-zA-Z0-9 _-]", "").trim();
        if (safe.isEmpty()) safe = "World";
        return safe;
    }

    public static Path worldDir(String worldName) {
        return SAVES_DIR.resolve(sanitize(worldName));
    }

    public static synchronized void start(String worldName) {
        if (active) {
            System.err.println("A singleplayer world is already running; stop it first.");
            return;
        }

        String Name = sanitize(worldName);
        Path dir = worldDir(Name);

        try {
            Server.start(dir, 0, InetAddress.getLoopbackAddress(), false, false);
        } catch (IOException e) {
            System.err.println("Failed to start embedded server for world \"" + Name + "\"");
            e.printStackTrace();
            return;
        }

        active = true;
        currentWorldName = Name;

        String username = Preferences.userNodeForPackage(ServerSelectScreen.class).get("username", "Player");
        Minecraft.mc.connect("127.0.0.1", Server.PORT, username);
    }

    public static synchronized void stop() {
        if (!active) return;
        active = false;
        currentWorldName = null;
        Server.stop();
    }
}