package com.example.CodeEditor.security;

import com.example.CodeEditor.model.clients.Client;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class AuthenticationResponse {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("refresh_token")
    private String refreshToken;
    private Client client;
    private String error;

    public  AuthenticationResponse(String accessToken, String refreshToken, Client client) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.client = client;
    }
    public AuthenticationResponse(String error) {
        this.error = error;
    }
}
