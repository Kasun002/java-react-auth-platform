package com.shop.auth.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Maps an Azure AD / LDAP group to a local {@link UserGroup}.
 *
 * <p>
 * The {@code adGroupId} is the Azure AD Object ID (GUID) or the LDAP CN,
 * depending on {@code app.ad.ldap.group-source} configuration. It is always
 * stored and compared in its original form — no normalisation is applied.
 *
 * <p>
 * When the local group is deleted, the mapping row is kept (FK ON DELETE SET
 * NULL) so history is preserved and the group can be remapped later.
 */
@Data
@Entity
@Table(name = "ad_group_mappings")
public class AdGroupMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Azure AD Object ID (GUID) or LDAP CN — unique, never null. */
    @Column(nullable = false, unique = true, length = 255)
    private String adGroupId;

    /** Human-readable display name from Azure AD or LDAP — informational only. */
    @Column(length = 255)
    private String adGroupName;

    /**
     * The local UserGroup this AD group is mapped to.
     * NULL if the local group was deleted (ON DELETE SET NULL on the FK).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_group_id")
    private UserGroup localGroup;

    /**
     * TRUE when this mapping was created automatically by the unmapped-group
     * strategy.
     */
    @Column(nullable = false)
    private boolean autoCreated = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
