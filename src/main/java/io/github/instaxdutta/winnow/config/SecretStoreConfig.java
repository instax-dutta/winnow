package io.github.instaxdutta.winnow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses where a credential lives.
 *
 * <p>The file backed store is the default. It is the right answer for a single host and
 * for a cluster whose instances share one volume, because the value is read from the volume
 * on every call and is never cached in the process.
 *
 * <p>A deployment that cannot use a shared volume points {@code seo.secret-store.type} at
 * another implementation. Any bean of type {@link SecretStore} registered under a different
 * type wins, and nothing in the service layer has to change, because the service layer only
 * knows the interface. A store backed by a key management service is the obvious thing to
 * contribute here.
 */
@Configuration
public class SecretStoreConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SecretStoreConfig.class);

    /**
     * The file backed store.
     *
     * <p>It is registered only when no other store has been selected, so a contributed
     * implementation does not have to be excluded explicitly.
     */
    @Bean
    @ConditionalOnProperty(name = "seo.secret-store.type", havingValue = "file", matchIfMissing = true)
    public FileSecretStore fileSecretStore(SecretStoreProperties properties) {
        LOG.info("Credentials are held in the file store at {}", properties.path().toAbsolutePath());
        return new FileSecretStore(properties);
    }
}
