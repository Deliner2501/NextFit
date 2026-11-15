package com.nextfit.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessPageTest {

    @Test
    void pingPageTellsThePhoneTheConnectionWorks() {
        String html = AccessPage.ping();

        assertTrue(html.contains("NextFit responde"));
        assertTrue(html.contains("carnet"));
    }

    @Test
    void notFoundPageIsAPlainMessage() {
        String html = AccessPage.notFound();

        assertTrue(html.contains("<html"));
        assertTrue(!html.contains("ENTRADA REGISTRADA"));
    }
}
