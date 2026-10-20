package io.github.instaxdutta.winnow.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A secret store backed by one file per secret.
 *
 * <p>The file name is the SHA-256 hex of the reference, so a reference can never escape the
 * store directory and the directory listing does not disclose what is stored. The file is
 * created with mode 0600 inside a 0700 directory, and a write goes to a temporary file that
 * is then renamed, so a reader never sees a half written secret.
 *
 * <p>The value is read from disk on every call and is never cached, so a value rotated on
 * disk takes effect on the next call. The value is never logged.
 */
@Component
public class FileSecretStore implements SecretStore {

    private static final Logger log = LoggerFactory.getLogger(FileSecretStore.class);

    private static final HexFormat HEX = HexFormat.of();

    private static final Set<PosixFilePermission> FILE_PERMISSIONS = PosixFilePermissions.fromString("rw-------");

    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS = PosixFilePermissions.fromString("rwx------");

    private final Path root;

    public FileSecretStore(SecretStoreProperties properties) {
        this.root = properties.path().toAbsolutePath().normalize();
    }

    @Override
    public String resolve(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        Path file = fileFor(reference);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read the secret behind " + reference, ex);
        }
    }

    @Override
    public String store(String namespace, String key, String value) {
        if (namespace == null || namespace.isBlank() || key == null || key.isBlank()) {
            throw new IllegalArgumentException("A secret needs a namespace and a key");
        }
        if (value == null) {
            throw new IllegalArgumentException("A secret needs a value");
        }
        String reference = namespace + "/" + key;
        Path file = fileFor(reference);
        try {
            Files.createDirectories(root);
            applyPermissions(root, DIRECTORY_PERMISSIONS);
            Path temporary = Files.createTempFile(root, ".secret-", ".tmp");
            try {
                Files.writeString(temporary, value, StandardCharsets.UTF_8);
                applyPermissions(temporary, FILE_PERMISSIONS);
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot store the secret behind " + reference, ex);
        }
        log.debug("Stored the secret behind {}", reference);
        return reference;
    }

    @Override
    public void delete(String reference) {
        if (reference == null || reference.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(fileFor(reference));
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot delete the secret behind " + reference, ex);
        }
    }

    private Path fileFor(String reference) {
        return root.resolve(sha256Hex(reference));
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available on this JVM", ex);
        }
    }

    private static void applyPermissions(Path path, Set<PosixFilePermission> permissions) {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException | IOException ex) {
            log.debug("The file system of {} does not support POSIX permissions", path.getFileSystem());
        }
    }
}
