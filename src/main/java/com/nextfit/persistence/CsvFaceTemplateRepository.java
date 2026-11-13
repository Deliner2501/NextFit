package com.nextfit.persistence;

import com.nextfit.model.FaceTemplate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Keeps the face templates in a CSV file: one row per template, the vector as numbers separated by semicolons. */
public class CsvFaceTemplateRepository implements FaceTemplateRepository {

    private static final String[] COLUMNS = {"member_id", "vector"};

    private final CsvStore store;

    /** @param file the CSV file, created when it does not exist */
    public CsvFaceTemplateRepository(Path file) {
        this.store = new CsvStore(file, COLUMNS);
    }

    @Override
    public synchronized List<FaceTemplate> findAll() {
        List<FaceTemplate> templates = new ArrayList<>();
        for (String[] row : store.readAll()) {
            templates.add(new FaceTemplate(Long.parseLong(row[0].trim()), parse(row[1])));
        }
        return templates;
    }

    @Override
    public synchronized void replace(long memberId, List<float[]> vectors) {
        List<String[]> rows = rowsWithout(memberId);
        for (float[] vector : vectors) {
            rows.add(new String[]{String.valueOf(memberId), format(vector)});
        }
        store.writeAll(rows);
    }

    @Override
    public synchronized void delete(long memberId) {
        store.writeAll(rowsWithout(memberId));
    }

    private List<String[]> rowsWithout(long memberId) {
        List<String[]> rows = new ArrayList<>();
        for (String[] row : store.readAll()) {
            if (Long.parseLong(row[0].trim()) != memberId) {
                rows.add(row);
            }
        }
        return rows;
    }

    private static String format(float[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(vector[i]);
        }
        return sb.toString();
    }

    private static float[] parse(String text) {
        String[] parts = text.trim().split(";");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }
}
