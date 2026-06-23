package com.example.CodeEditor.services.storage;

import com.example.CodeEditor.constants.CodeTemplates;
import com.example.CodeEditor.constants.FilesystemPaths;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.Comment;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.model.component.files.Snippet;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnippetStorageServiceTest {

    @Mock
    private EncryptionUtil encryptionUtil;

    @Spy
    private FileUtil fileUtil;

    @Spy
    private final CodeTemplates templates = new CodeTemplates();

    @Mock
    private ProjectRepository projectRepository;

    private final FilesystemPaths paths = new FilesystemPaths();

    @InjectMocks
    private SnippetStorageService snippetStorageService;

    @TempDir
    Path tempDir;

    private Client client;
    private Project project;

    @BeforeEach
    void setUp() throws Exception {
        paths.storageServicePath = tempDir.toString();
        ReflectionTestUtils.setField(fileUtil, "encryptionUtil", encryptionUtil);
        ReflectionTestUtils.setField(snippetStorageService, "fileUtil", fileUtil);
        ReflectionTestUtils.setField(snippetStorageService, "paths", paths);
        ReflectionTestUtils.setField(snippetStorageService, "templates", templates);
        ReflectionTestUtils.setField(snippetStorageService, "projectRepository", projectRepository);
        ReflectionTestUtils.setField(snippetStorageService, "encryptionUtil", encryptionUtil);

        client = new Client();
        client.setId(1L);
        client.setName("Owner");
        client.setEmail("owner@example.com");

        project = new Project();
        project.setId(10L);
        project.setName("demo");
        project.setClient(client);

        Files.createDirectories(tempDir.resolve("1/projects/10/comments"));
        Files.createDirectories(tempDir.resolve("1/projects/10/snippets"));
    }

    @Test
    void createSnippetCreatesTemplateAndCommentsFile() throws Exception {
        Snippet snippet = new Snippet("Main.java", 0L, 1L);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        snippetStorageService.createSnippet(client, snippet, project.getId());

        Path snippetPath = tempDir.resolve("1/projects/10/snippets/1_Main.java");
        assertTrue(Files.exists(snippetPath));
        assertEquals(templates.javaTemplate, Files.readString(snippetPath));
        List<Comment> comments = castCommentList(fileUtil.readObjectFromFile(
                tempDir.resolve("1/projects/10/comments/1").toString(), new ArrayList<>()));
        assertTrue(comments.isEmpty());
    }

    @Test
    void createSnippetUsesDefaultTemplateForUnsupportedExtension() throws Exception {
        Snippet snippet = new Snippet("Main.txt", 0L, 2L);
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        snippetStorageService.createSnippet(client, snippet, project.getId());

        Path snippetPath = tempDir.resolve("1/projects/10/snippets/2_Main.txt");
        assertTrue(Files.exists(snippetPath));
        assertTrue(Files.readString(snippetPath).contains("Extension txt not allowed"));
    }

    @Test
    void deleteSnippetRemovesFile() throws Exception {
        Snippet snippet = new Snippet("Main.java", 0L, 1L);
        Path snippetPath = tempDir.resolve("1/projects/10/snippets/1_Main.java");
        Files.writeString(snippetPath, "content");

        snippetStorageService.deleteSnippet(client, snippet, project.getId());

        assertFalse(Files.exists(snippetPath));
    }

    @Test
    void loadSnippetReadsDirectFile() throws Exception {
        Path snippetPath = tempDir.resolve("1/projects/10/snippets/1_Main.java");
        Files.writeString(snippetPath, "hello");

        assertEquals("hello", snippetStorageService.loadSnippet(client, 1L, "Main.java", project.getId()));
    }

    @Test
    void loadSnippetThrowsWhenSnippetIsMissing() {
        assertThrows(RuntimeException.class,
                () -> snippetStorageService.loadSnippet(client, 1L, "Missing.java", project.getId()));
    }

    @Test
    void updateSnippetOverwritesContent() throws Exception {
        Snippet snippet = new Snippet("Main.java", 0L, 1L);
        Path snippetPath = tempDir.resolve("1/projects/10/snippets/1_Main.java");
        Files.writeString(snippetPath, "old");

        snippetStorageService.updateSnippet(client, snippet.getId(), snippet.getName(), "new", project.getId());

        assertEquals("new", Files.readString(snippetPath));
    }

    @Test
    void commentAppendsCommentAndGetSnippetCommentsReadsIt() {
        Comment existing = Comment.builder()
                .editorName("Other")
                .editorEmail("other@example.com")
                .content("existing")
                .start(0)
                .end(1)
                .build();
        fileUtil.writeObjectOnFile(new ArrayList<>(List.of(existing)),
                tempDir.resolve("1/projects/10/comments/1").toString());

        Client editor = new Client();
        editor.setId(2L);
        editor.setName("Editor");
        editor.setEmail("editor@example.com");

        snippetStorageService.comment(editor, project, 1L, "new comment", 3, 5);
        List<Comment> comments = snippetStorageService.getSnippetComments(project, 1L);

        assertEquals(2, comments.size());
        assertEquals("existing", comments.get(0).getContent());
        assertEquals("new comment", comments.get(1).getContent());
    }

    @SuppressWarnings("unchecked")
    private List<Comment> castCommentList(Object value) {
        return (List<Comment>) value;
    }
}
