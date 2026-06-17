package com.example.CodeEditor.controllers;

import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.services.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    @Autowired
    private AdminService adminService;

    @GetMapping("/get-editors")
    public List<Client> getAllEditors(){
        return adminService.getAllEditors();
    }

    @DeleteMapping("/remove-editor/{editorId}")
    public void removeEditor(@PathVariable Long editorId){
        adminService.deleteEditor(editorId);
    }

    @GetMapping("/get-editor-projects/{editorId}")
    public List<Project> getEditorProjects(@PathVariable Long editorId){
        return adminService.getEditorProjects(editorId);
    }

    @DeleteMapping("/remvoe-project/{editorId}/{projectId}")
    public void removeEditorProject(@PathVariable Long editorId, @PathVariable Long projectId){
        adminService.removeEditorProject(editorId, projectId);
    }

    @GetMapping("/get-shared/{editorId}")
    public List<Project> getSharedProjects(@PathVariable Long editorId){
        return adminService.getSharedProjects(editorId);
    }

    @DeleteMapping("/remove-shared-project/{editorId}/{projectId}")
    public void removeSharedProject(@PathVariable Long editorId, @PathVariable Long projectId){
        adminService.removeSharedProject(editorId, projectId);
    }

    @GetMapping("/get-shared/{projectId}/{ownerId}")
    public List<Client> getSharedWith(@PathVariable Long projectId, @PathVariable Long ownerId){
        return adminService.getAllSharedWith(projectId, ownerId);
    }

}
