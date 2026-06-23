package com.example.CodeEditor.services.storage;

import com.example.CodeEditor.constants.FilesystemPaths;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.FileNode;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.model.component.files.ProjectStructure;
import com.example.CodeEditor.repository.ClientRepository;
import com.example.CodeEditor.repository.ProjectRepository;
import com.example.CodeEditor.utils.EncryptionUtil;
import com.example.CodeEditor.utils.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectStorageServiceTest {

    @Mock
    private EncryptionUtil encryptionUtil;

    @Spy
    private FileUtil fileUtil;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private PublicRepoStorageService publicRepoStorageService;

    @Mock
    private ClientRepository clientRepository;

    @Spy
    private final FilesystemPaths paths = new FilesystemPaths();

    @InjectMocks
    private ProjectStorageService projectStorageService;

    @TempDir
    Path tempDir;

    private Client client;
    private Project project;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(paths, "storageServicePath", tempDir.toString());
        ReflectionTestUtils.setField(fileUtil, "encryptionUtil", encryptionUtil);

        client = new Client();
        client.setId(1L);
        client.setName("Owner");
        client.setEmail("owner@example.com");

        project = new Project();
        project.setId(10L);
        project.setName("demo");
        project.setClient(client);

        Files.createDirectories(tempDir.resolve("1/projects"));
        Files.createDirectories(tempDir.resolve("2/shared"));
        Files.createDirectories(tempDir.resolve("3/shared"));
        Files.createDirectories(tempDir.resolve("2/shared_view"));
        Files.createDirectories(tempDir.resolve("3/shared_view"));
    }

    @Test
    void createProjectCreatesStorageLayoutAndStructureFile() {
        projectStorageService.createProject(client, project);

        Path projectPath = tempDir.resolve("1/projects/10");
        assertTrue(Files.isDirectory(projectPath));
        assertTrue(Files.isDirectory(projectPath.resolve("tree")));
        assertTrue(Files.isDirectory(projectPath.resolve("snippets")));
        assertTrue(Files.isDirectory(projectPath.resolve("comments")));
        assertTrue(Files.exists(projectPath.resolve("shared")));
        assertTrue(Files.exists(projectPath.resolve("shared_view")));
        assertTrue(Files.exists(projectPath.resolve("tree").resolve("_treeObject.ser")));
    }

    @Test
    void createProjectDeletesDuplicateProjectAndFails() {
        Path projectPath = tempDir.resolve("1/projects/10");
        try {
            Files.createDirectories(projectPath);
        } catch (Exception ignored) {
        }

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> projectStorageService.createProject(client, project));

        assertEquals("Project with name demo already exists", exception.getMessage());
        verify(projectRepository).delete(project);
    }

    @Test
    void saveAndLoadProjectStructureRoundTripsStructure() {
        ensureProjectDir();
        ProjectStructure structure = new ProjectStructure();
        structure.getTree().put(1L, new FileNode("src", new ArrayList<>(), 0L));

        projectStorageService.saveProjectStructure(client, structure, project.getId());
        ProjectStructure loaded = projectStorageService.loadProjectStructure(client, project.getId());

        assertEquals(structure.getTree().keySet(), loaded.getTree().keySet());
        assertEquals("src", loaded.getTree().get(1L).getName());
    }

    @Test
    void loadProjectStructureThrowsWhenNoStructureExists() {
        ensureProjectDir();
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> projectStorageService.loadProjectStructure(client, project.getId()));

        assertTrue(exception.getMessage().contains("Project directory not found"));
    }

    @Test
    void shareProjectWithEditAddsClientToSharedList() {
        ensureProjectDir();
        projectStorageService.shareProjectWithEdit(newClient(2L), project.getId(), client.getId());

        assertTrue(Files.exists(tempDir.resolve("2/shared/1_10")));
        List<Long> shared = castLongList(fileUtil.readObjectFromFile(
                tempDir.resolve("1/projects/10/shared").toString(), new ArrayList<>()));
        assertEquals(List.of(2L), shared);
    }

    @Test
    void shareProjectWithViewAddsClientToSharedViewList() {
        ensureProjectDir();
        projectStorageService.shareProjectWithView(newClient(3L), project.getId(), client.getId());

        assertTrue(Files.exists(tempDir.resolve("3/shared_view/1_10")));
        List<Long> shared = castLongList(fileUtil.readObjectFromFile(
                tempDir.resolve("1/projects/10/shared_view").toString(), new ArrayList<>()));
        assertEquals(List.of(3L), shared);
    }

    @Test
    void removesharedProjectDeletesLinkAndRemovesClientId() {
        ensureProjectDir();
        Path sharedFile = tempDir.resolve("1/projects/10/shared");
        fileUtil.writeObjectOnFile(new ArrayList<>(List.of(2L)), sharedFile.toString());
        try {
            Files.writeString(tempDir.resolve("2/shared/1_10"), "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        projectStorageService.removesharedProject(newClient(2L), project.getId());

        assertFalse(Files.exists(tempDir.resolve("2/shared/1_10")));
        List<Long> shared = castLongList(fileUtil.readObjectFromFile(sharedFile.toString(), new ArrayList<>()));
        assertEquals(List.of(), shared);
    }

    @Test
    void getAllSharedWithReturnsResolvedClients() {
        ensureProjectDir();
        Path sharedFile = tempDir.resolve("1/projects/10/shared");
        fileUtil.writeObjectOnFile(new ArrayList<>(List.of(2L, 3L)), sharedFile.toString());
        when(clientRepository.findById(2L)).thenReturn(Optional.of(newClient(2L)));
        when(clientRepository.findById(3L)).thenReturn(Optional.of(newClient(3L)));

        List<Client> sharedWith = projectStorageService.getAllSharedWith(client.getId(), project.getId());

        assertEquals(List.of(2L, 3L), sharedWith.stream().map(Client::getId).toList());
    }

    @Test
    void getSharedEditProjectsReturnsFileNames() throws IOException {
        ensureProjectDir();
        Path sharedPath = tempDir.resolve("1/shared");
        Files.createDirectories(sharedPath);
        try {
            Files.writeString(sharedPath.resolve("1_10"), "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<String> projects = projectStorageService.getSharedEditProjects(client);

        assertEquals(List.of("1_10"), projects);
    }

    @Test
    void deleteProjectRemovesProjectTreeAndSharedLinks() {
        ensureProjectDir();
        Path projectPath = tempDir.resolve("1/projects/10");
        Path sharedFile = projectPath.resolve("shared");
        fileUtil.writeObjectOnFile(new ArrayList<>(List.of(2L, 3L)), sharedFile.toString());
        try {
            Files.createDirectories(tempDir.resolve("2/shared"));
            Files.createDirectories(tempDir.resolve("3/shared"));
            Files.writeString(tempDir.resolve("2/shared/1_10"), "");
            Files.writeString(tempDir.resolve("3/shared/1_10"), "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        projectStorageService.deleteProject(client, project.getId());

        assertFalse(Files.exists(projectPath));
        assertFalse(Files.exists(tempDir.resolve("2/shared/1_10")));
        assertFalse(Files.exists(tempDir.resolve("3/shared/1_10")));
        verify(publicRepoStorageService).removeProjectFromPublic(project.getId());
    }

    private Client newClient(Long id) {
        Client c = new Client();
        c.setId(id);
        c.setName("Client " + id);
        c.setEmail("client" + id + "@example.com");
        return c;
    }

    private void ensureProjectDir() {
        try {
            Files.createDirectories(tempDir.resolve("1/projects/10"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Long> castLongList(Object value) {
        return (List<Long>) value;
    }
}
