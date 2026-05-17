package com.shop.auth.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends BusinessException {

    public AccountLockedException(LocalDateTime lockedUntil) {
        super("Account temporarily locked until " + lockedUntil + ". Too many failed login attempts.",
                HttpStatus.UNAUTHORIZED);
    }
}
