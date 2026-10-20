package io.github.instaxdutta.winnow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * An SMTP sending account.
 *
 * <p>The credential is never held here. {@code secretRef} is a key into the secret store;
 * the service resolves it immediately before the send and discards it afterwards.
 */
@Getter
@Setter
@Entity
@Table(name = "smtp_account")
public class SmtpAccount extends TenantOwnedEntity {

    @Column(name = "email", nullable = false, length = 254, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", nullable = false, length = 16)
    private AccountProtocol protocol = AccountProtocol.SMTP;

    @Column(name = "host", nullable = false, length = 255)
    private String host;

    @Column(name = "port", nullable = false)
    private int port;

    @Column(name = "username", nullable = false, length = 254)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method", nullable = false, length = 16)
    private AuthMethod authMethod = AuthMethod.PLAIN;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "secret_ref", length = 255)
    private String secretRef;
}
