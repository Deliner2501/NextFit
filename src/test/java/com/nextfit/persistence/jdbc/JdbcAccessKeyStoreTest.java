package com.nextfit.persistence.jdbc;

import com.nextfit.persistence.AccessKeyStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class JdbcAccessKeyStoreTest {

    @TempDir
    Path data;

    @Test
    void adoptsTheFirstInstallationsKeyAndCachesItOnTheOtherComputer() {
        H2Database database = new H2Database();
        try {
            JdbcStorage oracle = new JdbcStorage(database);
            Path first = data.resolve("first/access.key");
            Path second = data.resolve("second/access.key");
            byte[] original = AccessKeyStore.loadOrCreate(first);
            AccessKeyStore.cache(second, new byte[32]);

            assertArrayEquals(original, oracle.accessKey(first));
            assertArrayEquals(original, oracle.accessKey(second));
            assertArrayEquals(original, AccessKeyStore.loadOrCreate(second));
        } finally {
            database.close();
        }
    }

    @Test
    void simultaneousFirstStartsUseOneKey() throws Exception {
        H2Database database = new H2Database();
        try (var executor = Executors.newFixedThreadPool(2)) {
            JdbcStorage oracle = new JdbcStorage(database);
            Path first = data.resolve("first/access.key");
            Path second = data.resolve("second/access.key");
            byte[] otherKey = new byte[32];
            Arrays.fill(otherKey, (byte) 42);
            AccessKeyStore.cache(first, new byte[32]);
            AccessKeyStore.cache(second, otherKey);

            var firstStart = executor.submit(() -> oracle.accessKey(first));
            var secondStart = executor.submit(() -> oracle.accessKey(second));

            assertArrayEquals(firstStart.get(), secondStart.get());
            assertArrayEquals(AccessKeyStore.loadOrCreate(first), AccessKeyStore.loadOrCreate(second));
        } finally {
            database.close();
        }
    }
}
