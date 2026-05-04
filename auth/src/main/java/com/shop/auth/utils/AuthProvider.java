package com.shop.auth.utils;

/**
 * Identifies how a user account was originally provisioned.
 *
 * <p>LOCAL  — registered via {@code POST /auth/register}; has a BCrypt password.
 * <p>AZURE_AD — provisioned on first Azure AD login via {@code POST /auth/ad/login};
 *              password column holds an unguessable random hash — local login is blocked.
 */
public enum AuthProvider {
    LOCAL,
    AZURE_AD
}
