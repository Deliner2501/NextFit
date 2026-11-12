package com.nextfit.web;

import com.nextfit.model.Member;
import com.nextfit.service.AccessResult;
import com.nextfit.service.PhoneAccess;
import com.nextfit.service.portal.MemberPortal;
import com.nextfit.service.portal.PortalSummary;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Small web server of the reception computer, built into the JDK. A phone that
 * scans the QR of a carnet opens {@code /a/CODE}; the server registers the entry
 * or the exit and answers with {@link AccessPage}. It only serves the local
 * network and has these routes: {@code /a/CODE}, {@code /m/CODE} (the portal of the member, only
 * when a {@link MemberPortal} was given), {@code /photo/CODE} and {@code /ping} (to check the
 * connection from a phone).
 */
public final class AccessServer {

    private static final int PORT_TRIES = 10;
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    private final PhoneAccess phone;
    private final MemberPortal portal;
    private final int preferredPort;
    private HttpServer server;
    private ExecutorService executor;

    /**
     * Creates the server without starting it.
     *
     * @param phone         the service that registers the scans
     * @param preferredPort the port to try first; 0 lets the system choose a free one
     */
    public AccessServer(PhoneAccess phone, int preferredPort) {
        this(phone, null, preferredPort);
    }

    /**
     * Creates the server with the portal of the member, without starting it.
     *
     * @param phone         the service that registers the scans
     * @param portal        the service that gathers what the member sees; null leaves the portal off
     * @param preferredPort the port to try first; 0 lets the system choose a free one
     */
    public AccessServer(PhoneAccess phone, MemberPortal portal, int preferredPort) {
        this.phone = phone;
        this.portal = portal;
        this.preferredPort = preferredPort;
    }

    /**
     * Starts listening. When the preferred port is busy it tries the next ones.
     *
     * @throws IOException when no port could be opened
     */
    public synchronized void start() throws IOException {
        if (server != null) {
            return;
        }
        IOException last = null;
        for (int i = 0; i < PORT_TRIES; i++) {
            int port = preferredPort == 0 ? 0 : preferredPort + i;
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                break;
            } catch (IOException e) {
                last = e;
            }
        }
        if (server == null) {
            throw last == null ? new IOException("No se pudo abrir el puerto") : last;
        }
        executor = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "nextfit-access-http");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
    }

    /** Stops the server, if it is running. */
    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            executor.shutdownNow();
            server = null;
        }
    }

    /**
     * Returns the port the server listens on.
     *
     * @return the port, or -1 when it is stopped
     */
    public synchronized int port() {
        return server == null ? -1 : server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (!"GET".equals(exchange.getRequestMethod())) {
                send(exchange, 405, "text/plain; charset=utf-8", "Método no permitido".getBytes(StandardCharsets.UTF_8));
            } else if (path.equals("/") || path.equals("/ping")) {
                sendHtml(exchange, 200, AccessPage.ping());
            } else if (path.startsWith("/a/")) {
                String token = path.substring(3);
                AccessResult result = phone.scan(token);
                sendHtml(exchange, 200, AccessPage.render(result, token));
            } else if (path.startsWith("/m/") && portal != null) {
                sendPortal(exchange, path.substring(3));
            } else if (path.equals("/logo.png")) {
                sendLogo(exchange);
            } else if (path.startsWith("/photo/")) {
                sendPhoto(exchange, path.substring(7));
            } else {
                sendHtml(exchange, 404, AccessPage.notFound());
            }
        } catch (RuntimeException e) {
            sendHtml(exchange, 500, AccessPage.notFound());
        } finally {
            exchange.close();
        }
    }

    private void sendPortal(HttpExchange exchange, String token) throws IOException {
        Optional<PortalSummary> summary = portal.summaryFor(token);
        if (summary.isEmpty()) {
            sendHtml(exchange, 404, PortalPage.invalid());
            return;
        }
        sendHtml(exchange, 200, PortalPage.render(summary.get(), token));
    }

    private void sendLogo(HttpExchange exchange) throws IOException {
        try (java.io.InputStream in = AccessServer.class.getResourceAsStream("/images/logo/nextfit-logo-128.png")) {
            if (in == null) {
                send(exchange, 404, "text/plain; charset=utf-8", new byte[0]);
                return;
            }
            send(exchange, 200, "image/png", in.readAllBytes());
        }
    }

    private void sendPhoto(HttpExchange exchange, String token) throws IOException {
        Optional<Member> member = phone.findMember(token);
        Path file = member.map(Member::getPhotoPath).filter(p -> !p.isBlank()).map(Path::of).orElse(null);
        if (file == null || !Files.isRegularFile(file) || Files.size(file) > MAX_PHOTO_BYTES) {
            send(exchange, 404, "text/plain; charset=utf-8", new byte[0]);
            return;
        }
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        String type = name.endsWith(".png") ? "image/png" : "image/jpeg";
        send(exchange, 200, type, Files.readAllBytes(file));
    }

    private static void sendHtml(HttpExchange exchange, int status, String html) throws IOException {
        send(exchange, status, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        // A scan must never be answered from the cache of the phone.
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
    }
}
