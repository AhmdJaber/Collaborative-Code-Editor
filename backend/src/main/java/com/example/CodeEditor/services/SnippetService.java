package com.example.CodeEditor.services;

import com.example.CodeEditor.enums.Change;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.Comment;
import com.example.CodeEditor.model.component.files.ProjectStructure;
import com.example.CodeEditor.model.component.files.FileItem;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.model.component.files.Snippet;
import com.example.CodeEditor.repository.ProjectRepository;
import com.example.CodeEditor.services.storage.ProjectStorageService;
import com.example.CodeEditor.services.storage.SnippetStorageService;
import com.example.CodeEditor.services.storage.VCSStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class SnippetService {
    @Autowired
    private VCSStorageService storageService;

    @Autowired
    private FileItemService fileItemService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SnippetStorageService snippetStorageService;

    @Autowired
    private ProjectStorageService projectStorageService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CodeExecutionService codeExecutionService;

    @Transactional
    public Long createSnippet(Long editorId, Snippet snippet, Long projectId) throws IOException {
        Client editor = clientService.getClientById(editorId);
        Long snippetId = fileItemService.createFile(new FileItem(snippet.getName(), snippet.getParentId())).getId();
        snippet.setId(snippetId);
        ProjectStructure projectStructure = projectStorageService.loadProjectStructure(editor, projectId);
        projectStructure.getTree().get(snippet.getParentId()).getChildren().add(snippet);
        projectStorageService.saveProjectStructure(editor, projectStructure, projectId);
        snippetStorageService.createSnippet(editor, snippet, projectId);
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new NoSuchElementException("No Project with id " + projectId)
        );
        if (storageService.checkVCSProject(project)) {
            String branchName = storageService.getCurrentBranch(project);
            storageService.makeChange(project, branchName, '-', Change.CREATE, snippet);
        }
        return snippetId;
    }

    @Transactional
    public void removeSnippet(Long editorId, Snippet snippet, Long projectId) throws IOException {
        Client editor = clientService.getClientById(editorId);
        ProjectStructure projectStructure = projectStorageService.loadProjectStructure(editor, projectId);
        projectStructure.getTree().get(snippet.getParentId()).getChildren().remove(snippet);
        projectStorageService.saveProjectStructure(editor, projectStructure, projectId);
        snippetStorageService.deleteSnippet(editor, snippet, projectId);
        fileItemService.removeFile(snippet.getId());
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new NoSuchElementException("No Project with id " + projectId)
        );
        if (storageService.checkVCSProject(project)) {
            String branchName = storageService.getCurrentBranch(project);
            storageService.makeChange(project, branchName, '-', Change.DELETE, snippet);
        }
    }

    @Transactional
    public void updateSnippet(Long editorId, Long id, String name, String updatedContent, Long projectId) {
        Client editor = clientService.getClientById(editorId);
        snippetStorageService.updateSnippet(editor, id, name, updatedContent, projectId);
        System.out.println("Snippet " + id + "_" + name + " has been updated");
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new NoSuchElementException("No Project with id " + projectId)
        );
        FileItem snippet = fileItemService.getFileById(id);
        if (storageService.checkVCSProject(project)) {
            String branchName = storageService.getCurrentBranch(project);
            storageService.makeChange(project, branchName, '-', Change.UPDATE, snippet);
        }
    }

    public String loadSnippet(Long editorId, Long id, String name, Long projectId) throws Exception {
        Client editor = clientService.getClientById(editorId);
        return snippetStorageService.loadSnippet(editor, id, name, projectId);
    }

    public void comment(Client editor, Project project, Long snippetId, String comment, Integer start, Integer end) {
        snippetStorageService.comment(editor, project, snippetId, comment, start, end);
    }

    @Transactional
    public void comment(Long projectId, Long snippetId, Map<String, String> body, String reqToken) {
        String senderEmail = jwtService.extractUsername(reqToken.replace("Bearer ", ""));
        Client editor = clientService.getClientByEmail(senderEmail);
        Project project = projectService.getProjectById(projectId);
        String comment = body.get("comment");
        Integer startLine = Integer.parseInt(body.get("start"));
        Integer endLine = Integer.parseInt(body.get("end"));
        comment(editor, project, snippetId, comment, startLine, endLine);
    }

    public List<Comment> getSnippetComments(Long projectId, Long snippetId) {
        Project project = projectService.getProjectById(projectId);
        return snippetStorageService.getSnippetComments(project, snippetId);
    }

    public String executeCode(Map<String, String> body) {
        String code = body.get("code");
        String language = body.get("language");

        System.out.println(language);
        return codeExecutionService.executeCode(code, language);
    }
}
