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
            System.err.println("A world is already running, please stop it");
            return;
        }

        String Name = sanitize(worldName);
        Path dir = worldDir(Name);

        try {
            Server.start(dir, 0, InetAddress.getLoopbackAddress(), false, false);
        } catch (IOException e) {
            System.err.println("Failed to start the server for world \"" + Name + "\"");
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

    public static java.util.List<String> listWorlds() {
        java.util.List<String> names = new java.util.ArrayList<>();
        java.io.File[] children = SAVES_DIR.toFile().listFiles();
        if (children != null) {
            for (java.io.File f : children) {
                if (f.isDirectory()) names.add(f.getName());
            }
            names.sort(String.CASE_INSENSITIVE_ORDER);
        }
        return names;
    }

    public static synchronized boolean renameWorld(String oldName, String newName) {
        String OldName = sanitize(oldName);
        String NewName = sanitize(newName);
        if (NewName.equals(OldName)) return true;

        java.nio.file.Path from = worldDir(OldName);
        java.nio.file.Path to = worldDir(NewName);

        if (!java.nio.file.Files.isDirectory(from) || java.nio.file.Files.exists(to)) return false;

        try {
            java.nio.file.Files.move(from, to);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to rename world \"" + OldName + "\" to \"" + NewName + "\"");
            e.printStackTrace();
            return false;
        }
    }

    public static synchronized boolean deleteWorld(String worldName) {
        java.nio.file.Path dir = worldDir(worldName);
        if (!java.nio.file.Files.isDirectory(dir)) return false;

        try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        java.nio.file.Files.delete(p);
                    } catch (IOException ignored) {}
                });
            return true;
        } catch (IOException e) {
            System.err.println("Failed to delete world \"" + worldName + "\"");
            e.printStackTrace();
            return false;
        }
    }
}
