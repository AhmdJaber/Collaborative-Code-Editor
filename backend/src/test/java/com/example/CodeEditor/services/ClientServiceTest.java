package com.example.CodeEditor.services;

import com.example.CodeEditor.enums.Role;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.Token;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.repository.ClientRepository;
import com.example.CodeEditor.repository.ProjectRepository;
import com.example.CodeEditor.services.storage.ClientStorageService;
import com.example.CodeEditor.services.storage.ProjectStorageService;
import com.example.CodeEditor.services.storage.PublicRepoStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientStorageService clientStorageService;

    @Mock
    private JwtService jwtService;

    @Mock
    private ProjectStorageService projectStorageService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private PublicRepoStorageService publicRepoStorageService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private ClientService clientService;

    @Test
    void getAllEditorsFiltersOnlyEditors() {
        Client editor = client(1L, "editor@example.com", Role.EDITOR);
        Client viewer = client(2L, "viewer@example.com", Role.VIEWER);
        when(clientRepository.findAll()).thenReturn(List.of(editor, viewer));

        assertEquals(List.of(editor), clientService.getAllEditors());
    }

    @Test
    void getClientToShareWithRejectsNonOwner() {
        Client owner = client(1L, "owner@example.com", Role.EDITOR);
        Client sender = client(2L, "sender@example.com", Role.EDITOR);
        when(jwtService.extractUsername("token")).thenReturn("sender@example.com");
        when(clientRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(sender));

        assertThrows(IllegalArgumentException.class,
                () -> clientService.getClientToShareWith("Bearer token", 1L, "target@example.com"));
        verify(clientRepository).findByEmail("sender@example.com");
    }

    @Test
    void deleteEditorDeletesProjectsTokensAndClient() {
        Client editor = client(1L, "editor@example.com", Role.EDITOR);
        Project project = project(10L, editor);
        when(clientRepository.existsById(1L)).thenReturn(true);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(editor));
        when(projectRepository.findByClient(editor)).thenReturn(List.of(project));
        when(projectStorageService.getAllSharedWith(editor.getId(), project.getId())).thenReturn(List.of());
        when(tokenService.findAllValidTokenClient(1L)).thenReturn(List.of(Token.builder().id(5L).build()));

        clientService.deleteEditor(1L);

        verify(tokenService).deleteAll(anyList());
        verify(clientStorageService).deleteClient(1L);
        verify(projectRepository).delete(project);
        verify(clientRepository).deleteById(1L);
    }

    private Client client(Long id, String email, Role role) {
        Client client = new Client();
        client.setId(id);
        client.setEmail(email);
        client.setName("Name");
        client.setRole(role);
        return client;
    }

    private Project project(Long id, Client client) {
        Project project = new Project();
        project.setId(id);
        project.setClient(client);
        project.setName("Project");
        return project;
    }
}
