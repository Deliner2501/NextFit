package com.nextfit.ai;

import com.nextfit.exceptions.AiException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small reader and writer of JSON, enough to talk with the AI service without adding a
 * library. Objects are read as {@code Map<String, Object>}, arrays as {@code List<Object>},
 * whole numbers as {@code Long} and decimals as {@code Double}.
 */
public final class Json {

    private Json() {
    }

    /**
     * Reads a JSON text.
     *
     * @param text the JSON
     * @return a Map, List, String, Long, Double, Boolean or null
     * @throws AiException when the text is not valid JSON
     */
    public static Object parse(String text) {
        Parser parser = new Parser(text == null ? "" : text);
        Object value = parser.readValue();
        parser.skipSpaces();
        if (!parser.atEnd()) {
            throw parser.error("hay texto después del final");
        }
        return value;
    }

    /**
     * Writes a value as JSON.
     *
     * @param value a Map, List, String, Number, Boolean or null
     * @return the JSON text
     * @throws AiException when a value has a type that JSON cannot hold
     */
    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        append(out, value);
        return out.toString();
    }

    private static void append(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            appendString(out, text);
        } else if (value instanceof Boolean flag) {
            out.append(flag);
        } else if (value instanceof Double || value instanceof Float) {
            double number = ((Number) value).doubleValue();
            if (Double.isNaN(number) || Double.isInfinite(number)) {
                throw new AiException("No se puede escribir un número que no es finito en JSON");
            }
            out.append(number);
        } else if (value instanceof Number number) {
            out.append(number);
        } else if (value instanceof Map<?, ?> map) {
            appendObject(out, map);
        } else if (value instanceof Iterable<?> items) {
            appendArray(out, items);
        } else {
            throw new AiException("No se puede escribir en JSON un valor de tipo " + value.getClass().getSimpleName());
        }
    }

    private static void appendObject(StringBuilder out, Map<?, ?> map) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            appendString(out, String.valueOf(entry.getKey()));
            out.append(':');
            append(out, entry.getValue());
        }
        out.append('}');
    }

    private static void appendArray(StringBuilder out, Iterable<?> items) {
        out.append('[');
        boolean first = true;
        for (Object item : items) {
            if (!first) {
                out.append(',');
            }
            first = false;
            append(out, item);
        }
        out.append(']');
    }

    private static void appendString(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    // EXPLICACIÓN: lector recursivo; cada método lee un tipo de valor y deja la posición justo después.
    private static final class Parser {

        private final String text;
        private int position;

        Parser(String text) {
            this.text = text;
        }

        boolean atEnd() {
            return position >= text.length();
        }

        void skipSpaces() {
            while (!atEnd() && Character.isWhitespace(text.charAt(position))) {
                position++;
            }
        }

        AiException error(String reason) {
            return new AiException("La respuesta de la IA no es un JSON válido (" + reason
                    + ", posición " + position + ")");
        }

        Object readValue() {
            skipSpaces();
            if (atEnd()) {
                throw error("falta un valor");
            }
            char c = text.charAt(position);
            if (c == '{') {
                return readObject();
            }
            if (c == '[') {
                return readArray();
            }
            if (c == '"') {
                return readString();
            }
            if (c == 't') {
                return readLiteral("true", Boolean.TRUE);
            }
            if (c == 'f') {
                return readLiteral("false", Boolean.FALSE);
            }
            if (c == 'n') {
                return readLiteral("null", null);
            }
            if (c == '-' || Character.isDigit(c)) {
                return readNumber();
            }
            throw error("carácter inesperado '" + c + "'");
        }

        private Map<String, Object> readObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            position++;
            skipSpaces();
            if (consumeIf('}')) {
                return map;
            }
            while (true) {
                skipSpaces();
                if (atEnd() || text.charAt(position) != '"') {
                    throw error("se esperaba el nombre de un campo");
                }
                String key = readString();
                skipSpaces();
                if (!consumeIf(':')) {
                    throw error("falta ':'");
                }
                map.put(key, readValue());
                skipSpaces();
                if (consumeIf(',')) {
                    continue;
                }
                if (consumeIf('}')) {
                    return map;
                }
                throw error("se esperaba ',' o '}'");
            }
        }

        private List<Object> readArray() {
            List<Object> list = new ArrayList<>();
            position++;
            skipSpaces();
            if (consumeIf(']')) {
                return list;
            }
            while (true) {
                list.add(readValue());
                skipSpaces();
                if (consumeIf(',')) {
                    continue;
                }
                if (consumeIf(']')) {
                    return list;
                }
                throw error("se esperaba ',' o ']'");
            }
        }

        private String readString() {
            StringBuilder out = new StringBuilder();
            position++;
            while (true) {
                if (atEnd()) {
                    throw error("texto sin cerrar");
                }
                char c = text.charAt(position++);
                if (c == '"') {
                    return out.toString();
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (atEnd()) {
                    throw error("texto sin cerrar");
                }
                char escaped = text.charAt(position++);
                switch (escaped) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'u' -> out.append(readUnicode());
                    default -> throw error("escape desconocido '\\" + escaped + "'");
                }
            }
        }

        private char readUnicode() {
            if (position + 4 > text.length()) {
                throw error("escape \\u incompleto");
            }
            try {
                char c = (char) Integer.parseInt(text.substring(position, position + 4), 16);
                position += 4;
                return c;
            } catch (NumberFormatException e) {
                throw error("escape \\u inválido");
            }
        }

        private Object readNumber() {
            int start = position;
            while (!atEnd() && "+-0123456789.eE".indexOf(text.charAt(position)) >= 0) {
                position++;
            }
            String number = text.substring(start, position);
            try {
                if (number.contains(".") || number.contains("e") || number.contains("E")) {
                    return Double.parseDouble(number);
                }
                return Long.parseLong(number);
            } catch (NumberFormatException e) {
                throw error("número inválido '" + number + "'");
            }
        }

        private Object readLiteral(String word, Object value) {
            if (!text.startsWith(word, position)) {
                throw error("se esperaba '" + word + "'");
            }
            position += word.length();
            return value;
        }

        private boolean consumeIf(char expected) {
            if (!atEnd() && text.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }
    }
}