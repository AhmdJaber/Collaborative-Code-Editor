package com.example.CodeEditor.controllers;

import com.example.CodeEditor.dto.AuthenticationDTO;
import com.example.CodeEditor.dto.CurrentUserResponse;
import com.example.CodeEditor.dto.RegisterDTO;
import com.example.CodeEditor.security.AuthenticationResponse;
import com.example.CodeEditor.services.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterDTO request) {
        AuthenticationResponse response = authenticationService.register(request);
        if (response.getError() == null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationDTO request) {
        try {
            return ResponseEntity.ok(authenticationService.authenticate(request));
        } catch (AuthenticationException ex) {
            return ResponseEntity.badRequest().body(new AuthenticationResponse("Wrong username or password!"));
        }
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return ResponseEntity.ok(authenticationService.refreshToken(request, response));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        var client = authenticationService.getCurrentAuthenticatedClient(userDetails.getUsername());
        return ResponseEntity.ok(new CurrentUserResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getRole().name()
        ));
    }
}

