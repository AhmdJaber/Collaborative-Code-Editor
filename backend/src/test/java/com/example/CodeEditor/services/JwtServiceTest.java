package com.example.CodeEditor.services;

import com.example.CodeEditor.enums.Role;
import com.example.CodeEditor.model.clients.Client;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    @Test
    void generateAndExtractUsernameRoundTrips() {
        Client client = client("owner@example.com");
        String token = jwtService.generateAccessToken(client);

        assertEquals("owner@example.com", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, client));
    }

    @Test
    void refreshTokenIsNotExpiredImmediately() {
        Client client = client("owner@example.com");
        String token = jwtService.generateRefreshToken(client);

        assertFalse(jwtService.isTokenExpired(token));
    }

    private Client client(String email) {
        Client client = new Client();
        client.setEmail(email);
        client.setName("Owner");
        client.setRole(Role.EDITOR);
        return client;
    }
}
