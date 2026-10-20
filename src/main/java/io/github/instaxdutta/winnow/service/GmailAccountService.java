package io.github.instaxdutta.winnow.service;

import io.github.instaxdutta.winnow.config.SecretStore;
import io.github.instaxdutta.winnow.domain.ConflictException;
import io.github.instaxdutta.winnow.domain.GmailAccount;
import io.github.instaxdutta.winnow.domain.NotFoundException;
import io.github.instaxdutta.winnow.model.GmailAccountDto;
import io.github.instaxdutta.winnow.model.GmailAccountRequest;
import io.github.instaxdutta.winnow.repository.GmailAccountRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Google OAuth sending accounts.
 *
 * <p>The client secret and the refresh token are both write only. The refresh token grants
 * permanent access to a mailbox, so it lives in the secret store and the account keeps
 * nothing but a reference to it. Keeping all four OAuth fields in the database and
 * returning them, including a live refresh token, to any logged-in user hands over the
 * account.
 */
@Service
public class GmailAccountService {

    /** The namespace the credentials of this account kind live under. */
    private static final String NAMESPACE = "gmail";

    private static final String CLIENT_SECRET_SUFFIX = ":clientSecret";
    private static final String REFRESH_TOKEN_SUFFIX = ":refreshToken";

    private final GmailAccountRepository accounts;
    private final SecretStore secrets;

    public GmailAccountService(GmailAccountRepository accounts, SecretStore secrets) {
        this.accounts = accounts;
        this.secrets = secrets;
    }

    @Transactional(readOnly = true)
    public List<GmailAccountDto> list(Long ownerUserId) {
        return accounts.findByOwnerUserIdAndDeletedAtIsNullOrderByEmailAsc(ownerUserId).stream()
                .map(GmailAccountService::toDto)
                .toList();
    }

    @Transactional
    public GmailAccountDto create(Long ownerUserId, GmailAccountRequest request) {
        if (accounts.findByOwnerUserIdAndEmailAndDeletedAtIsNull(ownerUserId, request.email()).isPresent()) {
            throw new ConflictException("An account with the address '" + request.email() + "' already exists");
        }
        Instant now = Instant.now();
        GmailAccount account = new GmailAccount();
        account.setOwnerUserId(ownerUserId);
        account.setEmail(request.email().trim());
        account.setOauthClientId(request.oauthClientId().trim());
        account.setScopes(request.scopes());
        account.setEnabled(request.enabled());
        account.setCreatedAt(now);
        account.setCreatedBy(ownerUserId);
        account.setUpdatedAt(now);
        account.setUpdatedBy(ownerUserId);
        accounts.save(account);

        storeSecrets(account, request);
        return toDto(account);
    }

    @Transactional
    public GmailAccountDto update(Long ownerUserId, Long id, GmailAccountRequest request) {
        GmailAccount account = require(ownerUserId, id);
        if (!account.getEmail().equals(request.email().trim())
                && accounts.findByOwnerUserIdAndEmailAndDeletedAtIsNull(ownerUserId, request.email()).isPresent()) {
            throw new ConflictException("An account with the address '" + request.email() + "' already exists");
        }
        account.setEmail(request.email().trim());
        account.setOauthClientId(request.oauthClientId().trim());
        account.setScopes(request.scopes());
        account.setEnabled(request.enabled());
        account.setUpdatedAt(Instant.now());
        account.setUpdatedBy(ownerUserId);

        storeSecrets(account, request);
        accounts.save(account);
        return toDto(account);
    }

    @Transactional
    public void delete(Long ownerUserId, Long id) {
        GmailAccount account = require(ownerUserId, id);
        if (account.getSecretRef() != null) {
            secrets.delete(account.getSecretRef());
            account.setSecretRef(null);
        }
        if (account.getRefreshTokenRef() != null) {
            secrets.delete(account.getRefreshTokenRef());
            account.setRefreshTokenRef(null);
        }
        Instant now = Instant.now();
        account.setDeletedAt(now);
        account.setUpdatedAt(now);
        account.setUpdatedBy(ownerUserId);
        accounts.save(account);
    }

    private void storeSecrets(GmailAccount account, GmailAccountRequest request) {
        String id = String.valueOf(account.getId());
        if (hasValue(request.clientSecret())) {
            account.setSecretRef(secrets.store(NAMESPACE, id + CLIENT_SECRET_SUFFIX, request.clientSecret()));
        }
        if (hasValue(request.refreshToken())) {
            account.setRefreshTokenRef(secrets.store(NAMESPACE, id + REFRESH_TOKEN_SUFFIX, request.refreshToken()));
        }
        if (account.getSecretRef() != null || account.getRefreshTokenRef() != null) {
            account.setUpdatedAt(Instant.now());
            accounts.save(account);
        }
    }

    private GmailAccount require(Long ownerUserId, Long id) {
        return accounts
                .findByIdAndOwnerUserIdAndDeletedAtIsNull(id, ownerUserId)
                .orElseThrow(() -> NotFoundException.of("Gmail account", id));
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private static GmailAccountDto toDto(GmailAccount account) {
        return new GmailAccountDto(
                account.getId(),
                account.getEmail(),
                account.getOauthClientId(),
                account.getScopes(),
                account.isEnabled(),
                account.getSecretRef() != null && account.getRefreshTokenRef() != null);
    }
}
