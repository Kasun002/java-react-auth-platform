package com.shop.auth.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.shop.auth.dto.RegisterRequestDto;
import com.shop.auth.entity.Address;
import com.shop.auth.entity.User;
import com.shop.auth.exception.EmailAlreadyExistsException;
import com.shop.auth.repository.UserRepository;
import com.shop.auth.service.AuthService;
import com.shop.auth.utils.MaskingUtil;
import com.shop.auth.utils.Role;
import com.shop.auth.utils.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void register(RegisterRequestDto request) {
        log.debug("Checking email uniqueness for email=[{}]", MaskingUtil.maskEmail(request.getEmail()));
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected — email already exists: [{}]", MaskingUtil.maskEmail(request.getEmail()));
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        log.debug("Building user entity for email=[{}] role=[{}]",
                MaskingUtil.maskEmail(request.getEmail()), request.getRole());

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.NEW);
        user.setRole(request.getRole() != null ? request.getRole() : Role.USER);

        List<Address> addresses = request.getAddresses().stream()
                .map(dto -> {
                    Address address = new Address();
                    address.setAddressLine1(dto.getAddressLine1());
                    address.setAddressLine2(dto.getAddressLine2());
                    address.setStreet(dto.getStreet());
                    address.setPostalCode(dto.getPostalCode());
                    address.setState(dto.getState());
                    address.setCountry(dto.getCountry());
                    address.setUser(user);
                    return address;
                })
                .collect(Collectors.toList());

        user.getAddresses().addAll(addresses);

        log.debug("Persisting user with [{}] address(es)", addresses.size());
        userRepository.save(user);
        log.info("User persisted successfully — email=[{}] status=[{}] role=[{}]",
                MaskingUtil.maskEmail(user.getEmail()), user.getStatus(), user.getRole());
    }
}
