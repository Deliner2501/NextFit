package com.nextfit.persistence;

import com.nextfit.model.FaceTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvFaceTemplateRepositoryTest {

    @TempDir
    Path folder;

    @Test
    void startsEmpty() {
        assertTrue(new CsvFaceTemplateRepository(folder.resolve("faces.csv")).findAll().isEmpty());
    }

    @Test
    void keepsEveryNumberOfTheVectors() {
        CsvFaceTemplateRepository repo = new CsvFaceTemplateRepository(folder.resolve("faces.csv"));
        float[] vector = {0.123456789f, -1.5f, 3.0e-5f, 0f};

        repo.replace(7, List.of(vector, new float[]{1f, 2f, 3f, 4f}));

        List<FaceTemplate> read = new CsvFaceTemplateRepository(folder.resolve("faces.csv")).findAll();
        assertEquals(2, read.size());
        assertEquals(7, read.get(0).memberId());
        assertArrayEquals(vector, read.get(0).vector(), 0f);
    }

    @Test
    void replaceOnlyChangesThatMember() {
        CsvFaceTemplateRepository repo = new CsvFaceTemplateRepository(folder.resolve("faces.csv"));
        repo.replace(1, List.of(new float[]{1f}, new float[]{2f}));
        repo.replace(2, List.of(new float[]{3f}));

        repo.replace(1, List.of(new float[]{9f}));

        assertEquals(2, repo.findAll().size());
        assertTrue(repo.has(1) && repo.has(2));
    }

    @Test
    void deleteRemovesOnlyThatMember() {
        CsvFaceTemplateRepository repo = new CsvFaceTemplateRepository(folder.resolve("faces.csv"));
        repo.replace(1, List.of(new float[]{1f}));
        repo.replace(2, List.of(new float[]{3f}));

        repo.delete(1);

        assertFalse(repo.has(1));
        assertTrue(repo.has(2));
        repo.delete(99);
        assertEquals(1, repo.findAll().size());
    }
}
