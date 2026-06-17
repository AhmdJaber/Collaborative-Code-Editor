package com.example.CodeEditor.services;

import com.example.CodeEditor.model.component.Token;
import com.example.CodeEditor.repository.TokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {
    @Autowired
    private final TokenRepository tokenRepository;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String jwt = authHeader.substring(7);
        Token token = tokenRepository.findByToken(jwt).orElseThrow(() -> new RuntimeException("Token not found"));
        if (token != null) {
            token.setExpired(true);
            token.setRevoked(true);
            tokenRepository.save(token);
        }
    }
}
