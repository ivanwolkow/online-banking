package com.example.onlinebanking.api;

import com.example.onlinebanking.model.LoginRequest;
import com.example.onlinebanking.model.LoginResponse;
import com.example.onlinebanking.model.OverviewResponse;
import com.example.onlinebanking.model.ProblemResponse;
import com.example.onlinebanking.model.RegisterRequest;
import com.example.onlinebanking.model.RegisterResponse;
import com.example.onlinebanking.service.AuthenticationService;
import com.example.onlinebanking.service.OverviewService;
import com.example.onlinebanking.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Customer onboarding")
public class OnboardingController {
    private final RegistrationService registrations;
    private final AuthenticationService authentication;
    private final OverviewService overviews;

    public OnboardingController(
            RegistrationService registrations,
            AuthenticationService authentication,
            OverviewService overviews
    ) {
        this.registrations = registrations;
        this.authentication = authentication;
        this.overviews = overviews;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a customer and current account")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Customer registered",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RegisterResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Malformed, invalid, or unacceptable request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)
                    )
            )
    })
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrations.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a customer")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authenticated",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Malformed or invalid request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)
                    )
            )
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authentication.login(request));
    }

    @GetMapping("/overview")
    @Operation(
            summary = "Get the authenticated customer's current account",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account overview",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OverviewResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required or token is invalid",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemResponse.class)
                    )
            )
    })
    public ResponseEntity<OverviewResponse> overview(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(overviews.overview(UUID.fromString(jwt.getSubject())));
    }
}
