package com.pg.seo.repository;

import com.pg.seo.domain.GmailAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GmailAccountRepository extends JpaRepository<GmailAccount, Long> {

    List<GmailAccount> findByOwnerUserIdAndDeletedAtIsNullOrderByEmailAsc(Long ownerUserId);

    Optional<GmailAccount> findByIdAndOwnerUserIdAndDeletedAtIsNull(Long id, Long ownerUserId);

    Optional<GmailAccount> findByOwnerUserIdAndEmailAndDeletedAtIsNull(Long ownerUserId, String email);

    long countByOwnerUserIdAndDeletedAtIsNull(Long ownerUserId);
}
