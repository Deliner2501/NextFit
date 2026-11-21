package com.nextfit.service.alert;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Builds the link that opens a WhatsApp chat with a message already written. It
 * only formats a phone number and a text; it does not send anything and it does
 * not know members or alerts, so any screen can reuse it.
 */
public class WhatsAppLink {

    private static final int LOCAL_NUMBER_LENGTH = 10;
    private static final int MIN_INTERNATIONAL_LENGTH = 11;
    private static final int MAX_INTERNATIONAL_LENGTH = 15;

    private final String countryCode;

    /** Creates the builder for Colombian numbers (country code 57). */
    public WhatsAppLink() {
        this("57");
    }

    /**
     * Creates the builder.
     *
     * @param countryCode the country code added to the numbers written without it
     */
    public WhatsAppLink(String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * Builds the link of a chat with a message.
     *
     * @param phone the phone number as the member gave it; spaces, dashes and a plus sign are accepted
     * @param text  the message to write in the chat
     * @return the link, or empty when the phone is not a valid number
     */
    public Optional<String> forMessage(String phone, String text) {
        return normalize(phone).map(number -> "https://wa.me/" + number
                + "?text=" + URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20"));
    }

    private Optional<String> normalize(String phone) {
        if (phone == null) {
            return Optional.empty();
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        if (digits.length() == LOCAL_NUMBER_LENGTH) {
            return Optional.of(countryCode + digits);
        }
        boolean international = digits.startsWith(countryCode)
                && digits.length() >= MIN_INTERNATIONAL_LENGTH
                && digits.length() <= MAX_INTERNATIONAL_LENGTH;
        return international ? Optional.of(digits) : Optional.empty();
    }
}
