package com.shop.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Address {
    @Id @GeneratedValue
    Long id;

    @Column(nullable = true)
    String addressLine1;

    @Column(nullable = true)
    String addressLine2;

    @Column(nullable = true)
    String street;

    @Column(nullable = true)
    String postalCode;

    @Column(nullable = false)
    String state;

    @Column(nullable = false)
    String country;
}
