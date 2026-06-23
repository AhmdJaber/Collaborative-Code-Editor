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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private ClientService clientService;

    @Mock
    private ProjectStorageService projectStorageService;

    @InjectMocks
    private AdminService adminService;

    @Test
    void getAllEditorsDelegatesToClientService() {
        List<Client> editors = List.of(client(1L, "a@example.com"));
        when(clientService.getAllEditors()).thenReturn(editors);

        assertEquals(editors, adminService.getAllEditors());
        verify(clientService).getAllEditors();
    }

    @Test
    void removeEditorProjectDeletesProjectAndStorage() {
        Client client = client(1L, "a@example.com");
        when(clientService.getClientById(1L)).thenReturn(client);

        adminService.removeEditorProject(1L, 10L);

        verify(clientService).getClientById(1L);
        verify(projectService).deleteProjectById(10L);
        verify(projectStorageService).deleteProject(client, 10L);
    }

    @Test
    void getSharedProjectsDelegatesToProjectService() {
        Client client = client(1L, "a@example.com");
        List<Project> shared = List.of(project(10L, client));
        when(clientService.getClientById(1L)).thenReturn(client);
        when(projectService.getSharedEditProjects(client)).thenReturn(shared);

        assertEquals(shared, adminService.getSharedProjects(1L));
        verify(projectService).getSharedEditProjects(client);
    }

    private Client client(Long id, String email) {
        Client client = new Client();
        client.setId(id);
        client.setEmail(email);
        client.setName("Client");
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
