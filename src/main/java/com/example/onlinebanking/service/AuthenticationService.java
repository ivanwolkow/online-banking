package com.example.onlinebanking.service;

import com.example.onlinebanking.api.LoginRequest;
import com.example.onlinebanking.api.LoginResponse;
import com.example.onlinebanking.config.AppProperties;
import com.example.onlinebanking.domain.UsernameNormalizer;
import com.example.onlinebanking.persistence.Customer;
import com.example.onlinebanking.persistence.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class AuthenticationService {
    private final CustomerRepository customers; private final PasswordEncoder encoder; private final JwtEncoder jwtEncoder;
    private final AppProperties properties; private final Clock clock;
    public AuthenticationService(CustomerRepository customers, PasswordEncoder encoder, JwtEncoder jwtEncoder, AppProperties properties, Clock clock) {
        this.customers = customers; this.encoder = encoder; this.jwtEncoder = jwtEncoder; this.properties = properties; this.clock = clock;
    }
    public LoginResponse login(LoginRequest request) {
        Customer customer = customers.findByUsername(UsernameNormalizer.normalize(request.username())).orElse(null);
        if (customer == null || !encoder.matches(request.password(), customer.getPasswordHash()))
            throw new DomainException("INVALID_CREDENTIALS", "The username or password is incorrect.");
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.security().jwt().ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.security().jwt().issuer()).subject(customer.getId().toString())
                .issuedAt(issuedAt).expiresAt(expiresAt).claim("username", customer.getUsername()).build();
        return new LoginResponse(jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue(), "Bearer", properties.security().jwt().ttl().toSeconds());
    }
}
