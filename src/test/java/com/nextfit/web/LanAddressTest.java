package com.nextfit.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanAddressTest {

    @Test
    void theAddressIsEitherAPrivateIpv4OrLocalhost() {
        String address = LanAddress.find();

        assertFalse(address.isBlank());
        boolean privateIp = address.startsWith("192.168.") || address.startsWith("10.")
                || address.matches("172\\.(1[6-9]|2\\d|3[01])\\..*");
        assertTrue(privateIp || address.equals("localhost"), "unexpected address: " + address);
    }

    @Test
    void asksTwiceAndGetsTheSameAnswer() {
        assertEquals(LanAddress.find(), LanAddress.find());
    }
}
