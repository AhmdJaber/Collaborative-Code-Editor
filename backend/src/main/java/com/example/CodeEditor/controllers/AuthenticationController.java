package com.example.CodeEditor.controllers;

import com.example.CodeEditor.dto.AuthenticationDTO;
import com.example.CodeEditor.dto.RegisterDTO;
import com.example.CodeEditor.security.AuthenticationResponse;
import com.example.CodeEditor.services.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/register/{role}")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterDTO request, @PathVariable String role) {
        AuthenticationResponse response = authenticationService.register(request, role);
        if (response.getError() == null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/authenticate/{role}")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationDTO request, @PathVariable String role) {
        return ResponseEntity.ok(authenticationService.authenticate(request, role));
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return ResponseEntity.ok(authenticationService.refreshToken(request, response));
    }
}

