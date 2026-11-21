package com.nextfit.service.mail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/** Loads local email settings with environment overrides for individual runs. */
record EmailSettings(String mode, String account, String password, String testRecipient) {

    static final String FILE_NAME = "nextfit-email.properties";

    /**
     * Reads the optional local file and applies explicit environment overrides.
     *
     * @param file the workstation's local settings file
     * @param environment the environment of the running process
     * @return the settings used for this run
     * @throws IOException if the existing settings file cannot be read
     */
    static EmailSettings load(Path file, Map<String, String> environment) throws IOException {
        Properties properties = new Properties();
        if (Files.exists(file)) {
            try (var reader = Files.newBufferedReader(file)) {
                properties.load(reader);
            }
        }
        return new EmailSettings(
                value(environment, properties, "MODE", "mode"),
                value(environment, properties, "ACCOUNT", "account"),
                value(environment, properties, "PASSWORD", "password"),
                value(environment, properties, "TO", "testRecipient"));
    }

    private static String value(Map<String, String> environment, Properties properties, String suffix, String key) {
        return environment.getOrDefault("NEXTFIT_EMAIL_" + suffix, properties.getProperty("email." + key, "")).strip();
    }

    /** Prevents accidental disclosure when settings are printed for diagnostics. */
    @Override
    public String toString() {
        return "EmailSettings[mode=" + mode + "]";
    }
}
