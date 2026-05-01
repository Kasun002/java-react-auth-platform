package com.shop.auth.entity;

import java.util.ArrayList;
import java.util.List;

import com.shop.auth.utils.UserStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Size;

@Entity
public class User {
    @Id
    @GeneratedValue
    Long id;

    @Column(nullable = true)
    String name;

    @Column(nullable = true)
    String email;

    @Column(nullable = true)
    String phone;

    @Column(nullable = true)
    String password;

    @OneToMany(
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @JoinColumn(name = "user_id") 
    @Size(min = 1)
    private List<Address> addresses = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    UserStatus status;
}