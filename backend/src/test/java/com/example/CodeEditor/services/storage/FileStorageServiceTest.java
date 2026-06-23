package com.example.CodeEditor.services.storage;

import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.FileItem;
import com.example.CodeEditor.model.component.files.FileNode;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.model.component.files.ProjectStructure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private ProjectStorageService projectStorageService;

    private FileStorageService fileStorageService;

    private Client client;
    private Project project;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(projectStorageService);

        client = new Client();
        client.setId(1L);
        client.setEmail("owner@example.com");
        client.setName("Owner");

        project = new Project();
        project.setId(10L);
        project.setClient(client);
        project.setName("Demo");
    }

    @Test
    void getFileIdByPathResolvesNestedFile() {
        ProjectStructure structure = new ProjectStructure();
        FileItem srcFolder = new FileItem();
        srcFolder.setId(1L);
        srcFolder.setName("src");
        srcFolder.setParentId(0L);

        FileItem mainFile = new FileItem();
        mainFile.setId(2L);
        mainFile.setName("main.java");
        mainFile.setParentId(1L);

        structure.getTree().get(0L).getChildren().add(srcFolder);
        structure.getTree().put(1L, new FileNode("src", new ArrayList<>(List.of(mainFile)), 0L));
        structure.getTree().put(2L, new FileNode("main.java", new ArrayList<>(), 1L));
        when(projectStorageService.loadProjectStructure(client, project.getId())).thenReturn(structure);

        Long fileId = fileStorageService.getFileIdByPath(project, "/src/main.java");

        assertEquals(2L, fileId);
    }

    @Test
    void getFileIdByPathThrowsWhenPathDoesNotExist() {
        ProjectStructure structure = new ProjectStructure();
        FileItem srcFolder = new FileItem();
        srcFolder.setId(1L);
        srcFolder.setName("src");
        srcFolder.setParentId(0L);
        structure.getTree().get(0L).getChildren().add(srcFolder);
        structure.getTree().put(1L, new FileNode("src", new ArrayList<>(), 0L));
        when(projectStorageService.loadProjectStructure(client, project.getId())).thenReturn(structure);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.getFileIdByPath(project, "/missing/main.java"));

        assertEquals("No file with name missingin the directory ", exception.getMessage());
    }
}
