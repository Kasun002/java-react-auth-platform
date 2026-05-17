package com.shop.auth.exception;

import com.shop.auth.utils.UserStatus;
import org.springframework.http.HttpStatus;

public class UserNotActiveException extends BusinessException {

    public UserNotActiveException(UserStatus status) {
        super("Account is not active. Current status: " + status, HttpStatus.FORBIDDEN);
    }
}
