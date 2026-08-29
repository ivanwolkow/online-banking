package com.example.onlinebanking.security;

import com.example.onlinebanking.api.ProblemResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;

@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;

    public ProblemAuthenticationEntryPoint(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        String code = request.getHeader("Authorization") == null ? "AUTHENTICATION_REQUIRED" : "INVALID_TOKEN";
        String type = "urn:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-');
        ProblemResponse problem = new ProblemResponse(
                type,
                "Authentication failed",
                HttpServletResponse.SC_UNAUTHORIZED,
                "A valid bearer token is required.",
                request.getRequestURI(),
                code,
                Instant.now(),
                null
        );

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), problem);
    }
}
