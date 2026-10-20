package io.github.instaxdutta.winnow.service;

import io.github.instaxdutta.winnow.domain.ClientGroup;
import io.github.instaxdutta.winnow.domain.ConflictException;
import io.github.instaxdutta.winnow.domain.NotFoundException;
import io.github.instaxdutta.winnow.model.ClientGroupDto;
import io.github.instaxdutta.winnow.model.ClientGroupRequest;
import io.github.instaxdutta.winnow.repository.ClientGroupRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Client groups and their lifecycle.
 *
 * <p>A group code is unique across the platform, so the duplicate check is not tenant
 * scoped: the unique index behind it is global.
 */
@Service
public class ClientGroupService {

    private final ClientGroupRepository groups;

    public ClientGroupService(ClientGroupRepository groups) {
        this.groups = groups;
    }

    public List<ClientGroupDto> list(Long ownerUserId) {
        return groups.findByOwnerUserIdAndDeletedAtIsNullOrderByCodeAsc(ownerUserId).stream()
                .map(ClientGroupService::toDto)
                .toList();
    }

    @Transactional
    public ClientGroupDto create(Long ownerUserId, ClientGroupRequest request) {
        String code = request.code().trim();
        if (groups.existsByCode(code)) {
            throw new ConflictException("A client group with the code '" + code + "' already exists");
        }
        Instant now = Instant.now();
        ClientGroup group = new ClientGroup();
        group.setOwnerUserId(ownerUserId);
        group.setCode(code);
        group.setName(request.name().trim());
        group.setCreatedAt(now);
        group.setCreatedBy(ownerUserId);
        group.setUpdatedAt(now);
        group.setUpdatedBy(ownerUserId);
        groups.save(group);
        return toDto(group);
    }

    @Transactional
    public ClientGroupDto update(Long ownerUserId, Long id, ClientGroupRequest request) {
        ClientGroup group = require(ownerUserId, id);
        String code = request.code().trim();
        if (!code.equals(group.getCode()) && groups.existsByCode(code)) {
            throw new ConflictException("A client group with the code '" + code + "' already exists");
        }
        group.setCode(code);
        group.setName(request.name().trim());
        group.setUpdatedAt(Instant.now());
        group.setUpdatedBy(ownerUserId);
        groups.save(group);
        return toDto(group);
    }

    @Transactional
    public void delete(Long ownerUserId, Long id) {
        ClientGroup group = require(ownerUserId, id);
        Instant now = Instant.now();
        group.setDeletedAt(now);
        group.setUpdatedAt(now);
        group.setUpdatedBy(ownerUserId);
        groups.save(group);
    }

    private ClientGroup require(Long ownerUserId, Long id) {
        return groups
                .findByIdAndOwnerUserIdAndDeletedAtIsNull(id, ownerUserId)
                .orElseThrow(() -> NotFoundException.of("Client group", id));
    }

    private static ClientGroupDto toDto(ClientGroup group) {
        return new ClientGroupDto(group.getId(), group.getCode(), group.getName());
    }
}
