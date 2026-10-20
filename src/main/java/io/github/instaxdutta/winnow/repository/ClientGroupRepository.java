package io.github.instaxdutta.winnow.repository;

import io.github.instaxdutta.winnow.domain.ClientGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientGroupRepository extends JpaRepository<ClientGroup, Long> {

    List<ClientGroup> findByOwnerUserIdAndDeletedAtIsNullOrderByCodeAsc(Long ownerUserId);

    Optional<ClientGroup> findByIdAndOwnerUserIdAndDeletedAtIsNull(Long id, Long ownerUserId);

    Optional<ClientGroup> findByOwnerUserIdAndCodeAndDeletedAtIsNull(Long ownerUserId, String code);

    boolean existsByCode(String code);

    long countByOwnerUserIdAndDeletedAtIsNull(Long ownerUserId);
}
