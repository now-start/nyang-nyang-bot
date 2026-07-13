package org.nowstart.nyangnyangbot.architecture;

import static org.assertj.core.api.BDDAssertions.then;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
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
    private static final List<String> ALLOWED_DOMAIN_RECORDS = List.of(
            "AttendanceUserState.java",
            "FavoriteBalanceChange.java",
            "FavoriteLedgerEntry.java",
            "RouletteActivationValidation.java",
            "RouletteItemSnapshot.java"
    );

    @Test
    void sourceRoot_ShouldOnlyContainHexagonalTopLevelPackages() throws IOException {
        // 실행
        List<String> violations;
        try (Stream<Path> paths = Files.list(SOURCE_ROOT)) {
            violations = paths
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !ALLOWED_TOP_LEVEL_PACKAGES.contains(name))
                    .toList();
        }

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void domainLayer_ShouldNotDependOnAdaptersOrPersistence() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("domain"))
                .filter(path -> containsAny(path,
                        "org.nowstart.nyangnyangbot.application.",
                        "org.nowstart.nyangnyangbot.adapter.",
                        "jakarta.persistence",
                        "org.mapstruct.",
                        "org.springframework.",
                        "org.springframework.web"
                ))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void applicationLayer_ShouldNotDependOnAdaptersOrConfiguration() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("application"))
                .filter(path -> containsAny(path,
                        "org.nowstart.nyangnyangbot.adapter.",
                        "org.mapstruct.",
                        "org.nowstart.nyangnyangbot.config.",
                        "org.springframework.web.bind.annotation"
                ))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void applicationLayer_ShouldNotDependOnExternalClientLibraries() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("application"))
                .filter(path -> containsAny(path,
                        "com.fasterxml.jackson.",
                        "com.google.api.",
                        "com.google.auth.",
                        "io.socket.",
                        "org.springframework.cloud.openfeign"
                ))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void applicationModelPackage_ShouldNotContainJavaSources() throws IOException {
        // 실행
        List<Path> javaSources = javaFiles(SOURCE_ROOT.resolve("application/model")).toList();

        // 검증
        then(javaSources).isEmpty();
    }

    @Test
    void applicationExceptionPackage_ShouldNotContainJavaSources() throws IOException {
        // 실행
        List<Path> javaSources = javaFiles(SOURCE_ROOT.resolve("application/exception")).toList();

        // 검증
        then(javaSources).isEmpty();
    }

    @Test
    void applicationGatewayPackage_ShouldNotContainJavaSources() throws IOException {
        // 실행
        List<Path> javaSources = javaFiles(SOURCE_ROOT.resolve("application/gateway")).toList();

        // 검증
        then(javaSources).isEmpty();
    }

    @Test
    void applicationPort_ShouldNotContainSecondLevelTypePackages() throws IOException {
        // 실행
        List<Path> javaSources = Stream.of(
                        "application/port/in/attendance/dto",
                        "application/port/in/favorite/dto",
                        "application/port/in/favorite/usecase",
                        "application/port/in/overlay/dto",
                        "application/port/in/roulette/dto",
                        "application/port/in/upbo/dto",
                        "application/port/in/weeklychat/dto",
                        "application/port/out/authorization/repository",
                        "application/port/out/chzzk/dto",
                        "application/port/out/chzzk/repository",
                        "application/port/out/donation/repository",
                        "application/port/out/favorite/repository",
                        "application/port/out/google/dto",
                        "application/port/out/overlay/repository",
                        "application/port/out/roulette/dto",
                        "application/port/out/roulette/repository",
                        "application/port/out/upbo/dto",
                        "application/port/out/upbo/repository",
                        "application/port/out/weekly/repository"
                )
                .map(SOURCE_ROOT::resolve)
                .flatMap(this::javaFilesUnchecked)
                .toList();

        // 검증
        then(javaSources).isEmpty();
    }

    @Test
    void applicationInboundPorts_ShouldOnlyContainUseCaseInterfaces() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("application/port/in"))
                .filter(path -> !path.getFileName().toString().endsWith("UseCase.java")
                        || !declaresPublicInterfaceNamedAfterFile(path))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void applicationOutboundPorts_ShouldOnlyContainPortInterfaces() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("application/port/out"))
                .filter(path -> !path.getFileName().toString().endsWith("Port.java")
                        || !declaresPublicInterfaceNamedAfterFile(path))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void domainLayer_ShouldNotContainDtoClasses() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("domain"))
                .filter(path -> path.getFileName().toString().endsWith("Dto.java"))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void domainLayer_ShouldOnlyContainBusinessValueRecords() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("domain"))
                .filter(path -> containsAny(path, "public record "))
                .filter(path -> !ALLOWED_DOMAIN_RECORDS.contains(path.getFileName().toString()))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void applicationLayer_ShouldNotContainDtoNamedSources() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("application"))
                .filter(path -> path.getFileName().toString().endsWith("Dto.java")
                        || path.toString().contains("/dto/"))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void inboundWebAdapters_ShouldNotDependOnPersistence() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("adapter/in/web"))
                .filter(path -> containsAny(path,
                        "org.nowstart.nyangnyangbot.adapter.out.persistence."
                ))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void inboundWebAdapters_ShouldOnlyDependOnApplicationPorts() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("adapter/in/web"))
                .filter(path -> containsAny(path,
                        "org.nowstart.nyangnyangbot.application.service.",
                        "org.nowstart.nyangnyangbot.domain."
                ))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void persistenceAdapters_ShouldUseExplicitMappingWithoutMapStruct() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("adapter/out/persistence"))
                .filter(path -> path.getFileName().toString().endsWith("PersistenceMapper.java")
                        || containsAny(path, "org.mapstruct."))
                .toList();
        String buildScript = Files.readString(Path.of("build.gradle"));

        // 검증
        then(violations).isEmpty();
        then(buildScript).doesNotContain("mapstruct");
    }

    @Test
    void inboundWebAdapters_ShouldNotOwnScheduledTasks() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("adapter/in/web"))
                .filter(path -> containsAny(path,
                        "org.springframework.scheduling.annotation.Scheduled",
                        "@Scheduled"
                ))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void inboundWebCommonPackage_ShouldNotContainJavaSources() throws IOException {
        // 실행
        List<Path> javaSources = javaFiles(SOURCE_ROOT.resolve("adapter/in/web/common")).toList();

        // 검증
        then(javaSources).isEmpty();
    }

    @Test
    void persistenceLayer_ShouldNotContainSharedEntityOrRepositoryPackages() throws IOException {
        // 실행
        List<Path> javaSources = Stream.of("adapter/out/persistence/entity", "adapter/out/persistence/repository")
                .map(SOURCE_ROOT::resolve)
                .flatMap(this::javaFilesUnchecked)
                .toList();

        // 검증
        then(javaSources).isEmpty();
    }

    @Test
    void persistenceEntities_ShouldNotDeclareDdlConstraintsOrIndexes() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("adapter/out/persistence"))
                .filter(path -> path.toString().contains("/entity/"))
                .filter(path -> containsPattern(path,
                        "\\buniqueConstraints\\b",
                        "@UniqueConstraint\\b",
                        "@Index\\b",
                        "\\bunique\\s*=\\s*true\\b",
                        "\\bnullable\\s*=\\s*false\\b"
                ))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void persistenceEntities_ShouldUseImplicitJpaPhysicalNaming() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("adapter/out/persistence"))
                .filter(path -> path.toString().contains("/entity/"))
                .filter(path -> containsPattern(path,
                        "@Table\\s*\\([^)]*\\bname\\s*=",
                        "@Column\\s*\\([^)]*\\bname\\s*=",
                        "@JoinColumn\\s*\\([^)]*\\bname\\s*="
                ))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void persistenceEntityFiles_ShouldNotUseEntitySuffix() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("adapter/out/persistence"))
                .filter(path -> path.toString().contains("/entity/"))
                .filter(path -> path.getFileName().toString().endsWith("Entity.java"))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void externalAdapters_ShouldNotContainDtoPackagesOrDtoClasses() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("adapter/out/external"))
                .filter(path -> path.getFileName().toString().endsWith("Dto.java")
                        || path.toString().contains("/dto/"))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void externalProviderRoots_ShouldOnlyContainClientAdapters() throws IOException {
        // 실행
        List<Path> violations;
        try (Stream<Path> providers = Files.list(SOURCE_ROOT.resolve("adapter/out/external"))) {
            violations = providers
                    .filter(Files::isDirectory)
                    .flatMap(provider -> {
                        try {
                            return Files.list(provider)
                                    .filter(path -> path.toString().endsWith(".java"))
                                    .filter(path -> {
                                        String fileName = path.getFileName().toString();
                                        return !fileName.endsWith("ClientAdapter.java")
                                                && !fileName.endsWith("ExternalAdapter.java");
                                    });
                        } catch (IOException ex) {
                            throw new IllegalStateException("failed to list external provider root " + provider, ex);
                        }
                    })
                    .toList();
        }

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void externalRequestAndResponsePackages_ShouldUseMatchingSuffixes() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT.resolve("adapter/out/external"))
                .filter(path -> (path.toString().contains("/request/")
                        && !path.getFileName().toString().endsWith("Request.java"))
                        || (path.toString().contains("/response/")
                        && !path.getFileName().toString().endsWith("Response.java")))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void domainModelPackage_ShouldNotContainJavaSources() throws IOException {
        // 실행
        List<Path> javaSources = javaFiles(SOURCE_ROOT.resolve("domain/model")).toList();

        // 검증
        then(javaSources).isEmpty();
    }

    @Test
    void inboundWebControllers_ShouldLiveInFeaturePackages() throws IOException {
        // 실행
        List<Path> violations;
        try (Stream<Path> paths = Files.list(SOURCE_ROOT.resolve("adapter/in/web"))) {
            violations = paths
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .toList();
        }

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void mapperClasses_ShouldOnlyLiveInOutboundAdapters() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    if (fileName.endsWith("Mapper.java")) {
                        return !path.toString().contains("adapter\\out\\")
                                && !path.toString().contains("adapter/out/");
                    }
                    return fileName.endsWith("Converter.java")
                            || fileName.endsWith("Assembler.java")
                            || fileName.endsWith("Translator.java");
                })
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void legacyConversionNames_ShouldNotBeUsed() throws IOException {
        // 실행
        List<Path> violations = javaFiles(SOURCE_ROOT)
                .filter(path -> containsAny(path,
                        " to()",
                        " toCommand(",
                        " toDomain(",
                        " toResult(",
                        "fromDomain(",
                        "fromResult(",
                        "fromTableResult("
                ))
                .toList();

        // 검증
        then(violations).isEmpty();
    }

    @Test
    void legacyTopLevelPackages_ShouldNotContainJavaSources() throws IOException {
        // 실행
        List<Path> javaSources = Stream.of("controller", "data", "repository", "service")
                .map(SOURCE_ROOT::resolve)
                .flatMap(this::javaFilesUnchecked)
                .toList();

        // 검증
        then(javaSources).isEmpty();
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

    private boolean containsPattern(Path path, String... regexes) {
        try {
            String source = Files.readString(path);
            for (String regex : regexes) {
                if (Pattern.compile(regex).matcher(source).find()) {
                    return true;
                }
            }
            return false;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read source file " + path, ex);
        }
    }

    private boolean declaresPublicInterfaceNamedAfterFile(Path path) {
        String fileName = path.getFileName().toString();
        String typeName = fileName.substring(0, fileName.length() - ".java".length());

        try {
            return Files.readString(path).contains("public interface " + typeName);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read source file " + path, ex);
        }
    }
}
