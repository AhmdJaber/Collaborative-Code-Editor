package com.example.CodeEditor.services;

import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.Token;
import com.example.CodeEditor.repository.TokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private LogoutService logoutService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Test
    void logoutMarksTokenAsRevokedAndExpired() {
        Token token = Token.builder().token("jwt").expired(false).revoked(false).client(new Client()).build();
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt");
        when(tokenRepository.findByToken("jwt")).thenReturn(Optional.of(token));

        logoutService.logout(request, response, authentication);

        assertTrue(token.isExpired());
        assertTrue(token.isRevoked());
        verify(tokenRepository).save(token);
    }

    @Test
    void logoutReturnsWhenNoBearerHeader() {
        when(request.getHeader("Authorization")).thenReturn(null);

        logoutService.logout(request, response, authentication);

        verify(tokenRepository, never()).findByToken("jwt");
    }
}
