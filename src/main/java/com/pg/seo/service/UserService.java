package com.pg.seo.service;

import com.pg.seo.domain.AppUser;
import com.pg.seo.domain.NotFoundException;
import com.pg.seo.domain.ValidationException;
import com.pg.seo.model.ChangePasswordRequest;
import com.pg.seo.model.UserDto;
import com.pg.seo.repository.AppUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The user account of the caller.
 *
 * <p>The password is never read back, never returned and never logged. The hash that the
 * encoder produces is the only thing that is stored.
 */
@Service
public class UserService {

    /** The shortest password the application accepts. */
    private static final int MINIMUM_PASSWORD_LENGTH = 12;

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserDto current(Long ownerUserId) {
        return toDto(require(ownerUserId));
    }

    /**
     * Every user of the platform. The user table is the tenant table itself, so it is not
     * tenant owned data and the listing is not filtered.
     */
    @Transactional(readOnly = true)
    public List<UserDto> list(Long ownerUserId) {
        return users.findAll().stream()
                .filter(user -> user.getDeletedAt() == null)
                .map(UserService::toDto)
                .toList();
    }

    @Transactional
    public void changePassword(Long ownerUserId, ChangePasswordRequest request) {
        AppUser user = require(ownerUserId);
        if (request.currentPassword() == null || request.currentPassword().isEmpty()) {
            throw new ValidationException("The current password is required");
        }
        if (request.newPassword() == null || request.newPassword().length() < MINIMUM_PASSWORD_LENGTH) {
            throw new ValidationException(
                    "The new password must be at least " + MINIMUM_PASSWORD_LENGTH + " characters long");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ValidationException("The current password is not correct");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        user.setUpdatedBy(ownerUserId);
        users.save(user);
    }

    private AppUser require(Long ownerUserId) {
        return users
                .findByIdAndDeletedAtIsNull(ownerUserId)
                .orElseThrow(() -> NotFoundException.of("User", ownerUserId));
    }

    private static UserDto toDto(AppUser user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getCode())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getEmployeeId(),
                user.getStatus(),
                roles,
                user.getLastLoginAt());
    }
}
