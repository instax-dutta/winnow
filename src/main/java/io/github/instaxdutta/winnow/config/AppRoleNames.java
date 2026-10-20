package io.github.instaxdutta.winnow.config;

/**
 * The role codes, duplicated here so the config layer does not have to depend on the
 * security layer. The values are the same strings the security layer matches on, and the
 * database stores them verbatim.
 */
public final class AppRoleNames {

    /** Full access. */
    public static final String ADMIN = "ROLE_ADMIN";

    /** May approve templates and manage everything below. */
    public static final String MANAGER = "ROLE_MANAGER";

    /** May run campaigns and edit clients, groups, templates and accounts. */
    public static final String OPERATOR = "ROLE_OPERATOR";

    /** Read only. */
    public static final String VIEWER = "ROLE_VIEWER";

    private AppRoleNames() {}
}
