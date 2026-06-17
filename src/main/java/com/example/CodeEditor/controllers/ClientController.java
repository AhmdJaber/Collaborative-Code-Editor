package com.example.CodeEditor.controllers;

import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.ProjectStructure;
import com.example.CodeEditor.services.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/editor")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public class ClientController {
    @Autowired
    private ClientService clientService;

    @GetMapping("/all-editors")
    public List<Client> allEditors(){
        return clientService.getAllEditors();
    }

    @GetMapping("/directory/{ownerId}/{projectId}")
    public ProjectStructure getEditorDirectory(@PathVariable Long ownerId, @PathVariable Long projectId) {
        return clientService.loadProjectStructure(ownerId, projectId);
    }

    @PostMapping("/share_project_edit/{email}/{ownerId}/{projectId}")
    public ResponseEntity<?> shareProjectWithEdit(@PathVariable String email, @PathVariable Long ownerId, @PathVariable Long projectId, @RequestHeader("Authorization") String reqToken) {
        clientService.shareProjectWithEdit(email, ownerId, projectId, reqToken);
        System.out.println("here !!!!!!!!");
        return ResponseEntity.ok("Project shared with editor");
    }

    @PostMapping("/share_project_view/{email}/{ownerId}/{projectId}")
    public ResponseEntity<?> shareProjectWithView(@PathVariable String email, @PathVariable Long ownerId, @PathVariable Long projectId, @RequestHeader("Authorization") String reqToken) {
        clientService.shareProjectWithView(email, ownerId, projectId, reqToken);
        return ResponseEntity.ok("Project shared with view");
    }

    @PostMapping("/share_project_view_token/{projectId}")
    public ResponseEntity<?> shareProjectWithViewByToken(@PathVariable Long projectId, @RequestHeader("Authorization") String reqToken) {
        return clientService.shareProjectWithViewByToken(projectId, reqToken);
    }

    @PostMapping("/share-project-public/{projectId}")
    public ResponseEntity<?> shareToPublic(@PathVariable Long projectId, @RequestHeader("Authorization") String reqToken) {
        return clientService.shareToPublic(projectId, reqToken);
    }

    @GetMapping("/get-public-projects/{clientId}")
    public ResponseEntity<?> getPublicProjects(@PathVariable Long clientId){
        return clientService.getPublicProjects(clientId);
    }

    @DeleteMapping("/remove-project-public/{projectId}")
    public ResponseEntity<?> removePublicProject(@PathVariable Long projectId){
        return clientService.removePublicProject(projectId);
    }

    @GetMapping("/check-project-public/{projectId}")
    public ResponseEntity<?> checkProjectPublic(@PathVariable Long projectId){
        return clientService.checkProjectPublic(projectId);
    }

    @GetMapping("/get-editor-by-data/{email}")
    public ResponseEntity<?> getEditorByEmail(@PathVariable String email){
        if (clientService.existsClientByEmail(email)){
            return ResponseEntity.ok(clientService.getClientByEmail(email));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
