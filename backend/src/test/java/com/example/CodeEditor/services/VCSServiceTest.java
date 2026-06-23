package com.example.CodeEditor.services;

import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.ChangeHolder;
import com.example.CodeEditor.model.component.files.FileItem;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.services.storage.FileStorageService;
import com.example.CodeEditor.services.storage.VCSStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VCSServiceTest {

    @Mock
    private VCSStorageService vcsStorageService;

    @Mock
    private com.example.CodeEditor.repository.ProjectRepository projectRepository;

    @Mock
    private FileItemService fileItemService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private JwtService jwtService;

    @Mock
    private ClientService clientService;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private VCSService vcsService;

    @Test
    void statusListsTrackedAndUntrackedFiles() {
        Client client = client(1L);
        Project project = project(10L, client);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(vcsStorageService.getCurrentBranch(project)).thenReturn("main");
        when(vcsStorageService.readChanges(project, "main")).thenReturn(Map.of(1L, ChangeHolder.builder().build()));
        when(vcsStorageService.readTracked(project, "main")).thenReturn(Map.of(2L, ChangeHolder.builder().build()));
        when(fileItemService.getFileById(1L)).thenReturn(fileItem(1L, "A.java"));
        when(fileItemService.getFileById(2L)).thenReturn(fileItem(2L, "B.java"));

        Map<String, List<String>> status = vcsService.status(10L);

        assertEquals(List.of("A.java"), status.get("untracked"));
        assertEquals(List.of("B.java"), status.get("tracked"));
    }

    @Test
    void addTracksSingleFileByPath() throws Exception {
        Client client = client(1L);
        Project project = project(10L, client);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(vcsStorageService.getCurrentBranch(project)).thenReturn("main");
        when(fileStorageService.getFileIdByPath(project, "/src/Main.java")).thenReturn(1L);
        when(vcsStorageService.trackChanges(project, "main", List.of(1L))).thenReturn(Map.of());

        List<String> result = vcsService.add(10L, List.of("/src/Main.java"));

        assertEquals(List.of("/src/Main.java"), result);
    }

    @Test
    void commitReturnsTrackedChangeNames() {
        Client client = client(1L);
        Project project = project(10L, client);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(vcsStorageService.getCurrentBranch(project)).thenReturn("main");
        when(vcsStorageService.getCurrentCommit(project, "main")).thenReturn("c1");
        when(vcsStorageService.commitTracked(project, "main", client, "msg", "c1"))
                .thenReturn(Map.of(1L, ChangeHolder.builder().build()));
        when(fileItemService.getFileById(1L)).thenReturn(fileItem(1L, "A.java"));

        List<String> result = vcsService.commit(10L, client, "msg");

        assertEquals(List.of("A.java"), result);
    }

    @Test
    void forkRejectsOwnerForkingOwnProject() {
        Client client = client(1L);
        Project project = project(10L, client);
        when(jwtService.extractUsername("token")).thenReturn("owner@example.com");
        when(clientService.getClientByEmail("owner@example.com")).thenReturn(client);
        when(projectService.getProjectById(10L)).thenReturn(project);

        ResponseEntity<String> response = vcsService.fork(10L, "Bearer token");

        assertTrue(response.getStatusCode().is4xxClientError());
        assertEquals("Cannot fork projects you own", response.getBody());
    }

    private Client client(Long id) {
        Client client = new Client();
        client.setId(id);
        client.setName("Client");
        client.setEmail("owner@example.com");
        return client;
    }

    private Project project(Long id, Client client) {
        Project project = new Project();
        project.setId(id);
        project.setClient(client);
        project.setName("Project");
        return project;
    }

    private FileItem fileItem(Long id, String name) {
        FileItem fileItem = new FileItem();
        fileItem.setId(id);
        fileItem.setName(name);
        return fileItem;
    }
}
