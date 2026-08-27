package com.example.onlinebanking.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Customer onboarding")
public class OnboardingController {

    @PostMapping("/register")
    @Operation(summary = "Register a customer and current account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer registered", content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed, invalid, underage, or unsupported-country request", content = @Content(schema = @Schema(implementation = ProblemResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username already exists", content = @Content(schema = @Schema(implementation = ProblemResponse.class))),
            @ApiResponse(responseCode = "500", description = "Account-number generation failed", content = @Content(schema = @Schema(implementation = ProblemResponse.class))),
            @ApiResponse(responseCode = "503", description = "Database busy", content = @Content(schema = @Schema(implementation = ProblemResponse.class)))
    })
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed or invalid request", content = @Content(schema = @Schema(implementation = ProblemResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ProblemResponse.class))),
            @ApiResponse(responseCode = "503", description = "Database busy", content = @Content(schema = @Schema(implementation = ProblemResponse.class)))
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("/overview")
    @Operation(summary = "Get the authenticated customer's current account", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account overview", content = @Content(schema = @Schema(implementation = OverviewResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication is required or token is invalid", content = @Content(schema = @Schema(implementation = ProblemResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found", content = @Content(schema = @Schema(implementation = ProblemResponse.class))),
            @ApiResponse(responseCode = "503", description = "Database busy", content = @Content(schema = @Schema(implementation = ProblemResponse.class)))
    })
    public ResponseEntity<OverviewResponse> overview() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
