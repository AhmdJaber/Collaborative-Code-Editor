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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SnippetStorageService {
    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private FilesystemPaths paths;

    @Autowired
    private CodeTemplates templates;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Transactional
    public void createSnippet(Client client, Snippet snippet, Long projectId) throws IOException {
        Project project = projectRepository.findById(projectId).orElseThrow();
        String snippetsPath = paths.storageServicePath + "/" + client.getId() + "/projects/" + project.getId() + "/snippets";
        fileUtil.createFolderIfNotExists(snippetsPath);

        String fileName = snippet.getId() + "_" + snippet.getName();
        Path snippetPath = Paths.get(snippetsPath + "/" + fileName);
        Files.createFile(snippetPath);
        String extension = snippet.getName().substring(snippet.getName().lastIndexOf(".") + 1);
        String content = getCodeTemplate(extension);

        Files.write(snippetPath, content.getBytes());
        String commentPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/comments/" + snippet.getId();
        fileUtil.writeObjectOnFile(new ArrayList<>(), commentPath);
    }

    @Transactional
    public void deleteSnippet(Client client, Snippet snippet, Long projectId) throws IOException {// TODO: Move to SnippetStorageService
        String snippetsPath = paths.storageServicePath + "/" + client.getId() + "/projects/" + projectId + "/snippets";
        fileUtil.createFolderIfNotExists(snippetsPath);

        String fileName = snippet.getId() + "_" + snippet.getName();
        Path path = Paths.get(snippetsPath + "/" + fileName);
        Files.delete(path);
    }

    public String loadSnippet(Client client, Long id, String name, Long projectId) throws Exception {
        String snippetsPath = paths.storageServicePath + "/" + client.getId() + "/projects/" + projectId + "/snippets";
        fileUtil.createFolderIfNotExists(snippetsPath);

        String fileName = id + "_" + name;
        Path path = Paths.get(snippetsPath + "/" + fileName);
        if (Files.exists(path)) {
            return Files.readString(path);
        } else {
            path = Paths.get(path.toString().split("/.")[0]);
            if (Files.exists(path)) {
                return fileUtil.readFileContents(encryptionUtil.decrypt(fileUtil.readFileContents(path.toString())));
            } else {
                throw new RuntimeException("File not found");
            }
        }
    }

    @Transactional
    public synchronized void updateSnippet(Client client, Long id, String name, String updatedContent, Long projectId) {
        String snippetsPath = paths.storageServicePath + "/" + client.getId() + "/projects/" + projectId + "/snippets";
        fileUtil.createFolderIfNotExists(snippetsPath);

        String fileName = id + "_" + name;
        Path path = Paths.get(snippetsPath + "/" + fileName);
        try {
            Files.write(path, updatedContent.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not update snippet");
        }
    }

    @Transactional
    public void comment(Client editor, Project project, Long snippetId, String comment, Integer start, Integer end) {
        String commentPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/comments/" + snippetId;
        List<Comment> comments = (List<Comment>) fileUtil.readObjectFromFile(commentPath, new ArrayList<>());
        Comment currentComment = Comment.builder()
                .editorName(editor.getName())
                .editorEmail(editor.getEmail())
                .content(comment)
                .date(LocalDate.now())
                .start(start)
                .end(end)
                .build();
        comments.add(currentComment);
        System.out.println(comments);
        fileUtil.writeObjectOnFile(comments, commentPath);
    }

    public List<Comment> getSnippetComments(Project project, Long snippetId) {
        String commentPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/comments/" + snippetId;
        return (List<Comment>) fileUtil.readObjectFromFile(commentPath, new ArrayList<>());
    }

    private String getCodeTemplate(String extension) {
        return switch (extension) {
            case "cpp" -> templates.cppTemplate;
            case "java" -> templates.javaTemplate;
            case "py" -> templates.pyTemplate;
            default -> "Nothing\n\n Extension " + extension + " not allowed";
        };
    }
}
