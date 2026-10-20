package io.github.instaxdutta.winnow.repository;

import io.github.instaxdutta.winnow.domain.EmailTemplate;
import io.github.instaxdutta.winnow.domain.TemplateStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    List<EmailTemplate> findByOwnerUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long ownerUserId);

    List<EmailTemplate> findByOwnerUserIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(
            Long ownerUserId, TemplateStatus status);

    Optional<EmailTemplate> findByIdAndOwnerUserIdAndDeletedAtIsNull(Long id, Long ownerUserId);

    long countByOwnerUserIdAndDeletedAtIsNull(Long ownerUserId);

    long countByOwnerUserIdAndStatusAndDeletedAtIsNull(Long ownerUserId, TemplateStatus status);
}
