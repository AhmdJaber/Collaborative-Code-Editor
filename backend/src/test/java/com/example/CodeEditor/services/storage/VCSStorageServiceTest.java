package com.example.CodeEditor.services.storage;

import com.example.CodeEditor.constants.FilesystemPaths;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.ChangeHolder;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.model.component.files.ProjectStructure;
import com.example.CodeEditor.repository.FileItemRepository;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VCSStorageServiceTest {

    @Mock
    private EncryptionUtil encryptionUtil;

    @Spy
    private FileUtil fileUtil;

    @Mock
    private FileItemRepository fileItemRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private PublicRepoStorageService publicRepoStorageService;

    @Mock
    private ProjectStorageService projectStorageService;

    @Spy
    private final FilesystemPaths paths = new FilesystemPaths();

    private VCSStorageService vcsStorageService;

    @TempDir
    Path tempDir;

    private Client client;
    private Project project;

    @BeforeEach
    void setUp() throws Exception {
        paths.storageServicePath = tempDir.toString();
        ReflectionTestUtils.setField(fileUtil, "encryptionUtil", encryptionUtil);
        vcsStorageService = new VCSStorageService();
        ReflectionTestUtils.setField(vcsStorageService, "fileUtil", fileUtil);
        ReflectionTestUtils.setField(vcsStorageService, "fileItemRepository", fileItemRepository);
        ReflectionTestUtils.setField(vcsStorageService, "projectRepository", projectRepository);
        ReflectionTestUtils.setField(vcsStorageService, "publicRepoStorageService", publicRepoStorageService);
        ReflectionTestUtils.setField(vcsStorageService, "projectStorageService", projectStorageService);
        ReflectionTestUtils.setField(vcsStorageService, "paths", paths);

        client = new Client();
        client.setId(1L);
        client.setName("Owner");
        client.setEmail("owner@example.com");

        project = new Project();
        project.setId(10L);
        project.setName("demo");
        project.setClient(client);

        Files.createDirectories(tempDir.resolve("1/projects/10"));
        Files.createDirectories(tempDir.resolve("1/projects/10/snippets"));
        Files.createDirectories(tempDir.resolve("1/projects/10/tree"));
    }

    @Test
    void initVCSCreatesBootstrapFiles() {
        when(projectStorageService.loadProjectStructure(client, project.getId()))
                .thenReturn(new ProjectStructure());

        vcsStorageService.initVCS(project);

        Path vcsPath = tempDir.resolve("1/projects/10/.vcs");
        assertTrue(Files.isDirectory(vcsPath));
        assertTrue(Files.exists(vcsPath.resolve("HEAD")));
        assertEquals("main", fileUtil.readFileContents(vcsPath.resolve("HEAD").toString()));
        assertTrue(Files.exists(vcsPath.resolve("config")));
        assertTrue(Files.isDirectory(vcsPath.resolve("branches/main/commits")));
        assertTrue(Files.exists(vcsPath.resolve("branches/main/currentCommit")));
        assertTrue(Files.exists(vcsPath.resolve("branches/main/log")));
    }

    @Test
    void initVCSThrowsWhenAlreadyInitialized() {
        try {
            Files.createDirectories(tempDir.resolve("1/projects/10/.vcs"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> vcsStorageService.initVCS(project));

        assertTrue(exception.getMessage().contains("Project is already a vcs directory"));
    }

    @Test
    void checkVCSProjectReturnsFalseBeforeInitialization() {
        assertFalse(vcsStorageService.checkVCSProject(project));
    }

    @Test
    void createBranchCreatesBranchLayout() {
        FilesystemInit.createDirectories(tempDir.resolve("1/projects/10/.vcs"));

        vcsStorageService.createBranch(project, "feature");

        Path branchPath = tempDir.resolve("1/projects/10/.vcs/branches/feature");
        assertTrue(Files.isDirectory(branchPath));
        assertTrue(Files.exists(branchPath.resolve("changes")));
        assertTrue(Files.exists(branchPath.resolve("tracked")));
        assertTrue(Files.exists(branchPath.resolve("log")));
        assertTrue(Files.exists(branchPath.resolve("currentCommit")));
    }

    @Test
    void createBranchThrowsWhenBranchAlreadyExists() {
        FilesystemInit.createDirectories(tempDir.resolve("1/projects/10/.vcs/branches/main"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> vcsStorageService.createBranch(project, "main"));

        assertTrue(exception.getMessage().contains("Branch main already exists"));
    }

    @Test
    void getCurrentBranchReadsHeadFile() {
        Path head = tempDir.resolve("1/projects/10/.vcs/HEAD");
        fileUtil.createFolderIfNotExists(head.getParent().toString());
        fileUtil.createFile(head.toString(), "feature");

        assertEquals("feature", vcsStorageService.getCurrentBranch(project));
    }

    @Test
    void getCurrentBranchThrowsWhenHeadIsMissing() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> vcsStorageService.getCurrentBranch(project));

        assertTrue(exception.getMessage().contains("Couldn't get the current branch"));
    }

    @Test
    void deleteBranchRejectsMainBranch() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> vcsStorageService.deleteBranch(project, "main"));

        assertTrue(exception.getMessage().contains("Couldn't delete the defualt branch 'main'"));
    }

    @Test
    void checkoutRejectsMissingBranch() {
        FilesystemInit.createDirectories(tempDir.resolve("1/projects/10/.vcs/branches/main"));
        fileUtil.createFile(tempDir.resolve("1/projects/10/.vcs/branches/main/currentCommit").toString(), "commit-1");
        fileUtil.createFile(tempDir.resolve("1/projects/10/.vcs/HEAD").toString(), "main");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> vcsStorageService.checkout(project, "feature"));

        assertTrue(exception.getMessage().contains("Branch feature does not exist"));
    }

    @Test
    void checkoutUpdatesHeadAndInvokesRevert() {
        FilesystemInit.createDirectories(tempDir.resolve("1/projects/10/.vcs/branches/main"));
        FilesystemInit.createDirectories(tempDir.resolve("1/projects/10/.vcs/branches/feature"));
        FilesystemInit.createDirectories(tempDir.resolve("1/projects/10/.vcs/branches/feature/commits/commit-feature/tree"));
        FilesystemInit.createDirectories(tempDir.resolve("1/projects/10/.vcs/branches/feature/commits/commit-feature/snippets"));
        fileUtil.createFile(tempDir.resolve("1/projects/10/.vcs/branches/main/currentCommit").toString(), "commit-main");
        fileUtil.createFile(tempDir.resolve("1/projects/10/.vcs/branches/feature/currentCommit").toString(), "commit-feature");
        fileUtil.createFile(tempDir.resolve("1/projects/10/.vcs/HEAD").toString(), "main");
        fileUtil.writeObjectOnFile(new ProjectStructure(),
                tempDir.resolve("1/projects/10/.vcs/branches/feature/commits/commit-feature/tree/_treeObject.ser").toString());

        vcsStorageService.checkout(project, "feature");

        assertEquals("feature", fileUtil.readFileContents(tempDir.resolve("1/projects/10/.vcs/HEAD").toString()));
        assertTrue(Files.exists(tempDir.resolve("1/projects/10/.vcs/branches/feature/currentCommit")));
    }

    @Test
    void forkRejectsNonPublicProject() {
        when(publicRepoStorageService.checkProjectPublic(project.getId())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> vcsStorageService.fork(project, newClient(2L)));

        assertTrue(exception.getMessage().contains("Cannot fork this project"));
    }

    @Test
    void readChangesAndTrackChangesAreBackedBySerializedFiles() {
        Path vcsBranch = tempDir.resolve("1/projects/10/.vcs/branches/main");
        FilesystemInit.createDirectories(vcsBranch);
        fileUtil.writeObjectOnFile(new HashMap<Long, ChangeHolder>(), vcsBranch.resolve("changes").toString());
        fileUtil.writeObjectOnFile(new HashMap<Long, ChangeHolder>(), vcsBranch.resolve("tracked").toString());

        Map<Long, ChangeHolder> changes = vcsStorageService.readChanges(project, "main");
        Map<Long, ChangeHolder> tracked = vcsStorageService.readTracked(project, "main");

        assertTrue(changes.isEmpty());
        assertTrue(tracked.isEmpty());
    }

    private Client newClient(Long id) {
        Client c = new Client();
        c.setId(id);
        c.setName("Client " + id);
        c.setEmail("client" + id + "@example.com");
        return c;
    }

    private static final class FilesystemInit {
        private static void createDirectories(Path path) {
            try {
                Files.createDirectories(path);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
