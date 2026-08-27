package com.example.onlinebanking.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    private UUID id;
    @Column(name = "full_name", nullable = false, length = 100) private String fullName;
    @Column(name = "username", nullable = false, unique = true, length = 50) private String username;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Column(name = "date_of_birth", nullable = false) private LocalDate dateOfBirth;
    @Column(nullable = false, length = 100) private String street;
    @Column(name = "house_number", nullable = false, length = 20) private String houseNumber;
    @Column(name = "postal_code", nullable = false, length = 20) private String postalCode;
    @Column(nullable = false, length = 100) private String city;
    @Column(name = "country_code", nullable = false, length = 2) private String countryCode;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected Customer() { }

    public Customer(UUID id, String fullName, String username, String passwordHash, LocalDate dateOfBirth,
                    String street, String houseNumber, String postalCode, String city, String countryCode) {
        this.id = id; this.fullName = fullName; this.username = username; this.passwordHash = passwordHash;
        this.dateOfBirth = dateOfBirth; this.street = street; this.houseNumber = houseNumber;
        this.postalCode = postalCode; this.city = city; this.countryCode = countryCode; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
}
