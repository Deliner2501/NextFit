package com.nextfit.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Access codes of the carnets. The code is a short signature (HMAC) of the
 * document number with a secret key, so it can be checked without storing it
 * anywhere and it does not reveal the document. Changing the key invalidates
 * every carnet printed before.
 */
public final class AccessTokens {

    /** Length of a code, in characters. */
    public static final int LENGTH = 10;

    // 32 letters and digits without the easily confused 0, 1, I and O.
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final byte[] secret;

    /**
     * Creates the generator.
     *
     * @param secret the secret key, at least 16 bytes
     * @throws IllegalArgumentException when the key is too short
     */
    public AccessTokens(byte[] secret) {
        if (secret == null || secret.length < 16) {
            throw new IllegalArgumentException("La clave de acceso debe tener al menos 16 bytes");
        }
        this.secret = secret.clone();
    }

    /**
     * Returns the access code of a member.
     *
     * @param documentNumber the document number of the member
     * @return the code, {@value #LENGTH} characters long
     */
    public String tokenFor(String documentNumber) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] hash = mac.doFinal(("nextfit-carnet:" + documentNumber.trim()).getBytes(StandardCharsets.UTF_8));
            long bits = 0;
            for (int i = 0; i < 6; i++) {
                bits = (bits << 8) | (hash[i] & 0xFF);
            }
            StringBuilder code = new StringBuilder(LENGTH);
            for (int i = LENGTH - 1; i >= 0; i--) {
                code.append(ALPHABET.charAt((int) ((bits >> (5 * i)) & 31)));
            }
            return code.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el código de acceso", e);
        }
    }

    /**
     * Tells whether a received text is the code of the given document.
     *
     * @param documentNumber the document number to check
     * @param received       the text received from a scan
     * @return true when they match
     */
    public boolean matches(String documentNumber, String received) {
        Optional<String> clean = normalize(received);
        return clean.isPresent() && MessageDigest.isEqual(
                tokenFor(documentNumber).getBytes(StandardCharsets.UTF_8),
                clean.get().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Cleans a received code (spaces, case).
     *
     * @param received the text from a scan
     * @return the code in capitals, or empty when it cannot be a code
     */
    public static Optional<String> normalize(String received) {
        if (received == null) {
            return Optional.empty();
        }
        String text = received.trim().toUpperCase();
        return text.length() == LENGTH ? Optional.of(text) : Optional.empty();
    }
}
