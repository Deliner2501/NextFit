package com.nextfit.persistence;

import com.nextfit.exceptions.PersistenceException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes one CSV file. It is the only class that knows the
 * file format, so every file repository delegates here and a future
 * database repository can replace them without touching the services.
 */
public final class CsvStore {

    private final Path file;
    private final String[] columns;

    /**
     * Opens the store, creating the file with only its header when it does not exist.
     *
     * @param file    the CSV file
     * @param columns the header columns
     */
    public CsvStore(Path file, String... columns) {
        this.file = file;
        this.columns = columns.clone();
        if (Files.notExists(file)) {
            writeAll(List.of());
        }
    }

    /**
     * Returns every data row (the header is skipped).
     *
     * @return the rows, each one as an array of fields
     */
    public List<String[]> readAll() {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            List<String[]> rows = parse(content);
            if (!rows.isEmpty()) {
                rows.remove(0);
            }
            return rows;
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer el archivo " + file, e);
        }
    }

    /**
     * Replaces the whole file with the header plus the given rows.
     *
     * @param rows the rows to store
     */
    public void writeAll(List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, columns);
        for (String[] row : rows) {
            appendRow(sb, row);
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new PersistenceException("No se pudo escribir el archivo " + file, e);
        }
    }

    private static void appendRow(StringBuilder sb, String[] fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields[i]));
        }
        sb.append('\n');
    }

    private static String escape(String value) {
        String v = value == null ? "" : value;
        boolean needsQuotes = v.indexOf(',') >= 0 || v.indexOf('"') >= 0
                || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0;
        return needsQuotes ? '"' + v.replace("\"", "\"\"") + '"' : v;
    }

    private static List<String[]> parse(String text) {
        List<String[]> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean any = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
                any = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
                any = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                if (any) {
                    row.add(field.toString());
                    rows.add(row.toArray(new String[0]));
                }
                row.clear();
                field.setLength(0);
                any = false;
            } else {
                field.append(c);
                any = true;
            }
        }
        if (any) {
            row.add(field.toString());
            rows.add(row.toArray(new String[0]));
        }
        return rows;
    }
}
