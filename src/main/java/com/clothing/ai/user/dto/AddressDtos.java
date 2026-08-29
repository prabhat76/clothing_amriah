package com.clothing.ai.user.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public class AddressDtos {
    public record AddressRequest(
            @Size(max = 50) String label,
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 30) String phone,
            @NotBlank @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @NotBlank @Size(max = 100) String city,
            @Size(max = 100) String stateProvince,
            @NotBlank @Size(max = 30) String postalCode,
            @NotBlank @Size(max = 80) String country,
            boolean defaultAddress
    ) {}

    public record AddressResponse(
            UUID id, String label, String fullName, String phone, String line1, String line2,
            String city, String stateProvince, String postalCode, String country, boolean defaultAddress
    ) {}
}
