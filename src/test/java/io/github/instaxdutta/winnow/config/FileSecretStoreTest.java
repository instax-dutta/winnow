package io.github.instaxdutta.winnow.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The file backed secret store.
 *
 * <p>The store holds the only copy of a credential, so the file it writes is owner only
 * and the name of the file discloses nothing about the value.
 */
class FileSecretStoreTest {

    /** The value used in the fixtures. It is a test fixture, not a credential. */
    private static final String VALUE = "fixture-value-not-a-credential";

    @TempDir
    Path storeDirectory;

    private FileSecretStore store;

    @BeforeEach
    void createStore() {
        store = new FileSecretStore(new SecretStoreProperties(storeDirectory, "file"));
    }

    /** Whether the file system of the test can carry POSIX permissions. */
    private static boolean posixPermissionsAreSupported() {
        return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    /** The one file the store wrote. */
    private Path secretFile() throws IOException {
        try (var files = Files.list(storeDirectory)) {
            List<Path> regular = files.filter(Files::isRegularFile).toList();
            assertThat(regular).hasSize(1);
            return regular.get(0);
        }
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", e);
        }
    }

    @Test
    void theStoredValueIsTheResolvedValue() {
        String reference = store.store("smtp", "42", VALUE);

        assertThat(store.resolve(reference)).isEqualTo(VALUE);
    }

    @Test
    void aMissingReferenceResolvesToNull() {
        assertThat(store.resolve("smtp/there-is-no-such-secret")).isNull();
    }

    @Test
    void aBlankReferenceResolvesToNull() {
        assertThat(store.resolve(null)).isNull();
        assertThat(store.resolve("  ")).isNull();
    }

    @Test
    void aDeletedSecretResolvesToNull() {
        String reference = store.store("smtp", "42", VALUE);

        store.delete(reference);

        assertThat(store.resolve(reference)).isNull();
    }

    @Test
    void deletingAMissingSecretIsNotAnError() {
        store.delete("smtp/there-is-no-such-secret");
        store.delete(null);
    }

    @Test
    void theSecretFileIsNamedAfterTheHashOfTheReferenceAndNotAfterTheValue() throws IOException {
        String reference = store.store("smtp", "42", VALUE);

        Path file = secretFile();
        String name = file.getFileName().toString();

        assertThat(name).isEqualTo(sha256Hex(reference));
        assertThat(name).doesNotContain(VALUE);
        assertThat(name).doesNotContain("smtp");
        assertThat(name).doesNotContain("42");
        assertThat(name).matches("[0-9a-f]{64}");
    }

    @Test
    void theDirectoryListingNeverShowsTheValue() throws IOException {
        store.store("smtp", "42", VALUE);
        store.store("gmail", "7", "another-fixture-value");

        try (var files = Files.list(storeDirectory)) {
            assertThat(files.map(path -> path.getFileName().toString()))
                    .noneMatch(name -> name.contains(VALUE) || name.contains("another-fixture-value"));
        }
    }

    @Test
    void theSecretFileIsOwnerOnly() throws IOException {
        store.store("smtp", "42", VALUE);

        Path file = secretFile();
        if (posixPermissionsAreSupported()) {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file);
            assertThat(permissions)
                    .containsExactlyInAnyOrder(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE);
            assertThat(Files.getPosixFilePermissions(storeDirectory))
                    .containsExactlyInAnyOrder(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE);
        } else {
            assertThat(Files.isRegularFile(file)).isTrue();
        }
    }

    @Test
    void theFileHoldsTheValueAndLivesInsideTheStoreDirectory() throws IOException {
        store.store("smtp", "42", VALUE);

        Path file = secretFile();

        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo(VALUE);
        assertThat(file.getParent()).isEqualTo(storeDirectory);
    }

    @Test
    void storingTheSameReferenceTwiceKeepsTheLastValue() {
        store.store("smtp", "42", "first-value");
        String reference = store.store("smtp", "42", "second-value");

        assertThat(store.resolve(reference)).isEqualTo("second-value");
    }

    @Test
    void aStoreWithoutANamespaceOrAKeyIsRefused() {
        assertThatThrownBy(() -> store.store("smtp", "", VALUE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.store("  ", "42", VALUE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.store("smtp", "42", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
