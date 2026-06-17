package com.example.CodeEditor.services;

import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.repository.ProjectRepository;
import com.example.CodeEditor.services.storage.ProjectStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ProjectService {
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectStorageService projectStorageService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ClientService clientService;


    @Transactional
    public Project create(Project project) {
        return projectRepository.save(project);
    }

    @Transactional
    public Project create(Map<String, String> data, String reqToken) {
        String projectName = data.get("projectName");
        String token = reqToken.replace("Bearer ", "");
        String email = jwtService.extractUsername(token);
        Client client = clientService.getClientByEmail(email);
        Project project = Project.builder()
                .name(projectName)
                .client(client)
                .build();
        Project createdProject = create(project);
        projectStorageService.createProject(client, createdProject);
        return createdProject;
    }

    public List<Project> getClientProjects(Client client){
        return projectRepository.findByClient(client);
    }

    public List<Project> getClientProjects(String reqToken){
        String token = reqToken.replace("Bearer ", "");
        String email = jwtService.extractUsername(token);
        Client client = clientService.getClientByEmail(email);
        return getClientProjects(client);
    }

    public Project getProjectById(Long id) {
        return projectRepository.findById(id).orElse(null);
    }

    public List<Project> getSharedEditProjects(Client client) {
        List<String> projects = projectStorageService.getSharedEditProjects(client);
        return getSharedProjects(projects);
    }

    public List<Project> getSharedEditProjects(String reqToken) {
        String token = reqToken.replace("Bearer ", "");
        String email = jwtService.extractUsername(token);
        Client client = clientService.getClientByEmail(email);
        return getSharedEditProjects(client);
    }

    public List<Project> getShareViewProjects(Client client) {
        List<String> projects = projectStorageService.getSharedViewProjects(client);
        return getSharedProjects(projects);
    }

    public List<Project> getShareViewProjects(String reqToken) {
        String token = reqToken.replace("Bearer ", "");
        String email = jwtService.extractUsername(token);
        Client client = clientService.getClientByEmail(email);
        return getShareViewProjects(client);
    }

    private List<Project> getSharedProjects(List<String> projects) {
        List<Project> sharedProjects = new ArrayList<>();
        for (String shared : projects) {
            Long projectId = Long.parseLong(shared.split("_")[1]);
            Project project = getProjectById(projectId);
            if (project != null) {
                sharedProjects.add(project);
            }
        }
        return sharedProjects;
    }

    public Project updateProject(Long id, Project project) {
        Project old = getProjectById(id);
        if (project.getName() != null){
            old.setName(project.getName());
        }
        return projectRepository.save(old);
    }

    public Project getProjectByNameAndOwner(String name, Client client) {
        return projectRepository.findByNameAndClient(name, client).orElseThrow();
    }

    public void deleteProjectById(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new IllegalArgumentException("Project does not exist");
        }
        projectRepository.deleteById(id);
    }

    public List<Project> findByClient(Client client) {
        return projectRepository.findByClient(client);
    }

    public void deleteProject(Project project) {
        projectRepository.delete(project);
    }

    public String deleteProject(Long projectId, Long ownerId, String reqToken) {
        String token = reqToken.replace("Bearer ", "");
        String email = jwtService.extractUsername(token);
        Client client = clientService.getClientByEmail(email);
        if (!Objects.equals(client.getId(), ownerId)) {
            return "You aren't allowed to delete this project";
        }

        projectStorageService.deleteProject(client, projectId);
        deleteProjectById(projectId);
        return "Project deleted successfully";
    }
}
