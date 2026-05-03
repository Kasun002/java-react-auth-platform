package com.shop.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.auth.entity.BankingRole;

public interface BankingRoleRepository extends JpaRepository<BankingRole, Long> {

    Optional<BankingRole> findByName(String name);
}
