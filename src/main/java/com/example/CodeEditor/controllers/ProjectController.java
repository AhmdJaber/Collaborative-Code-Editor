package com.example.CodeEditor.controllers;

import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.services.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/project")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public class ProjectController {
    @Autowired
    private ProjectService projectService;

    @GetMapping("/client_projects")
    public List<Project> getProjects(@RequestHeader("Authorization") String reqToken) {
        return projectService.getClientProjects(reqToken);
    }

    @GetMapping("/shared_projects_edit")
    public List<Project> getSharedEditProjects(@RequestHeader("Authorization") String reqToken) {
        return projectService.getSharedEditProjects(reqToken);
    }

    @GetMapping("/shared_projects_view")
    public List<Project> getSharedViewProjects(@RequestHeader("Authorization") String reqToken) {
        return projectService.getShareViewProjects(reqToken);
    }

    @PostMapping("/create")
    public Project createProject(@RequestBody Map<String, String> data, @RequestHeader("Authorization") String reqToken) {
        return projectService.create(data, reqToken);
    }

    @DeleteMapping("/delete/{projectId}/{ownerId}")
    public ResponseEntity<String> deleteProject(@RequestHeader("Authorization") String reqToken, @PathVariable Long projectId, @PathVariable long ownerId) {
        String response = projectService.deleteProject(projectId, ownerId, reqToken);
        if (response.equals("Project deleted successfully")){
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(reqToken);
    }
}