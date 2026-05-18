package com.org.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.org.auth.entity.UserGroup;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    Optional<UserGroup> findByName(String name);

    boolean existsByName(String name);

    /** True if at least one group has this role assigned. */
    boolean existsByRolesId(Long roleId);
}
