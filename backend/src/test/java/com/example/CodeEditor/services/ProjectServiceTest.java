package com.example.CodeEditor.services;

import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.services.storage.ProjectStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private com.example.CodeEditor.repository.ProjectRepository projectRepository;

    @Mock
    private ProjectStorageService projectStorageService;

    @Mock
    private JwtService jwtService;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createBuildsProjectAndCreatesStorage() {
        Client client = client(1L, "owner@example.com");
        Project saved = project(10L, "Demo", client);
        when(jwtService.extractUsername("token")).thenReturn("owner@example.com");
        when(clientService.getClientByEmail("owner@example.com")).thenReturn(client);
        when(projectRepository.save(any(Project.class))).thenReturn(saved);

        Project result = projectService.create(Map.of("projectName", "Demo"), "Bearer token");

        assertEquals(saved, result);
        verify(projectStorageService).createProject(client, saved);
    }

    @Test
    void deleteProjectReturnsForbiddenMessageForNonOwner() {
        Client sender = client(2L, "sender@example.com");
        Client owner = client(1L, "owner@example.com");
        when(jwtService.extractUsername("token")).thenReturn("sender@example.com");
        when(clientService.getClientByEmail("sender@example.com")).thenReturn(sender);

        assertEquals("You aren't allowed to delete this project",
                projectService.deleteProject(10L, 1L, "Bearer token"));
    }

    @Test
    void getSharedEditProjectsResolvesIdsFromStorageNames() {
        Client client = client(1L, "owner@example.com");
        when(projectStorageService.getSharedEditProjects(client)).thenReturn(List.of("1_10", "1_20"));
        Project first = project(10L, "A", client);
        Project second = project(20L, "B", client);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(first));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(second));

        assertEquals(List.of(10L, 20L),
                projectService.getSharedEditProjects(client).stream().map(Project::getId).toList());
    }

    private Client client(Long id, String email) {
        Client client = new Client();
        client.setId(id);
        client.setEmail(email);
        client.setName("Client");
        return client;
    }

    private Project project(Long id, String name, Client client) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setClient(client);
        return project;
    }
}
