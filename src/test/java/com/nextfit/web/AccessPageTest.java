package com.nextfit.web;

import com.nextfit.model.Member;
import com.nextfit.service.AccessResult;
import com.nextfit.service.AccessResult.Outcome;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessPageTest {

    private static Member member(String name, String photo) {
        return new Member(1, name, "1065123456", "3001112233", "a@b.co", LocalDate.of(1998, 5, 1),
                LocalDate.of(2026, 1, 1), photo, "Mamá 3001", true);
    }

    @Test
    void entryPageIsGreenAndShowsTheMemberAndTheMembership() {
        AccessResult result = new AccessResult(Outcome.ENTRY, member("Juan Pablo Cuello", ""),
                "Bienvenido, Juan", LocalDate.of(2026, 11, 20), 15);

        String html = AccessPage.render(result, "ABCDEFGHJK");

        assertTrue(html.contains("ENTRADA REGISTRADA"));
        assertTrue(html.contains("#1FAE74"));
        assertTrue(html.contains("Juan Pablo Cuello"));
        assertTrue(html.contains("20 de noviembre 2026"));
        assertFalse(html.contains("/photo/"));
    }

    @Test
    void exitPageIsBlueAndWarnsWhenTheMembershipEndsSoon() {
        AccessResult result = new AccessResult(Outcome.EXIT, member("Ana Gómez", "data/photos/x.jpg"),
                "Hasta pronto, Ana", LocalDate.of(2026, 11, 8), 3);

        String html = AccessPage.render(result, "ABCDEFGHJK");

        assertTrue(html.contains("SALIDA REGISTRADA"));
        assertTrue(html.contains("#2F7DE1"));
        assertTrue(html.toLowerCase(java.util.Locale.ROOT).contains("quedan 3 días"));
        assertTrue(html.contains("/photo/ABCDEFGHJK"));
    }

    @Test
    void deniedPageIsRedAndShowsTheReason() {
        AccessResult result = new AccessResult(Outcome.DENIED, null, "Código no válido.", null, -1);

        String html = AccessPage.render(result, "x");

        assertTrue(html.contains("ACCESO DENEGADO"));
        assertTrue(html.contains("#E5484D"));
        assertTrue(html.contains("Código no válido."));
    }

    @Test
    void textFromTheMemberIsEscaped() {
        AccessResult result = new AccessResult(Outcome.DENIED, member("<script>alert(1)</script>", ""),
                "\"quoted\" & <b>", null, -1);

        String html = AccessPage.render(result, "x");

        assertFalse(html.contains("<script>alert"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("&quot;quoted&quot; &amp; &lt;b&gt;"));
    }

    @Test
    void aGrantedScanLinksToThePortalOfTheMember() {
        AccessResult entry = new AccessResult(Outcome.ENTRY, member("Juan Pablo Cuello", ""),
                "Bienvenido, Juan", LocalDate.of(2026, 11, 20), 15);
        AccessResult exit = new AccessResult(Outcome.EXIT, member("Juan Pablo Cuello", ""),
                "Hasta pronto", LocalDate.of(2026, 11, 20), 15);

        assertTrue(AccessPage.render(entry, "ABCDEFGHJK").contains("href=\"/m/ABCDEFGHJK\""));
        assertTrue(AccessPage.render(exit, "ABCDEFGHJK").contains("href=\"/m/ABCDEFGHJK\""));
    }

    @Test
    void aDeniedScanHasNoLinkToThePortal() {
        AccessResult denied = new AccessResult(Outcome.DENIED, member("Juan Pablo Cuello", ""),
                "Membresía vencida", null, -1);

        assertFalse(AccessPage.render(denied, "ABCDEFGHJK").contains("/m/"));
    }
}
