package com.example.CodeEditor.services;

import com.example.CodeEditor.dto.RegisterDTO;
import com.example.CodeEditor.dto.AuthenticationDTO;
import com.example.CodeEditor.enums.Role;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.Token;
import com.example.CodeEditor.repository.ClientRepository;
import com.example.CodeEditor.repository.TokenRepository;
import com.example.CodeEditor.security.AuthenticationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ClientService clientService;

    @Autowired
    private TokenRepository tokenRepository;

    public AuthenticationResponse register(RegisterDTO request) {
        Client client = Client
                .builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.EDITOR)
                .build();
        Client savedClient;

        try {
            savedClient = clientRepository.saveAndFlush(client);
        } catch (DataIntegrityViolationException ex) {
            return new AuthenticationResponse("Email already in use");
        }

            clientService.addClient(client);
        String accessToken = jwtService.generateAccessToken(client);
        String refreshToken = jwtService.generateRefreshToken(client);
        saveClientToken(savedClient, accessToken);
        return new AuthenticationResponse(accessToken, refreshToken, client);
    }

    private void saveClientToken(Client client, String jwtToken) {
        Token token = Token
                .builder()
                .client(client)
                .token(jwtToken)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    public AuthenticationResponse authenticate(AuthenticationDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        Client client = clientRepository.findByEmail(request.getEmail()).orElseThrow();
        String accessToken = jwtService.generateAccessToken(client);
        String refreshToken = jwtService.generateRefreshToken(client);
        deleteAllClientExpiredTokens(client);
        saveClientToken(client, accessToken);
        return new AuthenticationResponse(accessToken, refreshToken, client);
    }

    public Client getCurrentAuthenticatedClient(String email) {
        return clientRepository.findByEmail(email).orElseThrow();
    }

    public void deleteAllClientExpiredTokens(Client client){
        List<Token> tokens = tokenRepository.findAllValidTokenClient(client.getId());
        if (tokens.isEmpty()){
            return;
        }
        tokenRepository.deleteAll(tokens);
    }

    public AuthenticationResponse refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        final String refreshToken = authorizationHeader.substring(7);
        final String email = jwtService.extractUsername(refreshToken);
        if (email != null) {
            Client client = this.clientRepository.findByEmail(email).orElseThrow();
            if (jwtService.isTokenValid(refreshToken, client)) {
                String accessToken = jwtService.generateAccessToken(client);
                deleteAllClientExpiredTokens(client);
                saveClientToken(client, accessToken);
                AuthenticationResponse authResponse = new AuthenticationResponse(accessToken, refreshToken, client);
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
                return authResponse;
            }
        }

        return null;
    }
}
