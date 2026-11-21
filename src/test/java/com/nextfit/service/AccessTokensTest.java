package com.nextfit.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessTokensTest {

    private static byte[] key(int fill) {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) fill);
        return key;
    }

    @Test
    void tokenIsStableAndHasTheExpectedShape() {
        AccessTokens tokens = new AccessTokens(key(7));

        String first = tokens.tokenFor("1065123456");

        assertEquals(first, tokens.tokenFor("1065123456"));
        assertEquals(AccessTokens.LENGTH, first.length());
        assertTrue(first.matches("[A-HJ-NP-Z2-9]{10}"));
        assertFalse(first.contains("1065123456"));
    }

    @Test
    void differentDocumentsOrKeysGiveDifferentTokens() {
        AccessTokens a = new AccessTokens(key(7));
        AccessTokens b = new AccessTokens(key(9));

        assertFalse(a.tokenFor("1065123456").equals(a.tokenFor("1065123457")));
        assertFalse(a.tokenFor("1065123456").equals(b.tokenFor("1065123456")));
    }

    @Test
    void matchesAcceptsTheRightCodeEvenInLowerCaseAndRejectsOthers() {
        AccessTokens tokens = new AccessTokens(key(7));
        String token = tokens.tokenFor("1065123456");

        assertTrue(tokens.matches("1065123456", token));
        assertTrue(tokens.matches("1065123456", " " + token.toLowerCase() + " "));
        assertFalse(tokens.matches("1065123457", token));
        assertFalse(tokens.matches("1065123456", "XXXXXXXXXX"));
        assertFalse(tokens.matches("1065123456", null));
        assertFalse(tokens.matches("1065123456", "ABC"));
    }

    @Test
    void shortKeysAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AccessTokens(new byte[8]));
    }
}
