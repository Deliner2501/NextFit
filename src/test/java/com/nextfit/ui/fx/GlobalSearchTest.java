package com.nextfit.ui.fx;

import com.nextfit.model.Member;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalSearchTest {

    private static Member member(long id, String name, String document, String phone, boolean active) {
        return new Member(id, name, document, phone, "a@b.co", LocalDate.of(1995, 1, 1),
                LocalDate.of(2026, 1, 1), "", "Mamá 3001112233", active);
    }

    private final List<Member> members = List.of(
            member(1, "María Gómez", "1001", "3001112233", true),
            member(2, "Mario Pérez", "1002", "3114445566", false),
            member(3, "Luis Gómez", "1003", "3207778899", true));

    @Test
    void ignoresCaseAndAccents() {
        assertEquals("maria gomez", GlobalSearch.normalize("  MARÍA Gómez "));
        assertTrue(GlobalSearch.matches("maria", "María Gómez"));
        assertTrue(GlobalSearch.matches("GOMEZ", "María Gómez"));
    }

    @Test
    void blankQueryMatchesNothing() {
        assertFalse(GlobalSearch.matches("   ", "María"));
        assertFalse(GlobalSearch.matches(null, "María"));
    }

    @Test
    void everyWordMustAppear() {
        assertTrue(GlobalSearch.matches("luis gomez", "Luis Gómez"));
        assertFalse(GlobalSearch.matches("luis perez", "Luis Gómez"));
    }

    @Test
    void findsMembersByNameDocumentAndPhone() {
        assertEquals(2, GlobalSearch.memberHits("members", "gomez", members, 10).size());
        assertEquals("2", GlobalSearch.memberHits("members", "1002", members, 10).get(0).targetId());
        assertEquals("3", GlobalSearch.memberHits("members", "3207778899", members, 10).get(0).targetId());
    }

    @Test
    void marksInactiveMembersAndHonoursTheLimit() {
        SearchHit inactive = GlobalSearch.memberHits("members", "mario", members, 10).get(0);
        assertTrue(inactive.detail().contains("Inactivo"));
        assertEquals(1, GlobalSearch.memberHits("members", "gomez", members, 1).size());
    }
}
