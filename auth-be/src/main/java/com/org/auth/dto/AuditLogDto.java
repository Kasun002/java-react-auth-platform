package com.org.auth.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "A single audit log entry (PCI-DSS v4 Req 10.2)")
public class AuditLogDto {

    @Schema(description = "Surrogate PK", example = "1")
    private Long id;

    @Schema(description = "ID of the user who performed the action", example = "42")
    private Long actorId;

    @Schema(description = "Display name of the actor at the time of the action", example = "Priya Wickramasinghe")
    private String actorName;

    @Schema(description = "Machine-readable action code", example = "ROLE_ASSIGNED")
    private String action;

    @Schema(description = "Resource type affected", example = "USER")
    private String resource;

    @Schema(description = "ID of the specific resource instance", example = "usr-015")
    private String resourceId;

    @Schema(description = "Human-readable description of what changed")
    private String details;

    @Schema(description = "Originating IP address (PCI-DSS Req 10.2.4)", example = "192.168.1.45")
    private String ipAddress;

    @Schema(description = "Outcome of the action", example = "SUCCESS", allowableValues = {"SUCCESS", "WARNING", "FAILURE"})
    private String status;

    @Schema(description = "When the action occurred (UTC)")
    private LocalDateTime createdAt;
}
