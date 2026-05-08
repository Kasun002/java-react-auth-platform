package com.shop.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.shop.auth.entity.User;
import com.shop.auth.utils.AuthProvider;
import com.shop.auth.utils.UserStatus;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findById(Long id);
    Optional<User> findByAdObjectId(String adObjectId);

    // ── Dashboard aggregate queries ───────────────────────────────────────────

    long countByStatus(UserStatus status);
    long countByAuthProvider(AuthProvider authProvider);

    /**
     * Returns all users with their group memberships eagerly fetched.
     * Used by the dashboard group-distribution computation to avoid N+1 queries.
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.groups")
    List<User> findAllWithGroups();
}
