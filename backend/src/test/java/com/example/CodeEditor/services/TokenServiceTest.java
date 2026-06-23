package com.example.CodeEditor.services;

import com.example.CodeEditor.model.component.Token;
import com.example.CodeEditor.repository.TokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenService tokenService;

    @Test
    void findAllValidTokenClientDelegates() {
        List<Token> tokens = List.of(Token.builder().token("a").build());
        when(tokenRepository.findAllValidTokenClient(1L)).thenReturn(tokens);

        assertEquals(tokens, tokenService.findAllValidTokenClient(1L));
        verify(tokenRepository).findAllValidTokenClient(1L);
    }
}
