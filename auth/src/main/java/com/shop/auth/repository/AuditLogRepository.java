package com.shop.auth.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.auth.entity.AuditLog;
import com.shop.auth.utils.AuditStatus;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Returns a filtered, paginated view of the audit log.
     *
     * <p>Both parameters are optional — pass {@code null} to skip that filter.
     * Text search is case-insensitive and matches actor name, action, resource,
     * or details.</p>
     *
     * @param status filter by outcome; {@code null} = all statuses
     * @param q      free-text search term; {@code null} or blank = no text filter
     * @param pageable pagination and sort (default: createdAt DESC)
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:status IS NULL OR a.status = :status)
              AND (:q IS NULL OR :q = ''
                   OR LOWER(a.actorName) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.action)    LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.resource)  LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.details)   LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<AuditLog> findFiltered(
            @Param("status") AuditStatus status,
            @Param("q") String q,
            Pageable pageable);
}
