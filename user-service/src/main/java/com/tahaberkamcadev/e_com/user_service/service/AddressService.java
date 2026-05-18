package com.tahaberkamcadev.e_com.user_service.service;

import com.tahaberkamcadev.e_com.user_service.dto.request.CreateAddressRequest;
import com.tahaberkamcadev.e_com.user_service.dto.response.AddressResponse;
import com.tahaberkamcadev.e_com.user_service.model.AddressType;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    List<AddressResponse> getCurrentUserAddresses();
    
    List<AddressResponse> getCurrentUserAddressesByType(AddressType addressType);
    
    AddressResponse getAddressById(UUID id);
    
    AddressResponse createAddress(CreateAddressRequest request);
    
    AddressResponse updateAddress(UUID id, CreateAddressRequest request);
    
    void deleteAddress(UUID id);
    
    AddressResponse setDefaultAddress(UUID id);
    
    AddressResponse getDefaultAddress(AddressType addressType);
}