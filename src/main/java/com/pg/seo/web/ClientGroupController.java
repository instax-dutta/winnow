package com.pg.seo.web;

import com.pg.seo.model.ClientGroupDto;
import com.pg.seo.model.ClientGroupRequest;
import com.pg.seo.security.AuthPrincipalResolver;
import com.pg.seo.service.ClientGroupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The client groups.
 */
@RestController
@RequestMapping("/api/groups")
public class ClientGroupController {

    private final ClientGroupService groups;
    private final AuthPrincipalResolver principals;

    public ClientGroupController(ClientGroupService groups, AuthPrincipalResolver principals) {
        this.groups = groups;
        this.principals = principals;
    }

    /**
     * The groups of the tenant.
     */
    @GetMapping
    @PreAuthorize(Roles.READ)
    public ApiResponse<List<ClientGroupDto>> list() {
        return ApiResponse.ok(groups.list(principals.currentUserId()));
    }

    /**
     * Creates one group.
     */
    @PostMapping
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<ClientGroupDto> create(@Valid @RequestBody ClientGroupRequest request) {
        return ApiResponse.ok(groups.create(principals.currentUserId(), request));
    }

    /**
     * Changes one group.
     */
    @PutMapping("/{id}")
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<ClientGroupDto> update(
            @PathVariable("id") Long id, @Valid @RequestBody ClientGroupRequest request) {
        return ApiResponse.ok(groups.update(principals.currentUserId(), id, request));
    }

    /**
     * Removes one group.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.MUTATE)
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        groups.delete(principals.currentUserId(), id);
        return ApiResponse.ok("The group was removed", null);
    }
}
