package io.github.instaxdutta.winnow.repository;

import io.github.instaxdutta.winnow.domain.GmailAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GmailAccountRepository extends JpaRepository<GmailAccount, Long> {

    List<GmailAccount> findByOwnerUserIdAndDeletedAtIsNullOrderByEmailAsc(Long ownerUserId);

    Optional<GmailAccount> findByIdAndOwnerUserIdAndDeletedAtIsNull(Long id, Long ownerUserId);

    Optional<GmailAccount> findByOwnerUserIdAndEmailAndDeletedAtIsNull(Long ownerUserId, String email);

    long countByOwnerUserIdAndDeletedAtIsNull(Long ownerUserId);

    /**
     * The address is unique across the whole platform, not only within a tenant, so the
     * duplicate check has to be global. Without it a cross-tenant duplicate surfaces as a
     * database constraint violation instead of a 409.
     */
    boolean existsByEmail(String email);
}
