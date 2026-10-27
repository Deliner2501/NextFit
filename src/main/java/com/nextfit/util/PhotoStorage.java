package com.nextfit.util;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.PersistenceException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Copies the photo a user picks into the application's photos folder, so the
 * member keeps the picture even if the original file moves or is deleted.
 */
public class PhotoStorage {

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Path directory;

    /**
     * Creates the storage.
     *
     * @param directory the folder where the photos are kept
     */
    public PhotoStorage(Path directory) {
        this.directory = directory;
    }

    /**
     * Copies a photo into the photos folder, named after the document number.
     * A previous photo of the same member is replaced.
     *
     * @param source         the picture chosen by the user (JPG or PNG, up to 5 MB)
     * @param documentNumber the document number of the member, used as file name
     * @return the path of the stored photo, with forward slashes
     * @throws InvalidDataException  when the file is not a JPG or PNG or is too big
     * @throws PersistenceException  when the file cannot be copied
     */
    public String copy(Path source, String documentNumber) {
        String extension = extensionOf(source);
        try {
            if (Files.size(source) > MAX_BYTES) {
                throw new InvalidDataException("foto", "no puede pesar más de 5 MB");
            }
            Files.createDirectories(directory);
            String safeName = documentNumber.replaceAll("[^A-Za-z0-9]", "_");
            Path target = directory.resolve(safeName + extension);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString().replace('\\', '/');
        } catch (IOException e) {
            throw new PersistenceException("No se pudo guardar la foto del socio", e);
        }
    }

    private String extensionOf(Path source) {
        String name = source.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return ".png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return ".jpg";
        }
        throw new InvalidDataException("foto", "debe ser una imagen JPG o PNG");
    }
}