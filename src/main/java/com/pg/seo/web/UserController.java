package com.pg.seo.web;

import com.pg.seo.model.ChangePasswordRequest;
import com.pg.seo.model.UserDto;
import com.pg.seo.security.AuthPrincipalResolver;
import com.pg.seo.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The user accounts.
 *
 * <p>A user never carries a password hash. The DTO has no field for it.
 */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService users;
    private final AuthPrincipalResolver principals;

    public UserController(UserService users, AuthPrincipalResolver principals) {
        this.users = users;
        this.principals = principals;
    }

    /**
     * The account of the caller.
     */
    @GetMapping("/me")
    @PreAuthorize(Roles.READ)
    public ApiResponse<UserDto> me() {
        return ApiResponse.ok(users.current(principals.currentUserId()));
    }

    /**
     * Changes the password of the caller.
     */
    @PostMapping("/me/password")
    @PreAuthorize(Roles.READ)
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        users.changePassword(principals.currentUserId(), request);
        return ApiResponse.ok("The password was changed", null);
    }

    /**
     * Every user of the platform.
     */
    @GetMapping("/users")
    @PreAuthorize(Roles.ADMIN)
    public ApiResponse<List<UserDto>> list() {
        return ApiResponse.ok(users.list(principals.currentUserId()));
    }
}
