package com.shop.auth.service;

import java.util.List;

import com.shop.auth.dto.PermissionDto;

public interface PermissionService {

    /** Returns all permissions ordered by category and code. */
    List<PermissionDto> listAll();
}
