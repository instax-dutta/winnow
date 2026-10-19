package com.pg.seo.repository;

import com.pg.seo.domain.Client;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByOwnerUserIdAndDeletedAtIsNullOrderByNameAsc(Long ownerUserId);

    List<Client> findByOwnerUserIdAndGroupIdAndDeletedAtIsNullOrderByNameAsc(Long ownerUserId, Long groupId);

    Page<Client> findByOwnerUserIdAndDeletedAtIsNull(Long ownerUserId, Pageable pageable);

    Optional<Client> findByIdAndOwnerUserIdAndDeletedAtIsNull(Long id, Long ownerUserId);

    long countByOwnerUserIdAndDeletedAtIsNull(Long ownerUserId);

    long countByOwnerUserIdAndGroupIdAndDeletedAtIsNull(Long ownerUserId, Long groupId);
}
