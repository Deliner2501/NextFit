package com.nextfit.service;

import java.util.Optional;

/**
 * The text stored in the QR code of a member's carnet. It carries only the
 * document number behind a fixed prefix, so the code never exposes other
 * personal data and a scanner can tell a NextFit carnet from any other QR.
 */
public final class CarnetCode {

    /** Prefix that marks a NextFit carnet. */
    public static final String PREFIX = "NF:";

    private CarnetCode() {
    }

    /**
     * Builds the QR payload of a member.
     *
     * @param documentNumber the document number of the member
     * @return the text to encode in the QR
     */
    public static String encode(String documentNumber) {
        return PREFIX + documentNumber.trim();
    }

    /**
     * Reads a scanned text and returns the document number it carries.
     * A plain document number (typed by hand) is also accepted.
     *
     * @param scanned the text read by the scanner or typed by the receptionist
     * @return the document number, or empty when the text is blank
     */
    public static Optional<String> decode(String scanned) {
        if (scanned == null) {
            return Optional.empty();
        }
        String text = scanned.trim();
        if (text.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            text = text.substring(PREFIX.length()).trim();
        }
        return text.isEmpty() ? Optional.empty() : Optional.of(text);
    }
}
