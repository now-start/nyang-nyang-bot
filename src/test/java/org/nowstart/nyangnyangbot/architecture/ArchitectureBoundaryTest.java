package org.nowstart.nyangnyangbot.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ArchitectureBoundaryTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java/org/nowstart/nyangnyangbot");

    @Test
    void domainLayer_ShouldNotDependOnAdaptersOrPersistence() throws IOException {
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("domain"))
                .filter(path -> containsAny(path,
                        "org.nowstart.nyangnyangbot.adapter.",
                        "org.nowstart.nyangnyangbot.repository",
                        "org.nowstart.nyangnyangbot.data.entity",
                        "org.springframework.web"
                ))
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void applicationLayer_ShouldNotDependOnAdapters() throws IOException {
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("application"))
                .filter(path -> containsAny(path,
                        "org.nowstart.nyangnyangbot.adapter.",
                        "org.nowstart.nyangnyangbot.repository",
                        "org.nowstart.nyangnyangbot.data.entity",
                        "org.springframework.web.bind.annotation"
                ))
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void serviceLayer_ShouldDependOnPortsInsteadOfPersistence() throws IOException {
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("service"))
                .filter(path -> containsAny(path,
                        "org.nowstart.nyangnyangbot.repository",
                        "org.nowstart.nyangnyangbot.data.entity"
                ))
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void inboundWebAdapters_ShouldNotDependOnPersistence() throws IOException {
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("adapter/in/web"))
                .filter(path -> containsAny(path,
                        "org.nowstart.nyangnyangbot.repository",
                        "org.nowstart.nyangnyangbot.data.entity"
                ))
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void legacyControllerPackage_ShouldNotContainJavaSources() throws IOException {
        List<Path> javaSources = javaFiles(SOURCE_ROOT.resolve("controller")).toList();

        assertThat(javaSources).isEmpty();
    }

    private Stream<Path> javaFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return Stream.empty();
        }
        return Files.walk(root).filter(path -> path.toString().endsWith(".java"));
    }

    private boolean containsAny(Path path, String... needles) {
        try {
            String source = Files.readString(path);
            for (String needle : needles) {
                if (source.contains(needle)) {
                    return true;
                }
            }
            return false;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read source file " + path, ex);
        }
    }
}
