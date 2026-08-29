package com.example.onlinebanking.service;

import com.example.onlinebanking.api.LoginRequest;
import com.example.onlinebanking.api.LoginResponse;
import com.example.onlinebanking.config.AppProperties;
import com.example.onlinebanking.exception.InvalidCredentialsException;
import com.example.onlinebanking.persistence.Customer;
import com.example.onlinebanking.persistence.CustomerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthenticationService {
    private final CustomerRepository customers;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final AppProperties properties;
    private final Clock clock;

    public AuthenticationService(
            CustomerRepository customers,
            PasswordEncoder encoder,
            JwtEncoder jwtEncoder,
            AppProperties properties,
            Clock clock
    ) {
        this.customers = customers;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        Customer customer = customers.findByUsername(username).orElse(null);

        if (customer == null || !encoder.matches(request.password(), customer.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.security().jwt().ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.security().jwt().issuer())
                .subject(customer.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("username", customer.getUsername())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new LoginResponse(accessToken, "Bearer", properties.security().jwt().ttl().toSeconds());
    }
}
