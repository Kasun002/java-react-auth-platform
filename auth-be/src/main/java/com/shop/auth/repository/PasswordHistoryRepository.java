package com.shop.auth.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.auth.entity.PasswordHistory;
import com.shop.auth.entity.User;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    /** Returns the N most recent password history entries for the user, newest first. */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.user = :user ORDER BY ph.createdAt DESC")
    List<PasswordHistory> findRecentByUser(@Param("user") User user, Pageable pageable);

    /** Returns all history entry IDs for the user, newest first — used for pruning old entries. */
    @Query("SELECT ph.id FROM PasswordHistory ph WHERE ph.user = :user ORDER BY ph.createdAt DESC")
    List<Long> findAllIdsByUserOrderByCreatedAtDesc(@Param("user") User user);

    void deleteByIdIn(List<Long> ids);
}
