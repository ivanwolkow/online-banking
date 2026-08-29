package com.example.onlinebanking.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Customer residential address")
public record AddressRequest(
        @NotBlank @Size(max = 100) @Schema(example = "Keizersgracht") String street,
        @NotBlank @Size(max = 20) @Schema(example = "123A") String houseNumber,
        @NotBlank @Size(max = 20) @Schema(example = "1015 CJ") String postalCode,
        @NotBlank @Size(max = 100) @Schema(example = "Amsterdam") String city,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") @Schema(example = "NL") String countryCode) {

    public AddressRequest {
        street = trim(street);
        houseNumber = trim(houseNumber);
        postalCode = trim(postalCode);
        city = trim(city);
        countryCode = trim(countryCode);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
