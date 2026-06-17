package com.example.CodeEditor.services;

import com.example.CodeEditor.enums.Change;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.ProjectStructure;
import com.example.CodeEditor.model.component.files.FileItem;
import com.example.CodeEditor.model.component.files.FileNode;
import com.example.CodeEditor.model.component.files.Folder;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.repository.ProjectRepository;
import com.example.CodeEditor.services.storage.ProjectStorageService;
import com.example.CodeEditor.services.storage.VCSStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.NoSuchElementException;

@Service
public class FolderService {
    @Autowired
    private VCSStorageService storageService;

    @Autowired
    private FileItemService fileItemService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectStorageService projectStorageService;

    @Autowired
    private ClientService clientService;

    @Transactional
    public Long createFolder(Long editorId, Folder folder, Long projectId) {
        Client editor = clientService.getClientById(editorId);
        Long folderId = fileItemService.createFile(new FileItem(folder.getName(), folder.getParentId())).getId();
        folder.setId(folderId);
        ProjectStructure projectStructure = projectStorageService.loadProjectStructure(editor, projectId);
        projectStructure.getTree().get(folder.getParentId()).getChildren().add(folder);
        projectStructure.getTree().put(folderId, new FileNode(folder.getName(), new ArrayList<>(), folder.getParentId()));
        projectStorageService.saveProjectStructure(editor, projectStructure, projectId);
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new NoSuchElementException("No Project with id " + projectId)
        );
        if (storageService.checkVCSProject(project)){
            String branchName = storageService.getCurrentBranch(project);
            storageService.makeChange(project, branchName, 'd', Change.CREATE, folder);
        }
        return folderId;
    }

    @Transactional
    public void removeFolder(Long editorId, Folder folder, Long projectId) {
        Client editor = clientService.getClientById(editorId);
        ProjectStructure projectStructure = projectStorageService.loadProjectStructure(editor, projectId);
        System.out.println(projectStructure);
        projectStructure.getTree().get(folder.getParentId()).getChildren().remove(folder);
        projectStructure.getTree().remove(folder.getId());
        projectStorageService.saveProjectStructure(editor, projectStructure, projectId);
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new NoSuchElementException("No Project with id " + projectId)
        );
        if (storageService.checkVCSProject(project)) {
            String branchName = storageService.getCurrentBranch(project);
            storageService.makeChange(project, branchName, 'd', Change.DELETE, folder);
        }
    }
}