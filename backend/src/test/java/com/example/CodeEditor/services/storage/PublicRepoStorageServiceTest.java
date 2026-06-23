package com.example.CodeEditor.services.storage;

import com.example.CodeEditor.constants.FilesystemPaths;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.Project;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicRepoStorageServiceTest {

    @Mock
    private EncryptionUtil encryptionUtil;

    @Spy
    private FileUtil fileUtil;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ClientRepository clientRepository;

    @Spy
    private final FilesystemPaths paths = new FilesystemPaths();

    @InjectMocks
    private PublicRepoStorageService publicRepoStorageService;

    @TempDir
    Path tempDir;

    private Client client;
    private Project project;

    @BeforeEach
    void setUp() throws Exception {
        paths.storageServicePath = tempDir.toString();
        ReflectionTestUtils.setField(fileUtil, "encryptionUtil", encryptionUtil);
        ReflectionTestUtils.setField(publicRepoStorageService, "paths", paths);

        client = new Client();
        client.setId(1L);
        client.setName("Owner");
        client.setEmail("owner@example.com");

        project = new Project();
        project.setId(10L);
        project.setName("demo");
        project.setClient(client);

        Files.createDirectories(tempDir.resolve("1"));
        Files.createDirectories(tempDir.resolve("2"));
    }

    @Test
    void shareProjectToPublicAddsProjectId() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        publicRepoStorageService.shareProjectToPublic(project.getId());

        List<Long> publicProjects = castLongList(fileUtil.readObjectFromFile(
                tempDir.resolve("1/public").toString(), new ArrayList<>()));
        assertEquals(List.of(project.getId()), publicProjects);
    }

    @Test
    void removeProjectFromPublicRemovesProjectId() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        fileUtil.writeObjectOnFile(new ArrayList<>(List.of(project.getId())), tempDir.resolve("1/public").toString());

        publicRepoStorageService.removeProjectFromPublic(project.getId());

        List<Long> publicProjects = castLongList(fileUtil.readObjectFromFile(
                tempDir.resolve("1/public").toString(), new ArrayList<>()));
        assertFalse(publicProjects.contains(project.getId()));
    }

    @Test
    void getPublicProjectsReturnsExistingProjectsOnly() {
        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        fileUtil.writeObjectOnFile(new ArrayList<>(List.of(project.getId(), 999L)), tempDir.resolve("1/public").toString());

        List<Project> publicProjects = publicRepoStorageService.getPublicProjects(client.getId());

        assertEquals(List.of(project.getId()), publicProjects.stream().map(Project::getId).toList());
    }

    @Test
    void checkProjectPublicReturnsTrueWhenProjectIsListed() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        fileUtil.writeObjectOnFile(new ArrayList<>(List.of(project.getId())), tempDir.resolve("1/public").toString());

        assertTrue(publicRepoStorageService.checkProjectPublic(project.getId()));
    }

    @Test
    void checkProjectPublicReturnsFalseWhenProjectIsNotListed() {
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        fileUtil.writeObjectOnFile(new ArrayList<Long>(), tempDir.resolve("1/public").toString());

        assertFalse(publicRepoStorageService.checkProjectPublic(project.getId()));
    }

    @SuppressWarnings("unchecked")
    private List<Long> castLongList(Object value) {
        return (List<Long>) value;
    }
}
