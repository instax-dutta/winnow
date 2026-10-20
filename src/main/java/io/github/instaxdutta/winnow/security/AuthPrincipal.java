package io.github.instaxdutta.winnow.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;

/**
 * The authenticated caller.
 *
 * <p>{@code userId} is the tenant key. Every service method takes it explicitly so that a
 * request can never choose its own tenant. Reading the tenant from a session attribute
 * that is set inconsistently lets one tenant read another's data.
 */
public record AuthPrincipal(Long userId, String username, Set<String> authorities) implements Serializable {

    public AuthPrincipal {
        authorities = Set.copyOf(authorities);
    }

    public static AuthPrincipal of(Long userId, String username, Collection<? extends GrantedAuthority> granted) {
        return new AuthPrincipal(
                userId,
                username,
                granted.stream().map(GrantedAuthority::getAuthority).collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }

    public boolean hasRole(String role) {
        return authorities.contains(role);
    }
}
