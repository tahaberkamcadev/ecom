package com.tahaberkamcadev.e_com.user_service.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tahaberkamcadev.e_com.user_service.dto.response.UserNameResponse;
import com.tahaberkamcadev.e_com.user_service.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{id}/name")
    public ResponseEntity<UserNameResponse> getUserName(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserNameById(id));
    }
}
