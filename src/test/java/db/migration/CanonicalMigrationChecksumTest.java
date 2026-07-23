package db.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;

class CanonicalMigrationChecksumTest {

    @Test
    void checksumIncludesMigrationAndSupportNestedClassBytecodeInBinaryNameOrder() throws IOException {
        CRC32 expected = new CRC32();
        updateClassTree(expected, CanonicalMigrationSupport.class);
        updateClassTree(expected, V8_1__materialize_roulette_history.class);

        assertThat(CanonicalMigrationSupport.checksum(
                V8_1__materialize_roulette_history.class,
                false
        )).isEqualTo((int) expected.getValue());
    }

    private void updateClassTree(CRC32 crc32, Class<?> rootClass) throws IOException {
        List<Class<?>> classes = new ArrayList<>();
        collect(rootClass, classes);
        classes.sort(Comparator.comparing(Class::getName));
        for (Class<?> type : classes) {
            crc32.update(type.getName().getBytes(StandardCharsets.UTF_8));
            crc32.update(classBytes(type));
        }
    }

    private void collect(Class<?> type, List<Class<?>> classes) {
        classes.add(type);
        for (Class<?> declaredClass : type.getDeclaredClasses()) {
            collect(declaredClass, classes);
        }
    }

    private byte[] classBytes(Class<?> type) throws IOException {
        String resourceName = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream inputStream = type.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Missing class resource " + resourceName);
            }
            return inputStream.readAllBytes();
        }
    }
}
