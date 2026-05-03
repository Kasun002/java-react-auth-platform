package com.shop.auth.utils;

/**
 * @deprecated Superseded by the RBAC group/role model ({@code user_group_memberships}).
 *             Kept for backward compatibility and JWT {@code role} claim.
 *             Do not add new values. Remove after V13 migration is run.
 */
@Deprecated(since = "RBAC-Step4", forRemoval = true)
public enum Role {
    USER,
    ADMIN
}
