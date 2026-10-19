package com.pg.seo.service;

import com.pg.seo.config.SecretStore;
import com.pg.seo.domain.ConflictException;
import com.pg.seo.domain.NotFoundException;
import com.pg.seo.domain.SmtpAccount;
import com.pg.seo.model.SmtpAccountDto;
import com.pg.seo.model.SmtpAccountRequest;
import com.pg.seo.repository.SmtpAccountRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SMTP sending accounts.
 *
 * <p>The password is write only. It goes straight into the secret store and the account
 * keeps nothing but the reference. The original stored the password in the account row and
 * returned it to any logged-in user.
 */
@Service
public class SmtpAccountService {

    /** The namespace the credentials of this account kind live under. */
    private static final String NAMESPACE = "smtp";

    private final SmtpAccountRepository accounts;
    private final SecretStore secrets;

    public SmtpAccountService(SmtpAccountRepository accounts, SecretStore secrets) {
        this.accounts = accounts;
        this.secrets = secrets;
    }

    @Transactional(readOnly = true)
    public List<SmtpAccountDto> list(Long ownerUserId) {
        return accounts.findByOwnerUserIdAndDeletedAtIsNullOrderByEmailAsc(ownerUserId).stream()
                .map(SmtpAccountService::toDto)
                .toList();
    }

    @Transactional
    public SmtpAccountDto create(Long ownerUserId, SmtpAccountRequest request) {
        if (accounts.findByOwnerUserIdAndEmailAndDeletedAtIsNull(ownerUserId, request.email()).isPresent()) {
            throw new ConflictException("An account with the address '" + request.email() + "' already exists");
        }
        Instant now = Instant.now();
        SmtpAccount account = new SmtpAccount();
        account.setOwnerUserId(ownerUserId);
        account.setEmail(request.email().trim());
        account.setProtocol(request.protocol());
        account.setHost(request.host().trim());
        account.setPort(request.port());
        account.setUsername(request.username().trim());
        account.setAuthMethod(request.authMethod());
        account.setEnabled(request.enabled());
        account.setCreatedAt(now);
        account.setCreatedBy(ownerUserId);
        account.setUpdatedAt(now);
        account.setUpdatedBy(ownerUserId);
        accounts.save(account);

        if (hasSecret(request.secret())) {
            account.setSecretRef(secrets.store(NAMESPACE, String.valueOf(account.getId()), request.secret()));
            account.setUpdatedAt(Instant.now());
            accounts.save(account);
        }
        return toDto(account);
    }

    @Transactional
    public SmtpAccountDto update(Long ownerUserId, Long id, SmtpAccountRequest request) {
        SmtpAccount account = require(ownerUserId, id);
        if (!account.getEmail().equals(request.email().trim())
                && accounts.findByOwnerUserIdAndEmailAndDeletedAtIsNull(ownerUserId, request.email()).isPresent()) {
            throw new ConflictException("An account with the address '" + request.email() + "' already exists");
        }
        account.setEmail(request.email().trim());
        account.setProtocol(request.protocol());
        account.setHost(request.host().trim());
        account.setPort(request.port());
        account.setUsername(request.username().trim());
        account.setAuthMethod(request.authMethod());
        account.setEnabled(request.enabled());
        account.setUpdatedAt(Instant.now());
        account.setUpdatedBy(ownerUserId);

        if (hasSecret(request.secret())) {
            account.setSecretRef(secrets.store(NAMESPACE, String.valueOf(account.getId()), request.secret()));
        }
        accounts.save(account);
        return toDto(account);
    }

    @Transactional
    public void delete(Long ownerUserId, Long id) {
        SmtpAccount account = require(ownerUserId, id);
        if (account.getSecretRef() != null) {
            secrets.delete(account.getSecretRef());
            account.setSecretRef(null);
        }
        Instant now = Instant.now();
        account.setDeletedAt(now);
        account.setUpdatedAt(now);
        account.setUpdatedBy(ownerUserId);
        accounts.save(account);
    }

    private SmtpAccount require(Long ownerUserId, Long id) {
        return accounts
                .findByIdAndOwnerUserIdAndDeletedAtIsNull(id, ownerUserId)
                .orElseThrow(() -> NotFoundException.of("SMTP account", id));
    }

    private static boolean hasSecret(String secret) {
        return secret != null && !secret.isBlank();
    }

    private static SmtpAccountDto toDto(SmtpAccount account) {
        return new SmtpAccountDto(
                account.getId(),
                account.getEmail(),
                account.getProtocol(),
                account.getHost(),
                account.getPort(),
                account.getUsername(),
                account.getAuthMethod(),
                account.isEnabled(),
                account.getSecretRef() != null);
    }
}
