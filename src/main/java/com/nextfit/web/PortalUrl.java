package com.nextfit.web;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/** Reads the optional public HTTPS address used on member cards. */
public final class PortalUrl {

    private static final String KEY = "portal.publicUrl";

    private PortalUrl() {
    }

    /**
     * @param settings the local NextFit settings file
     * @return the public HTTPS origin, or empty for the desktop LAN address
     * @throws IllegalArgumentException when the configured address is invalid
     * @throws IllegalStateException when the settings file cannot be read
     */
    public static Optional<String> load(Path settings) {
        return publicOrigin(readSettings(settings));
    }

    /**
     * @param settings the local NextFit settings file
     * @return whether the desktop should use the independent LAN server
     */
    public static boolean usesStandaloneLan(Path settings) {
        return Boolean.parseBoolean(readSettings(settings).getProperty("portal.standaloneLan", "false").strip());
    }

    /**
     * @param settings the local NextFit settings file
     * @param defaultPort the port used when none is configured
     * @return the fixed port of the independent LAN server
     */
    public static int standaloneLanPort(Path settings, int defaultPort) {
        String value = readSettings(settings).getProperty("portal.lanPort", Integer.toString(defaultPort)).strip();
        try {
            int port = Integer.parseInt(value);
            if (port >= 1 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException ignored) {
            // Report the same setting error for nonnumeric and out-of-range values.
        }
        throw new IllegalArgumentException("portal.lanPort debe ser un puerto entre 1 y 65535.");
    }

    private static Properties readSettings(Path settings) {
        Properties properties = new Properties();
        if (Files.notExists(settings)) {
            return properties;
        }
        try (Reader reader = Files.newBufferedReader(settings, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException failure) {
            throw new IllegalStateException("No se pudo leer la configuración del portal.", failure);
        }
        return properties;
    }

    private static Optional<String> publicOrigin(Properties properties) {
        String value = properties.getProperty(KEY, "").strip();
        if (value.isEmpty()) {
            return Optional.empty();
        }
        URI uri = URI.create(value);
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null
                || uri.getPort() == 0 || uri.getPort() > 65535
                || !(uri.getRawPath().isEmpty() || "/".equals(uri.getRawPath()))) {
            throw new IllegalArgumentException(KEY + " debe ser una dirección HTTPS sin ruta ni parámetros.");
        }
        return Optional.of("https://" + uri.getRawAuthority());
    }
}
