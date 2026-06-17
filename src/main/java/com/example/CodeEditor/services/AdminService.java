package com.example.CodeEditor.services;

import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.services.storage.ProjectStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    @Autowired
    private ProjectService projectService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProjectStorageService projectStorageService;


    public List<Client> getAllEditors() {
        return clientService.getAllEditors();
    }

    public void deleteEditor(Long editorId) {
        clientService.deleteEditor(editorId);
    }

    public List<Project> getEditorProjects(Long editorId) {
        Client client = clientService.getClientById(editorId);
        return projectService.getClientProjects(client);
    }

    public void removeEditorProject(Long editorId, Long projectId) {
        Client client = clientService.getClientById(editorId);
        projectService.deleteProjectById(projectId);
        projectStorageService.deleteProject(client, projectId);
    }

    public List<Project> getSharedProjects(Long editorId) {
        Client client = clientService.getClientById(editorId);
        return projectService.getSharedEditProjects(client);
    }

    public void removeSharedProject(Long editorId, Long projectId) {
        Client client = clientService.getClientById(editorId);
        projectStorageService.removesharedProject(client, projectId);
    }

    public List<Client> getAllSharedWith(Long projectId, Long ownerId) {
        return projectStorageService.getAllSharedWith(projectId, ownerId);
    }
}
