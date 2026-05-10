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
    private static final List<String> ALLOWED_TOP_LEVEL_PACKAGES = List.of(
            "adapter",
            "application",
            "config",
            "domain"
    );

    @Test
    void sourceRoot_ShouldOnlyContainHexagonalTopLevelPackages() throws IOException {
        List<String> violations;
        try (Stream<Path> paths = Files.list(SOURCE_ROOT)) {
            violations = paths
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !ALLOWED_TOP_LEVEL_PACKAGES.contains(name))
                    .toList();
        }

        assertThat(violations).isEmpty();
    }

    @Test
    void domainLayer_ShouldNotDependOnAdaptersOrPersistence() throws IOException {
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("domain"))
                .filter(path -> containsAny(path,
                        "org.nowstart.nyangnyangbot.application.",
                        "org.nowstart.nyangnyangbot.adapter.",
                        "jakarta.persistence",
                        "org.springframework.",
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
                        "org.springframework.web.bind.annotation"
                ))
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void inboundWebAdapters_ShouldNotDependOnPersistence() throws IOException {
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("adapter/in/web"))
                .filter(path -> containsAny(path,
                        "org.nowstart.nyangnyangbot.adapter.out.persistence.repository",
                        "org.nowstart.nyangnyangbot.adapter.out.persistence.entity"
                ))
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void legacyTopLevelPackages_ShouldNotContainJavaSources() throws IOException {
        List<Path> javaSources = Stream.of("controller", "data", "repository", "service")
                .map(SOURCE_ROOT::resolve)
                .flatMap(this::javaFilesUnchecked)
                .toList();

        assertThat(javaSources).isEmpty();
    }

    private Stream<Path> javaFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return Stream.empty();
        }
        return Files.walk(root).filter(path -> path.toString().endsWith(".java"));
    }

    private Stream<Path> javaFilesUnchecked(Path root) {
        try {
            return javaFiles(root);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to walk source root " + root, ex);
        }
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
