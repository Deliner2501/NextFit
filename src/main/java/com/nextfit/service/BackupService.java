package com.nextfit.service;

import com.nextfit.exceptions.InvalidDataException;
import com.nextfit.exceptions.PersistenceException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Makes and restores backups of the data folder. A backup is one zip with the CSV files, the access key
 * and the exercise pictures. It knows nothing about screens: it only works with folders and files.
 *
 * <p>Restoring is careful: it checks that the zip really is a NextFit backup, refuses any entry that
 * could write outside the data folder, and makes a safety backup of the current data first.
 */
public class BackupService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String PREFIX = "nextfit-respaldo-";
    private static final String SAFETY_PREFIX = "nextfit-antes-de-restaurar-";
    private static final Set<String> PICTURE_TYPES = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final long MAX_RESTORE_BYTES = 1L << 30;

    private final Path dataDir;
    private final Path backupDir;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param dataDir   the folder with the application data
     * @param backupDir the folder where the backups are kept (outside the data folder)
     * @param clock     the clock, to name the files and to know what "today" is
     */
    public BackupService(Path dataDir, Path backupDir, Clock clock) {
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.backupDir = backupDir.toAbsolutePath().normalize();
        this.clock = clock;
    }

    /**
     * The folder where the backups are kept.
     *
     * @return the backups folder
     */
    public Path backupDir() {
        return backupDir;
    }

    /**
     * Makes a backup of the data now.
     *
     * @return the backup file
     */
    public Path createBackup() {
        return write(PREFIX);
    }

    /**
     * Makes a backup only when none was made today. It is meant to run when the application starts, and
     * it also removes the oldest automatic backups so the folder does not grow forever.
     *
     * @param keep how many backups to keep
     * @return the new backup, or {@code null} when there already was one today
     */
    public Path backupIfNoneToday(int keep) {
        LocalDate today = LocalDate.now(clock);
        boolean done = list().stream().anyMatch(b -> b.name().startsWith(PREFIX) && b.createdAt().toLocalDate().equals(today));
        Path created = done ? null : createBackup();
        prune(keep);
        return created;
    }

    /**
     * Lists the backups, the newest first.
     *
     * @return the backups found in the folder
     */
    public List<BackupInfo> list() {
        List<BackupInfo> found = new ArrayList<>();
        if (!Files.isDirectory(backupDir)) {
            return found;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(backupDir, "nextfit-*.zip")) {
            for (Path file : files) {
                LocalDateTime created = LocalDateTime.ofInstant(Files.getLastModifiedTime(file).toInstant(),
                        clock.getZone());
                found.add(new BackupInfo(file, created, Files.size(file)));
            }
        } catch (IOException e) {
            throw new PersistenceException("No se pudo leer la carpeta de respaldos", e);
        }
        found.sort(Comparator.comparing(BackupInfo::createdAt).reversed().thenComparing(BackupInfo::name,
                Comparator.reverseOrder()));
        return found;
    }

    /**
     * Restores the data from a backup. The current data is saved in a safety backup first.
     *
     * @param zip the backup file
     * @return the safety backup of the data that was replaced
     * @throws InvalidDataException when the file is not a NextFit backup or has unsafe contents
     */
    public Path restore(Path zip) {
        List<String> names = check(zip);
        Path safety = write(SAFETY_PREFIX);
        Path staging = null;
        try {
            Files.createDirectories(backupDir);
            staging = Files.createTempDirectory(backupDir, ".restaurando-");
            try (ZipFile file = new ZipFile(zip.toFile())) {
                for (String name : names) {
                    Path target = staging.resolve(name);
                    Files.createDirectories(target.getParent());
                    try (InputStream in = file.getInputStream(file.getEntry(name))) {
                        Files.copy(in, target);
                    }
                }
            }
            for (String name : names) {
                Path target = dataDir.resolve(name);
                Files.createDirectories(target.getParent());
                Files.move(staging.resolve(name), target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new PersistenceException("No se pudo restaurar el respaldo. Tus datos anteriores están en "
                    + safety.getFileName(), e);
        } finally {
            deleteTree(staging);
        }
        return safety;
    }

    // Validates the zip and returns the names of the entries to extract.
    private List<String> check(Path zip) {
        if (zip == null || !Files.isRegularFile(zip)) {
            throw new InvalidDataException("respaldo", "el archivo no existe");
        }
        List<String> names = new ArrayList<>();
        long total = 0;
        try (ZipFile file = new ZipFile(zip.toFile())) {
            var entries = file.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!allowed(name)) {
                    throw new InvalidDataException("respaldo", "contiene un archivo que no corresponde: " + name);
                }
                total += entry.getSize() < 0 ? MAX_RESTORE_BYTES : entry.getSize();
                if (total > MAX_RESTORE_BYTES) {
                    throw new InvalidDataException("respaldo", "es demasiado grande");
                }
                names.add(name);
            }
        } catch (IOException e) {
            throw new InvalidDataException("respaldo", "no es un archivo zip válido");
        }
        if (!names.contains("users.csv")) {
            throw new InvalidDataException("respaldo", "no parece un respaldo de NextFit (falta users.csv)");
        }
        return names;
    }

    // Only the files a backup holds, always inside the data folder.
    private boolean allowed(String name) {
        if (name.isEmpty() || name.startsWith("/") || name.contains("\\") || name.contains(":")) {
            return false;
        }
        Path relative = Path.of(name).normalize();
        if (relative.isAbsolute() || relative.startsWith("..") || !relative.toString().replace('\\', '/').equals(name)) {
            return false;
        }
        if (!dataDir.resolve(name).normalize().startsWith(dataDir)) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (!lower.contains("/")) {
            return lower.endsWith(".csv") || lower.equals("access.key");
        }
        return lower.startsWith("images/") && PICTURE_TYPES.contains(lower.substring(lower.lastIndexOf('.') + 1));
    }

    private Path write(String prefix) {
        Path target = backupDir.resolve(prefix + LocalDateTime.now(clock).format(STAMP) + ".zip");
        try {
            Files.createDirectories(backupDir);
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(tmp));
                 Stream<Path> files = Files.isDirectory(dataDir) ? Files.walk(dataDir) : Stream.empty()) {
                for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile).sorted()::iterator) {
                    String name = dataDir.relativize(file).toString().replace('\\', '/');
                    if (allowed(name)) {
                        out.putNextEntry(new ZipEntry(name));
                        Files.copy(file, out);
                        out.closeEntry();
                    }
                }
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            throw new PersistenceException("No se pudo crear el respaldo", e);
        }
    }

    private void prune(int keep) {
        List<BackupInfo> automatic = list().stream().filter(b -> b.name().startsWith(PREFIX)).toList();
        for (int i = keep; i < automatic.size(); i++) {
            try {
                Files.deleteIfExists(automatic.get(i).file());
            } catch (IOException e) {
                // A backup that cannot be removed now will be tried again next time.
            }
        }
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            // Leftover temporary files are harmless.
        }
    }
}
