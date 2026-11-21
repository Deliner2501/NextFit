package com.nextfit.service.alert;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsAppLinkTest {

    private final WhatsAppLink link = new WhatsAppLink();

    @Test
    void aLocalNumberGetsTheCountryCode() {
        assertEquals(Optional.of("https://wa.me/573001234567?text=Hola"), link.forMessage("3001234567", "Hola"));
    }

    @Test
    void spacesDashesAndPlusSignAreIgnored() {
        assertEquals(Optional.of("https://wa.me/573001234567?text=Hola"),
                link.forMessage("+57 300-123 4567", "Hola"));
        assertEquals(Optional.of("https://wa.me/573001234567?text=Hola"),
                link.forMessage("0057 3001234567", "Hola"));
    }

    @Test
    void theTextIsEncodedWithPercentSigns() {
        String url = link.forMessage("3001234567", "¡Hola Ana! ¿Qué tal?").orElseThrow();

        assertTrue(url.endsWith("?text=%C2%A1Hola%20Ana%21%20%C2%BFQu%C3%A9%20tal%3F"));
        assertFalse(url.contains("+"));
        assertFalse(url.contains(" "));
    }

    @Test
    void invalidNumbersGiveNoLink() {
        assertTrue(link.forMessage("12345", "Hola").isEmpty());
        assertTrue(link.forMessage("", "Hola").isEmpty());
        assertTrue(link.forMessage(null, "Hola").isEmpty());
        assertTrue(link.forMessage("abc", "Hola").isEmpty());
    }

    @Test
    void anotherCountryCodeCanBeUsed() {
        WhatsAppLink mexico = new WhatsAppLink("52");

        assertEquals(Optional.of("https://wa.me/525512345678?text=Hola"), mexico.forMessage("5512345678", "Hola"));
    }
}
