package io.github.instaxdutta.winnow.security;

import java.util.Set;

/**
 * The role vocabulary.
 *
 * <p>The codes are the ones the {@code app_role} table stores, already prefixed with
 * {@code ROLE_}. They are used verbatim as the granted authority, so a role code and an
 * authority string are the same thing.
 *
 * <p>The four roles here are the ones the application seeds and the ones the vocabulary
 * below describes. Gating the home page on a role that no seeded role carries, such as
 * {@code hasRole("USER")}, gives every user a 403 on the home page.
 */
public final class RoleNames {

    /** Full administrative control, including account administration. */
    public static final String ADMIN = "ROLE_ADMIN";

    /** May approve a template and run a campaign. */
    public static final String MANAGER = "ROLE_MANAGER";

    /** May create and edit templates, clients, groups and accounts. */
    public static final String OPERATOR = "ROLE_OPERATOR";

    /** Read only access to everything the tenant owns. */
    public static final String VIEWER = "ROLE_VIEWER";

    /** Reading needs an authenticated caller and nothing more. */
    public static final String READ = "isAuthenticated()";

    /** Creating or editing templates, clients, groups and accounts. */
    public static final String OPERATOR_OR_ABOVE = "hasAnyRole('ROLE_OPERATOR','ROLE_MANAGER','ROLE_ADMIN')";

    /** Approving a template. */
    public static final String MANAGER_OR_ABOVE = "hasAnyRole('ROLE_MANAGER','ROLE_ADMIN')";

    /** Administering accounts. */
    public static final String ADMIN_ONLY = "hasRole('ROLE_ADMIN')";

    /** Every role the application knows. */
    public static final Set<String> ALL = Set.of(ADMIN, MANAGER, OPERATOR, VIEWER);

    private RoleNames() {}

    /** True when the caller may create or edit templates, clients, groups and accounts. */
    public static boolean atLeastOperator(Set<String> authorities) {
        return containsAny(authorities, OPERATOR, MANAGER, ADMIN);
    }

    /** True when the caller may approve a template. */
    public static boolean atLeastManager(Set<String> authorities) {
        return containsAny(authorities, MANAGER, ADMIN);
    }

    /** True when the caller may administer accounts. */
    public static boolean isAdmin(Set<String> authorities) {
        return authorities != null && authorities.contains(ADMIN);
    }

    private static boolean containsAny(Set<String> authorities, String... roles) {
        if (authorities == null) {
            return false;
        }
        for (String role : roles) {
            if (authorities.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
