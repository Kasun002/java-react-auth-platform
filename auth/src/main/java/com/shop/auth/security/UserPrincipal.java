package com.shop.auth.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Immutable principal populated from JWT claims on every request.
 * Never touches the database — all data is sourced from the validated token.
 *
 * <p>Held in the {@code SecurityContext} for the duration of the request.
 * Controllers and services can access it via
 * {@code (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()}.</p>
 *
 * <p>Account-level flags (locked, enabled, etc.) are intentionally {@code true}:
 * those checks are performed at login time. A valid JWT implies the account was
 * ACTIVE when the token was issued.</p>
 */
public class UserPrincipal implements UserDetails {

    private final Long                         userId;
    private final String                       email;
    private final Collection<GrantedAuthority> authorities;
    private final List<String>                 groups;

    public UserPrincipal(Long userId,
                         String email,
                         Collection<? extends GrantedAuthority> authorities,
                         List<String> groups) {
        this.userId      = userId;
        this.email       = email;
        this.authorities = Collections.unmodifiableList(new ArrayList<>(authorities));
        this.groups      = Collections.unmodifiableList(groups);
    }

    public Long getId() {
        return userId;
    }

    public List<String> getGroups() {
        return groups;
    }

    // ── UserDetails ───────────────────────────────────────────────────────────

    @Override public String                       getUsername()             { return email; }
    @Override public String                       getPassword()             { return null;  }
    @Override public Collection<GrantedAuthority> getAuthorities()          { return authorities; }
    @Override public boolean                      isAccountNonExpired()     { return true;  }
    @Override public boolean                      isAccountNonLocked()      { return true;  }
    @Override public boolean                      isCredentialsNonExpired() { return true;  }
    @Override public boolean                      isEnabled()               { return true;  }
}
