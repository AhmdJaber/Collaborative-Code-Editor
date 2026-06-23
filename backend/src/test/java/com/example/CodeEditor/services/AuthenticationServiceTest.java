package com.example.CodeEditor.services;

import com.example.CodeEditor.dto.AuthenticationDTO;
import com.example.CodeEditor.dto.RegisterDTO;
import com.example.CodeEditor.enums.Role;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.Token;
import com.example.CodeEditor.repository.ClientRepository;
import com.example.CodeEditor.repository.TokenRepository;
import com.example.CodeEditor.security.AuthenticationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ClientService clientService;

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void registerCreatesEditorAndToken() {
        RegisterDTO request = RegisterDTO.builder()
                .name("Owner")
                .email("owner@example.com")
                .password("secret")
                .build();
        Client savedClient = client("owner@example.com", Role.EDITOR);
        savedClient.setId(1L);

        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(clientRepository.saveAndFlush(any(Client.class))).thenReturn(savedClient);
        when(jwtService.generateAccessToken(any(Client.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(Client.class))).thenReturn("refresh");

        AuthenticationResponse response = authenticationService.register(request, "EDITOR");

        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        assertEquals(savedClient.getEmail(), response.getClient().getEmail());
        verify(clientService).addClient(any(Client.class));
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void authenticateReturnsNullWhenRoleDoesNotMatch() {
        AuthenticationDTO request = AuthenticationDTO.builder()
                .email("owner@example.com")
                .password("secret")
                .build();
        Client client = client("owner@example.com", Role.VIEWER);
        when(clientRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(client));

        Object response = authenticationService.authenticate(request, "EDITOR");

        assertNull(response);
    }

    @Test
    void authenticateReturnsTokensWhenRoleMatches() {
        AuthenticationDTO request = AuthenticationDTO.builder()
                .email("owner@example.com")
                .password("secret")
                .build();
        Client client = client("owner@example.com", Role.EDITOR);
        client.setId(1L);

        when(clientRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(client));
        when(jwtService.generateAccessToken(client)).thenReturn("access");
        when(jwtService.generateRefreshToken(client)).thenReturn("refresh");
        when(tokenRepository.findAllValidTokenClient(1L)).thenReturn(List.of(Token.builder().id(1L).build()));

        AuthenticationResponse response = authenticationService.authenticate(request, "EDITOR");

        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        verify(tokenRepository).deleteAll(anyList());
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void refreshTokenReturnsAuthenticationResponseWhenValid() throws Exception {
        Client client = client("owner@example.com", Role.EDITOR);
        client.setId(1L);
        String refreshToken = "refresh-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken);

        when(jwtService.extractUsername(refreshToken)).thenReturn("owner@example.com");
        when(clientRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(client));
        when(jwtService.isTokenValid(refreshToken, client)).thenReturn(true);
        when(jwtService.generateAccessToken(client)).thenReturn("access");
        when(tokenRepository.findAllValidTokenClient(client.getId())).thenReturn(List.of());

        AuthenticationResponse result = authenticationService.refreshToken(request, response);

        assertNotNull(result);
        assertEquals("access", result.getAccessToken());
        assertEquals(refreshToken, result.getRefreshToken());
        assertTrue(response.getContentAsString().contains("access_token"));
        verify(tokenRepository).save(any(Token.class));
    }

    private Client client(String email, Role role) {
        Client client = new Client();
        client.setEmail(email);
        client.setName("Owner");
        client.setRole(role);
        client.setPassword("encoded");
        return client;
    }
}
