package com.shop.auth.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.shop.auth.dto.AuditLogDto;
import com.shop.auth.entity.AuditLog;
import com.shop.auth.repository.AuditLogRepository;
import com.shop.auth.repository.UserRepository;
import com.shop.auth.service.AuditLogService;
import com.shop.auth.utils.AuditStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(String status, String q, Pageable pageable) {
        AuditStatus statusEnum = StringUtils.hasText(status) ? AuditStatus.valueOf(status.toUpperCase()) : null;
        String searchTerm = StringUtils.hasText(q) ? q.trim() : null;

        return auditLogRepository
                .findFiltered(statusEnum, searchTerm, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public void record(Long actorId, String actorName, String action, String resource,
                       String resourceId, String details, String ipAddress, AuditStatus status) {
        AuditLog entry = new AuditLog();
        // getReferenceById returns a Hibernate proxy — no SQL SELECT needed
        entry.setActor(userRepository.getReferenceById(actorId));
        entry.setActorName(actorName);
        entry.setAction(action);
        entry.setResource(resource);
        entry.setResourceId(resourceId);
        entry.setDetails(details);
        entry.setIpAddress(ipAddress);
        entry.setStatus(status);

        auditLogRepository.save(entry);
        log.info("Audit: actor=[{}] action=[{}] resource=[{}] resourceId=[{}] status=[{}]",
                actorId, action, resource, resourceId, status);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private AuditLogDto toDto(AuditLog entry) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(entry.getId());
        dto.setActorId(entry.getActor().getId());
        dto.setActorName(entry.getActorName());
        dto.setAction(entry.getAction());
        dto.setResource(entry.getResource());
        dto.setResourceId(entry.getResourceId());
        dto.setDetails(entry.getDetails());
        dto.setIpAddress(entry.getIpAddress());
        dto.setStatus(entry.getStatus().name());
        dto.setCreatedAt(entry.getCreatedAt());
        return dto;
    }
}
