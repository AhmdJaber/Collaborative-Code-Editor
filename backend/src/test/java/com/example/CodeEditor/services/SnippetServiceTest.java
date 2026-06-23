package com.example.CodeEditor.services;

import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.FileItem;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.model.component.files.ProjectStructure;
import com.example.CodeEditor.model.component.files.Snippet;
import com.example.CodeEditor.repository.ProjectRepository;
import com.example.CodeEditor.services.storage.ProjectStorageService;
import com.example.CodeEditor.services.storage.SnippetStorageService;
import com.example.CodeEditor.services.storage.VCSStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnippetServiceTest {

    @Mock
    private VCSStorageService storageService;

    @Mock
    private FileItemService fileItemService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SnippetStorageService snippetStorageService;

    @Mock
    private ProjectStorageService projectStorageService;

    @Mock
    private ClientService clientService;

    @Mock
    private ProjectService projectService;

    @Mock
    private JwtService jwtService;

    @Mock
    private CodeExecutionService codeExecutionService;

    @InjectMocks
    private SnippetService snippetService;

    @Test
    void createSnippetSavesSnippetAndTracksVcs() throws Exception {
        Client client = client(1L);
        Project project = project(10L, client);
        Snippet snippet = new Snippet("Main.java", 0L, null);
        FileItem saved = new FileItem();
        saved.setId(2L);
        saved.setName("Main.java");
        saved.setParentId(0L);
        ProjectStructure structure = new ProjectStructure();
        when(clientService.getClientById(1L)).thenReturn(client);
        when(fileItemService.createFile(any())).thenReturn(saved);
        when(projectStorageService.loadProjectStructure(client, 10L)).thenReturn(structure);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(storageService.checkVCSProject(project)).thenReturn(true);
        when(storageService.getCurrentBranch(project)).thenReturn("main");

        Long id = snippetService.createSnippet(1L, snippet, 10L);

        assertEquals(2L, id);
        verify(snippetStorageService).createSnippet(client, snippet, 10L);
        verify(storageService).makeChange(project, "main", '-', com.example.CodeEditor.enums.Change.CREATE, snippet);
    }

    @Test
    void updateSnippetDelegatesToStorage() {
        Client client = client(1L);
        Project project = project(10L, client);
        FileItem fileItem = new FileItem();
        fileItem.setId(2L);
        fileItem.setName("Main.java");

        when(clientService.getClientById(1L)).thenReturn(client);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(fileItemService.getFileById(2L)).thenReturn(fileItem);
        when(storageService.checkVCSProject(project)).thenReturn(false);

        snippetService.updateSnippet(1L, 2L, "Main.java", "new", 10L);

        verify(snippetStorageService).updateSnippet(client, 2L, "Main.java", "new", 10L);
    }

    @Test
    void executeCodeDelegatesToExecutionService() {
        when(codeExecutionService.executeCode("code", "java")).thenReturn("ok");

        assertEquals("ok", snippetService.executeCode(Map.of("code", "code", "language", "java")));
    }

    private Client client(Long id) {
        Client client = new Client();
        client.setId(id);
        client.setName("Client");
        client.setEmail("client@example.com");
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
