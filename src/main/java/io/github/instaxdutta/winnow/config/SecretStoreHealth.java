package io.github.instaxdutta.winnow.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Checks at start-up that the credential store is usable.
 *
 * <p>A cluster whose instances do not share one volume fails in a way that is easy to
 * miss: a credential written on one instance is simply not there on another, and the first
 * symptom is a mail that fails to send, some hours later, with no obvious cause.
 *
 * <p>This check turns that into a start-up failure with a message that says what to do.
 * A single host, or a cluster on a shared volume, passes it.
 */
@Component
public class SecretStoreHealth implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SecretStoreHealth.class);

    private final SecretStoreProperties properties;
    private final Environment environment;

    public SecretStoreHealth(SecretStoreProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"file".equals(properties.type())) {
            LOG.info("The credential store is '{}', the file store check does not apply", properties.type());
            return;
        }
        Path root = properties.path().toAbsolutePath().normalize();
        try {
            if (!Files.exists(root)) {
                Files.createDirectories(root);
                LOG.warn("The credential store directory {} was created", root);
            }
            if (!Files.isDirectory(root) || !Files.isReadable(root) || !Files.isWritable(root)) {
                throw new IOException("the directory is not a readable and writable directory");
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "The credential store at " + root + " is not usable: " + e.getMessage()
                            + ". On a single host, point SEO_SECRET_STORE_PATH at a writable directory."
                            + " On a cluster, every instance must mount the SAME shared volume there,"
                            + " or a credential written on one instance will be missing on another.",
                    e);
        }
        // A shared volume is what makes a cluster work. Say so plainly at start-up.
        String instances = environment.getProperty("seo.secret-store.instances", "1");
        if (!"1".equals(instances)) {
            LOG.warn("This instance is one of {} and the credential store is a local directory."
                    + " Every instance must mount the same shared volume at {}.",
                    instances, root);
        }
        LOG.info("The credential store at {} is ready", root);
    }
}
