package com.example.CodeEditor.services;

import com.example.CodeEditor.enums.Change;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.FileItem;
import com.example.CodeEditor.model.component.files.FileNode;
import com.example.CodeEditor.model.component.files.Folder;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.model.component.files.ProjectStructure;
import com.example.CodeEditor.repository.ProjectRepository;
import com.example.CodeEditor.services.storage.ProjectStorageService;
import com.example.CodeEditor.services.storage.VCSStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    @Mock
    private VCSStorageService storageService;

    @Mock
    private FileItemService fileItemService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectStorageService projectStorageService;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private FolderService folderService;

    @Test
    void createFolderUpdatesStructureAndTracksChangeWhenVcsEnabled() {
        Client client = client(1L);
        Project project = project(10L, client);
        Folder folder = new Folder("src", 0L, null);
        FileItem saved = new FileItem();
        saved.setId(2L);
        saved.setName("src");
        saved.setParentId(0L);

        ProjectStructure structure = new ProjectStructure();
        when(clientService.getClientById(1L)).thenReturn(client);
        when(fileItemService.createFile(any())).thenReturn(saved);
        when(projectStorageService.loadProjectStructure(client, 10L)).thenReturn(structure);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(storageService.checkVCSProject(project)).thenReturn(true);
        when(storageService.getCurrentBranch(project)).thenReturn("main");

        Long id = folderService.createFolder(1L, folder, 10L);

        assertEquals(2L, id);
        verify(projectStorageService).saveProjectStructure(client, structure, 10L);
        verify(storageService).makeChange(project, "main", 'd', Change.CREATE, folder);
    }

    @Test
    void removeFolderUpdatesStructure() {
        Client client = client(1L);
        Project project = project(10L, client);
        Folder folder = new Folder("src", 0L, 2L);
        ProjectStructure structure = new ProjectStructure();
        structure.getTree().get(0L).getChildren().add(folder);
        structure.getTree().put(2L, new FileNode("src", new ArrayList<>(), 0L));

        when(clientService.getClientById(1L)).thenReturn(client);
        when(projectStorageService.loadProjectStructure(client, 10L)).thenReturn(structure);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(storageService.checkVCSProject(project)).thenReturn(false);

        folderService.removeFolder(1L, folder, 10L);

        assertEquals(0, structure.getTree().get(0L).getChildren().size());
        verify(projectStorageService).saveProjectStructure(client, structure, 10L);
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
