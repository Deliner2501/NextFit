package com.nextfit.ai;

import com.nextfit.exceptions.AiException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unchecked")
class JsonTest {

    @Test
    void readsAnObjectWithNestedValues() {
        Map<String, Object> map = (Map<String, Object>) Json.parse(
                "{\"name\":\"Ana\",\"active\":true,\"phone\":null,\"tags\":[\"a\",\"b\"],\"inner\":{\"x\":1}}");

        assertEquals("Ana", map.get("name"));
        assertEquals(true, map.get("active"));
        assertNull(map.get("phone"));
        assertTrue(map.containsKey("phone"));
        assertEquals(List.of("a", "b"), map.get("tags"));
        assertEquals(1L, ((Map<String, Object>) map.get("inner")).get("x"));
    }

    @Test
    void wholeNumbersAreLongAndDecimalsAreDouble() {
        List<Object> numbers = (List<Object>) Json.parse("[7, -3, 2.5, 1e3]");

        assertEquals(7L, numbers.get(0));
        assertEquals(-3L, numbers.get(1));
        assertEquals(2.5, numbers.get(2));
        assertEquals(1000.0, numbers.get(3));
    }

    @Test
    void readsEscapesAndUnicode() {
        assertEquals("línea1\nlínea2\t\"ok\" \\ /", Json.parse("\"l\\u00ednea1\\nl\\u00ednea2\\t\\\"ok\\\" \\\\ \\/\""));
    }

    @Test
    void toleratesSpacesAndEmptyContainers() {
        Map<String, Object> map = (Map<String, Object>) Json.parse("  { \"a\" : [ ] , \"b\" : { } }  ");

        assertEquals(List.of(), map.get("a"));
        assertEquals(Map.of(), map.get("b"));
    }

    @Test
    void writesObjectsArraysAndScalarsInOrder() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "Ana");
        map.put("age", 30);
        map.put("score", 2.5);
        map.put("active", true);
        map.put("phone", null);
        map.put("tags", List.of("a", "b"));

        assertEquals("{\"name\":\"Ana\",\"age\":30,\"score\":2.5,\"active\":true,\"phone\":null,\"tags\":[\"a\",\"b\"]}",
                Json.write(map));
    }

    @Test
    void writesEscapesAndKeepsAccents() {
        assertEquals("\"mañana \\\"sí\\\"\\n\\t\\\\\"", Json.write("mañana \"sí\"\n\t\\"));
        assertEquals("\"\\u0001\"", Json.write("\u0001"));
    }

    @Test
    void whatIsWrittenCanBeReadBack() {
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("text", "Hola \"mundo\"\nsegunda línea");
        original.put("list", List.of(1L, 2L, 3L));
        original.put("nested", Map.of("ok", false));

        assertEquals(original, Json.parse(Json.write(original)));
    }

    @Test
    void invalidTextIsRejected() {
        assertThrows(AiException.class, () -> Json.parse(""));
        assertThrows(AiException.class, () -> Json.parse("{\"a\":1} extra"));
        assertThrows(AiException.class, () -> Json.parse("\"sin cerrar"));
        assertThrows(AiException.class, () -> Json.parse("{\"a\" 1}"));
        assertThrows(AiException.class, () -> Json.parse("[1,2"));
        assertThrows(AiException.class, () -> Json.parse("tru"));
        assertThrows(AiException.class, () -> Json.parse("\"\\x\""));
        assertThrows(AiException.class, () -> Json.parse("{\"a\":}"));
    }

    @Test
    void valuesJsonCannotHoldAreRejected() {
        assertThrows(AiException.class, () -> Json.write(new Object()));
        assertThrows(AiException.class, () -> Json.write(Double.NaN));
        assertThrows(AiException.class, () -> Json.write(List.of(Double.POSITIVE_INFINITY)));
    }
}