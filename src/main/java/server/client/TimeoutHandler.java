package server.client;
import server.Server;

import java.io.IOException;

public class TimeoutHandler {

    private static final long TIMEOUT_MS = 10_000;

    public static Thread start() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                long now = System.currentTimeMillis();

                for (Client client : Server.clients) {
                    Long last = Server.lastKeepAlive.get(client);
                    if (last == null) continue;

                    if (now - last > TIMEOUT_MS) {
                        System.out.println("Client timed out: " + client.getUsername());
                        try { client.getSocket().close(); } catch (IOException ignored) {}
                        Server.clients.remove(client);
                        Server.lastKeepAlive.remove(client);
                    }
                }

                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "TimeoutHandler");

        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}