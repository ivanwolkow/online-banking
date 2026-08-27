package com.example.onlinebanking.security;

import com.example.onlinebanking.config.AppProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    SecretKey jwtSecretKey(AppProperties properties) {
        byte[] secret = Base64.getDecoder().decode(properties.security().jwt().secretBase64());
        if (secret.length < 32) throw new IllegalStateException("JWT secret must decode to at least 32 bytes");
        return new SecretKeySpec(secret, "HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey jwtSecretKey) { return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey)); }
    @Bean JwtDecoder jwtDecoder(SecretKey jwtSecretKey, AppProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.security().jwt().issuer()));
        return decoder;
    }
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemAuthenticationEntryPoint entryPoint) throws Exception {
        return http.csrf(csrf -> csrf.disable()).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/register", "/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/overview").authenticated().anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(entryPoint).jwt(jwt -> { }))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint)).build();
    }
}
