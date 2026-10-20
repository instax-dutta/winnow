package io.github.instaxdutta.winnow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A Google OAuth sending account.
 *
 * <p>The client secret and the refresh token are references, not values. The refresh token
 * is the highest-value credential in the system: it grants permanent read and send access to
 * a mailbox, so it lives in the secret store and never in the database.
 */
@Getter
@Setter
@Entity
@Table(name = "gmail_account")
public class GmailAccount extends TenantOwnedEntity {

    @Column(name = "email", nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "oauth_client_id", nullable = false, length = 255)
    private String oauthClientId;

    @Column(name = "secret_ref", length = 255)
    private String secretRef;

    @Column(name = "refresh_token_ref", length = 255)
    private String refreshTokenRef;

    @Column(name = "scopes", length = 512)
    private String scopes;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
