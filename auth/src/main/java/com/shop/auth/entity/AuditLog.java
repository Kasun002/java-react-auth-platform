package com.shop.auth.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.shop.auth.utils.AuditStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Persistent audit trail entry — PCI-DSS v4 Req 10.2.
 *
 * <p>Written by {@code AuditLogService.record()} at the end of every
 * admin operation. Never updated after creation.</p>
 */
@Data
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The admin user who performed the action. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    /** Denormalised name so the log remains readable even if the user is deleted. */
    @Column(nullable = false, length = 255)
    private String actorName;

    /** Machine-readable action code, e.g. ROLE_ASSIGNED, USER_SUSPENDED. */
    @Column(nullable = false, length = 100)
    private String action;

    /** Resource type affected, e.g. USER, ROLE, GROUP. */
    @Column(nullable = false, length = 100)
    private String resource;

    /** ID of the specific resource instance (may be null for bulk operations). */
    @Column(length = 255)
    private String resourceId;

    /** Human-readable description of what changed. */
    @Column(columnDefinition = "TEXT")
    private String details;

    /** Originating IP — PCI-DSS Req 10.2.4. */
    @Column(length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuditStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
