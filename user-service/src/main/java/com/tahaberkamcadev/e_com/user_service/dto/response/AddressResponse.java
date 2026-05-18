package com.tahaberkamcadev.e_com.user_service.dto.response;

import com.tahaberkamcadev.e_com.user_service.model.Address;
import com.tahaberkamcadev.e_com.user_service.model.AddressType;

import java.time.LocalDateTime;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        AddressType addressType,
        String title,
        String firstName,
        String lastName,
        String phone,
        String addressLine1,
        String addressLine2,
        String neighborhood,
        String district,
        String city,
        String postalCode,
        String country,
        boolean isDefault,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AddressResponse fromEntity(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getAddressType(),
                address.getTitle(),
                address.getFirstName(),
                address.getLastName(),
                address.getPhone(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getNeighborhood(),
                address.getDistrict(),
                address.getCity(),
                address.getPostalCode(),
                address.getCountry(),
                address.isDefault(),
                address.isActive(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}