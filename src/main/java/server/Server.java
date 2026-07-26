package server;

import server.auth.AuthDatabase;
import server.client.Client;
import server.client.ClientHandler;
import server.client.TimeoutHandler;
import server.commands.*;
import server.level.Level;

import java.util.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    /** Directory server.properties/banned_ips.json/auth.sqlite/chunks live under. "" = working directory. */
    private static Path dataDir = Paths.get("");

    private static Path PROPERTIES_PATH = Paths.get("server.properties");
    public static Path BANNED_PATH = Paths.get("banned_ips.json");
    private static Path AUTH_DB_PATH  = Paths.get("auth.sqlite");

    public static int PORT         = 9090;
    public static int PLAYER_LIMIT = 50;
    public static int MAX_PER_IP   = 3;

    public static boolean ANTICHEAT      = true;
    public static double  MAX_REACH      = 10.0;
    public static double  MOVE_RATE      = 20.0;   // moves / sec
    public static double  MOVE_BURST     = 10.0;
    public static double  PLACE_RATE     = 5.0;    // places / sec
    public static double  BREAK_RATE     = 5.0;    // breaks / sec
    public static int     RENDER_DISTANCE = 8;
    /** Vertical render distance, in cubic chunks (each 16 tall). */
    public static int     VERTICAL_RENDER_DISTANCE = 4;
    public static double  VOID_Y         = -64.0;
    public static boolean LOGS = true;

    public static Level level;
    public static AuthDatabase authDb;

    public static final Set<Client>                   clients       = ConcurrentHashMap.newKeySet();
    public static final ConcurrentHashMap<Client,Long> lastKeepAlive = new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<String, byte[]> skins = new ConcurrentHashMap<>();

    public static volatile boolean running = false;

    private static ServerSocket serverSocket;
    private static Thread acceptThread;
    private static Thread consoleThread;
    private static Thread timeoutThread;
    private static Thread timeBroadcastThread;
    private static Thread serverPingerThread;
    private static Thread pingBroadcastThread;
    private static Thread shutdownHook;

    public static void main(String[] args) throws IOException {
        start(Paths.get(""), null, null, true);
        try {
            acceptThread.join();
        } catch (InterruptedException ignored) {}
    }

    /**
     * Starts the server. This is the same startup logic used for a dedicated
     * server (via main()) and for an embedded singleplayer server; the wire
     * protocol and ClientHandler logic are untouched either way, so this
     * never affects compatibility with other rd-multiplayer clients/servers.
     *
     * @param dir           directory server.properties/banned_ips.json/auth.sqlite/chunks live under
     * @param portOverride  if non-null, overrides the port from server.properties (0 = pick any free port)
     * @param bindAddress   if non-null, binds only to this address (e.g. loopback for singleplayer);
     *                      if null, binds all interfaces, matching prior dedicated-server behavior
     * @param withConsole   whether to read admin commands from stdin (dedicated server only)
     */
    public static synchronized void start(Path dir, Integer portOverride, InetAddress bindAddress, boolean withConsole) throws IOException {
        if (running) throw new IllegalStateException("Server is already running");

        dataDir = dir != null ? dir : Paths.get("");
        PROPERTIES_PATH = dataDir.resolve("server.properties");
        BANNED_PATH      = dataDir.resolve("banned_ips.json");
        AUTH_DB_PATH     = dataDir.resolve("auth.sqlite");

        if (!dataDir.toString().isEmpty()) Files.createDirectories(dataDir);

        loadProperties();
        if (portOverride != null) PORT = portOverride;

        authDb = new AuthDatabase(AUTH_DB_PATH.toString());
        System.out.println("Auth database opened at " + AUTH_DB_PATH);

        level = new Level(dataDir.resolve("chunks"));

        clients.clear();
        lastKeepAlive.clear();
        skins.clear();

        shutdownHook = new Thread(() -> {
            System.out.println("Saving all chunks...");
            level.save();
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        serverSocket = bindAddress != null
                ? new ServerSocket(PORT, 50, bindAddress)
                : new ServerSocket(PORT);
        PORT = serverSocket.getLocalPort();
        System.out.println("Server started on port " + PORT);

        timeoutThread       = TimeoutHandler.start();
        timeBroadcastThread = server.net.TimeBroadcaster.start();
        serverPingerThread  = server.net.ServerPinger.start();
        pingBroadcastThread = server.net.PingBroadcaster.start();

        running = true;

        if (withConsole) {
            CommandManager commandManager = new CommandManager();
            commandManager.register(new KickCommand());
            commandManager.register(new BanCommand());
            commandManager.register(new SayCommand());
            commandManager.register(new TeleportCommand());
            commandManager.register(new ListCommand());

            Scanner scanner = new Scanner(System.in);

            consoleThread = new Thread(() -> {
                while (running && scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    commandManager.execute(line);
                }
            }, "Server-Console");
            consoleThread.setDaemon(true);
            consoleThread.start();
        }

        final ServerSocket boundSocket = serverSocket;
        acceptThread = new Thread(() -> {
            while (running) {
                try {
                    Socket clientSocket = boundSocket.accept();
                    System.out.println("Client connected from: "
                            + clientSocket.getInetAddress().getHostAddress());
                    new Thread(() -> ClientHandler.handle(clientSocket)).start();
                } catch (IOException e) {
                    if (running) e.printStackTrace();
                    // else: socket was closed by stop() -- expected, loop exits below
                }
            }
        }, "Server-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /** Stops the server: closes all connections, saves the world, and shuts down background threads. */
    public static synchronized void stop() {
        if (!running) return;
        running = false;

        for (Client c : clients) {
            try { c.getSocket().close(); } catch (IOException ignored) {}
        }
        clients.clear();
        lastKeepAlive.clear();
        skins.clear();

        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}

        if (acceptThread != null) acceptThread.interrupt();
        if (consoleThread != null) consoleThread.interrupt();
        if (timeoutThread != null) timeoutThread.interrupt();
        if (timeBroadcastThread != null) timeBroadcastThread.interrupt();
        if (serverPingerThread != null) serverPingerThread.interrupt();
        if (pingBroadcastThread != null) pingBroadcastThread.interrupt();

        if (level != null) {
            level.save();
            level = null;
        }

        if (shutdownHook != null) {
            try { Runtime.getRuntime().removeShutdownHook(shutdownHook); } catch (IllegalStateException ignored) {}
            shutdownHook = null;
        }

        serverSocket = null;
        acceptThread = null;
        consoleThread = null;
        timeoutThread = null;
        timeBroadcastThread = null;
        serverPingerThread = null;
        pingBroadcastThread = null;

        System.out.println("Server stopped.");
    }

    public static Optional<Client> getClient(String username) {
        return clients.stream()
                .filter(c -> c.getUsername().equals(username))
                .findFirst();
    }

    private static void loadProperties() {
        try {
            if (!Files.exists(PROPERTIES_PATH)) createDefaultProperties();
            if (!Files.exists(BANNED_PATH)) createBannedJSON();

            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(PROPERTIES_PATH)) { p.load(in); }

            PORT         = Integer.parseInt(p.getProperty("port",         "9090"));
            PLAYER_LIMIT = Integer.parseInt(p.getProperty("player_limit", "50"));
            MAX_PER_IP   = Integer.parseInt(p.getProperty("max_per_ip",   "3"));

            ANTICHEAT                = Boolean.parseBoolean(p.getProperty("anticheat",       "true"));
            MAX_REACH                = Double.parseDouble (p.getProperty("max_reach",        "10.0"));
            MOVE_RATE                = Double.parseDouble (p.getProperty("move_rate",        "20.0"));
            MOVE_BURST               = Double.parseDouble (p.getProperty("move_burst",       "10.0"));
            PLACE_RATE               = Double.parseDouble (p.getProperty("place_rate",       "5.0"));
            BREAK_RATE               = Double.parseDouble (p.getProperty("break_rate",       "5.0"));
            RENDER_DISTANCE          = Integer.parseInt   (p.getProperty("render_distance",  "8"));
            VERTICAL_RENDER_DISTANCE = Integer.parseInt   (p.getProperty("vertical_render_distance", "4"));
            VOID_Y                   = Double.parseDouble (p.getProperty("void_y",           "-64.0"));
            LOGS                     = Boolean.parseBoolean(p.getProperty("logs", "true"));

            System.out.println("Loaded server.properties");
        } catch (Exception e) {
            System.err.println("Failed to load server.properties");
            e.printStackTrace();
        }
    }

    private static void createDefaultProperties() throws IOException {
        Properties d = new Properties();
        d.setProperty("port",                      "9090");
        d.setProperty("player_limit",              "50");
        d.setProperty("max_per_ip",                "3");
        d.setProperty("anticheat",                 "true");
        d.setProperty("max_reach",                 "10.0");
        d.setProperty("move_rate",                 "20.0");
        d.setProperty("move_burst",                "10.0");
        d.setProperty("place_rate",                "5.0");
        d.setProperty("break_rate",                "5.0");
        d.setProperty("render_distance",           "8");
        d.setProperty("vertical_render_distance",  "4");
        d.setProperty("void_y",                    "-64.0");
        try (OutputStream out = Files.newOutputStream(PROPERTIES_PATH)) {
            d.store(out, "Server Properties");
        }
        System.out.println("Created default server.properties");
    }

    private static void createBannedJSON() throws IOException {
        try (FileWriter writer = new FileWriter(BANNED_PATH.toFile())) {
            writer.write("[]");
        }
    }
}