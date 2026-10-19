package com.pg.seo.repository;

import com.pg.seo.domain.SmtpAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmtpAccountRepository extends JpaRepository<SmtpAccount, Long> {

    List<SmtpAccount> findByOwnerUserIdAndDeletedAtIsNullOrderByEmailAsc(Long ownerUserId);

    Optional<SmtpAccount> findByIdAndOwnerUserIdAndDeletedAtIsNull(Long id, Long ownerUserId);

    Optional<SmtpAccount> findByOwnerUserIdAndEmailAndDeletedAtIsNull(Long ownerUserId, String email);

    long countByOwnerUserIdAndDeletedAtIsNull(Long ownerUserId);

    /**
     * The address is unique across the whole platform, not only within a tenant, so the
     * duplicate check has to be global. Without it a cross-tenant duplicate surfaces as a
     * database constraint violation instead of a 409.
     */
    boolean existsByEmail(String email);
}
