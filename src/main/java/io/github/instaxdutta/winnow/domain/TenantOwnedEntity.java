package io.github.instaxdutta.winnow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Base for every entity that belongs to exactly one tenant.
 *
 * <p>{@code ownerUserId} is the tenant key. It is written once and never updated, so a row
 * cannot be moved between tenants. Soft delete is {@code deletedAt}: a null value means
 * live. The filter is applied explicitly by the repository, never by a Hibernate class
 * filter, because a class filter is silently bypassed by native queries.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantOwnedEntity extends AuditedEntity {

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private Long ownerUserId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
