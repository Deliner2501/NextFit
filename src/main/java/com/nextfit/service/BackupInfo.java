package com.nextfit.service;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * One backup file found in the backups folder.
 *
 * @param file      the zip file
 * @param createdAt when it was made
 * @param size      its size in bytes
 */
public record BackupInfo(Path file, LocalDateTime createdAt, long size) {

    /**
     * The file name.
     *
     * @return the name without its folder
     */
    public String name() {
        return file.getFileName().toString();
    }
}
