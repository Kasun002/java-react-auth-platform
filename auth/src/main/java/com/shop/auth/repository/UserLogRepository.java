package com.shop.auth.repository;

import java.util.List;

import com.shop.auth.entity.UserLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserLogRepository extends JpaRepository<UserLog, Long> {

    /**
     * Returns the 10 most-recent login events across all users, with the
     * associated user eagerly fetched to avoid N+1 queries in the dashboard.
     */
    @Query("SELECT ul FROM UserLog ul JOIN FETCH ul.user ORDER BY ul.issuedAt DESC")
    List<UserLog> findTop10WithUserOrderByIssuedAtDesc(org.springframework.data.domain.Pageable pageable);
}
