package com.smartdine.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smartdine.dto.AuthResponse;
import com.smartdine.dto.LoginRequest;
import com.smartdine.dto.PinLoginRequest;
import com.smartdine.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticateUser(request));
    }

    @PostMapping("/pin-login")
    public ResponseEntity<AuthResponse> pinLogin(@RequestBody PinLoginRequest request) {
        return ResponseEntity.ok(authService.authenticateWithPin(request));
    }
}