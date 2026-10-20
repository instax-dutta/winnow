package io.github.instaxdutta.winnow.security;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * The authenticated application user.
 *
 * <p>The stock {@link User} carries the name, the password and the authorities but not the
 * user id, and the id is the tenant key every service takes. This subclass adds it.
 *
 * <p>It extends {@link User} rather than replacing it so the equality, the serialisation
 * and the session registry key of the stock type are unchanged.
 */
public class AppUserPrincipal extends User {

    private static final long serialVersionUID = 1L;

    private final Long userId;

    public AppUserPrincipal(
            Long userId,
            String username,
            String password,
            Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
    }

    /** The tenant key. */
    public Long userId() {
        return userId;
    }

    /** The granted authorities as plain strings. */
    public Set<String> authorityNames() {
        return getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());
    }

    /** The same caller as the principal the web layer works with. */
    public AuthPrincipal authPrincipal() {
        return new AuthPrincipal(userId, getUsername(), authorityNames());
    }
}
