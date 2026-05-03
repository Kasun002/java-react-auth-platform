package com.shop.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Stores the BCrypt hashes of a user's previous passwords.
 *
 * <p>Used to enforce the banking requirement that a user cannot reuse any of
 * their last N passwords (PCI-DSS Req 8.3.6 / NIST 800-63B §5.1.1).</p>
 *
 * <p>Only the hash is stored — the raw password is never persisted.</p>
 */
@Data
@Entity
@Table(
    name = "password_history",
    indexes = @Index(name = "idx_password_history_user_created", columnList = "user_id, created_at DESC")
)
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** BCrypt hash of the historical password. Never store plain text. */
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
