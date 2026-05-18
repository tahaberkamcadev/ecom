package com.tahaberkamcadev.e_com.user_service.service;

import com.tahaberkamcadev.e_com.user_service.dto.request.ChangePasswordRequest;
import com.tahaberkamcadev.e_com.user_service.dto.request.UpdateUserRequest;
import com.tahaberkamcadev.e_com.user_service.dto.request.UpdateUserPreferencesRequest;
import com.tahaberkamcadev.e_com.user_service.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    List<UserResponse> getAllUsers();

    UserResponse getUserById(UUID id);

    UserResponse getCurrentUser();

    UserResponse updateCurrentUser(UpdateUserRequest request);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    void changePassword(ChangePasswordRequest request);

    void deleteUser(UUID id);
    
    UserResponse updateCurrentUserPreferences(UpdateUserPreferencesRequest request);
}
