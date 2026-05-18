package com.tahaberkamcadev.e_com.user_service.service.impl;

import com.tahaberkamcadev.e_com.user_service.dto.request.CreateAddressRequest;
import com.tahaberkamcadev.e_com.user_service.dto.response.AddressResponse;
import com.tahaberkamcadev.e_com.user_service.exception.UnauthorizedAccessException;
import com.tahaberkamcadev.e_com.user_service.exception.UserNotFoundException;
import com.tahaberkamcadev.e_com.user_service.model.Address;
import com.tahaberkamcadev.e_com.user_service.model.AddressType;
import com.tahaberkamcadev.e_com.user_service.model.User;
import com.tahaberkamcadev.e_com.user_service.repository.AddressRepository;
import com.tahaberkamcadev.e_com.user_service.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getCurrentUserAddresses() {
        User authenticatedUser = getAuthenticatedUser();
        return addressRepository.findByUserAndIsActiveTrue(authenticatedUser)
                .stream()
                .map(AddressResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getCurrentUserAddressesByType(AddressType addressType) {
        User authenticatedUser = getAuthenticatedUser();
        return addressRepository.findByUserAndAddressTypeAndIsActiveTrue(authenticatedUser, addressType)
                .stream()
                .map(AddressResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(UUID id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Address not found: " + id));
        
        User authenticatedUser = getAuthenticatedUser();
        if (!address.getUser().getId().equals(authenticatedUser.getId())) {
            throw new UnauthorizedAccessException("Access denied to address: " + id);
        }
        
        return AddressResponse.fromEntity(address);
    }

    @Override
    @Transactional
    public AddressResponse createAddress(CreateAddressRequest request) {
        User authenticatedUser = getAuthenticatedUser();

        // If this is set as default, clear other defaults of the same type
        if (request.isDefault()) {
            addressRepository.clearDefaultAddressesByUserAndType(authenticatedUser, request.addressType());
        }

        Address address = Address.builder()
                .user(authenticatedUser)
                .addressType(request.addressType())
                .title(request.title().trim())
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phone(request.phone().trim())
                .addressLine1(request.addressLine1().trim())
                .addressLine2(request.addressLine2() != null ? request.addressLine2().trim() : null)
                .neighborhood(request.neighborhood().trim())
                .district(request.district().trim())
                .city(request.city().trim())
                .postalCode(request.postalCode().trim())
                .country(request.country().trim())
                .isDefault(request.isDefault())
                .build();

        Address savedAddress = addressRepository.save(address);
        log.info("New address created for user: {} with id: {}", authenticatedUser.getId(), savedAddress.getId());
        
        return AddressResponse.fromEntity(savedAddress);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID id, CreateAddressRequest request) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Address not found: " + id));
        
        User authenticatedUser = getAuthenticatedUser();
        if (!address.getUser().getId().equals(authenticatedUser.getId())) {
            throw new UnauthorizedAccessException("Access denied to address: " + id);
        }

        // If this is set as default, clear other defaults of the same type
        if (request.isDefault() && !address.isDefault()) {
            addressRepository.clearDefaultAddressesByUserAndType(authenticatedUser, request.addressType());
        }

        // Update address fields
        address.setAddressType(request.addressType());
        address.setTitle(request.title().trim());
        address.setFirstName(request.firstName().trim());
        address.setLastName(request.lastName().trim());
        address.setPhone(request.phone().trim());
        address.setAddressLine1(request.addressLine1().trim());
        address.setAddressLine2(request.addressLine2() != null ? request.addressLine2().trim() : null);
        address.setNeighborhood(request.neighborhood().trim());
        address.setDistrict(request.district().trim());
        address.setCity(request.city().trim());
        address.setPostalCode(request.postalCode().trim());
        address.setCountry(request.country().trim());
        address.setDefault(request.isDefault());

        Address savedAddress = addressRepository.save(address);
        log.info("Address updated for user: {} with id: {}", authenticatedUser.getId(), savedAddress.getId());
        
        return AddressResponse.fromEntity(savedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Address not found: " + id));
        
        User authenticatedUser = getAuthenticatedUser();
        if (!address.getUser().getId().equals(authenticatedUser.getId())) {
            throw new UnauthorizedAccessException("Access denied to address: " + id);
        }

        // Soft delete
        address.setActive(false);
        address.setDefault(false);
        addressRepository.save(address);
        
        log.info("Address soft deleted for user: {} with id: {}", authenticatedUser.getId(), id);
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(UUID id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Address not found: " + id));
        
        User authenticatedUser = getAuthenticatedUser();
        if (!address.getUser().getId().equals(authenticatedUser.getId())) {
            throw new UnauthorizedAccessException("Access denied to address: " + id);
        }

        // Clear other defaults of the same type
        addressRepository.clearDefaultAddressesByUserAndType(authenticatedUser, address.getAddressType());
        
        // Set this as default
        address.setDefault(true);
        Address savedAddress = addressRepository.save(address);
        
        log.info("Address set as default for user: {} with id: {}", authenticatedUser.getId(), id);
        return AddressResponse.fromEntity(savedAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(AddressType addressType) {
        User authenticatedUser = getAuthenticatedUser();
        return addressRepository.findByUserAndIsDefaultTrueAndAddressType(authenticatedUser, addressType)
                .map(AddressResponse::fromEntity)
                .orElseThrow(() -> new UserNotFoundException("No default " + addressType.name().toLowerCase() + " address found"));
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() 
            || !(authentication.getPrincipal() instanceof User user)) {
            throw new UnauthorizedAccessException("Authentication context not found");
        }
        return user;
    }
}