package com.pg.seo.config;

/**
 * Resolves a credential by reference.
 *
 * <p>The application never stores a credential. An entity holds a reference (a key), and the
 * service resolves it here immediately before the provider needs it. The value is never
 * cached on an entity, never returned to a client, and never logged.
 *
 * <p>Implementations must not return the same mutable byte array to two callers, and must
 * not keep the value after the call returns.
 */
public interface SecretStore {

    /**
     * Returns the secret behind {@code reference}, or null when there is no such secret.
     *
     * @param reference the key stored on the entity, for example {@code smtp/42}
     */
    String resolve(String reference);

    /**
     * Stores a secret and returns the reference to use.
     *
     * @param namespace a logical grouping, for example {@code smtp}
     * @param key       the identifier within the namespace
     * @param value     the secret value
     */
    String store(String namespace, String key, String value);

    /**
     * Removes a secret. A missing reference is not an error.
     */
    void delete(String reference);
}
