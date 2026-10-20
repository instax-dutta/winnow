package io.github.instaxdutta.winnow.web;

import io.github.instaxdutta.winnow.security.RoleNames;

/**
 * The role checks the endpoints use.
 *
 * <p>The names come from the security package, so a role that is renamed there is renamed
 * here as well.
 */
final class Roles {

    /** Any authenticated caller. */
    static final String READ = RoleNames.READ;

    /** The administrators. */
    static final String ADMIN = "hasRole('" + RoleNames.ADMIN + "')";

    /** The administrators and the managers. */
    static final String APPROVE = "hasAnyRole('" + RoleNames.ADMIN + "','" + RoleNames.MANAGER + "')";

    /** The administrators and the operators, who administer the sending accounts. */
    static final String ACCOUNTS = "hasAnyRole('" + RoleNames.ADMIN + "','" + RoleNames.OPERATOR + "')";

    /** Everybody who may change data. */
    static final String MUTATE = "hasAnyRole('"
            + RoleNames.ADMIN
            + "','"
            + RoleNames.MANAGER
            + "','"
            + RoleNames.OPERATOR
            + "')";

    private Roles() {}
}
