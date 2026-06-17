package com.example.CodeEditor.services;

import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.FileItem;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.repository.ProjectRepository;
import com.example.CodeEditor.services.storage.FileStorageService;
import com.example.CodeEditor.services.storage.VCSStorageService;
import com.example.CodeEditor.model.component.ChangeHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Service
public class VCSService {
    @Autowired
    private VCSStorageService vcsStorageService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FileItemService fileItemService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProjectService projectService;

    public void initVCS(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        vcsStorageService.initVCS(project);
    }

    public void deleteVCS(Long projectId){
        Project project = projectRepository.findById(projectId).orElseThrow();
        vcsStorageService.deleteVCS(project);
    }

    public Map<String, List<String>> status(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        String branchName = vcsStorageService.getCurrentBranch(project);
        Map<Long, ChangeHolder> untrackedChanges = vcsStorageService.readChanges(project, branchName);
        Map<Long, ChangeHolder> trackedChanges = vcsStorageService.readTracked(project, branchName);
        Map<String, List<String>> status = new HashMap<>();
        status.put("untracked", new ArrayList<>());
        status.put("tracked", new ArrayList<>());
        for(Long id : untrackedChanges.keySet()){
            FileItem fileItem = fileItemService.getFileById(id);
            status.get("untracked").add(fileItem.getName());
        }
        for(Long id : trackedChanges.keySet()){
            FileItem fileItem = fileItemService.getFileById(id);
            status.get("tracked").add(fileItem.getName());
        }
        return status;
    }

    @Transactional
    public List<String> add(Long projectId, List<String> files) throws IOException {
        Project project = projectRepository.findById(projectId).orElseThrow();
        String branchName = vcsStorageService.getCurrentBranch(project);
        Map<Long, ChangeHolder> changes;
        if (files.contains(".")) {
            changes = vcsStorageService.trackAllChanges(project, branchName);
        } else {
            List<Long> filesIds = new ArrayList<>();
            for(String filePath : files){
                Long fileId = fileStorageService.getFileIdByPath(project, filePath);
                filesIds.add(fileId);
            }
            changes = vcsStorageService.trackChanges(project, branchName, filesIds);
        }
        if (files.contains(".")) {
            List<String> changesNames = new ArrayList<>();
            for (Long change : changes.keySet()) {
                changesNames.add(fileItemService.getFileById(change).getName());
            }
            return changesNames;
        } else {
            return files;
        }
    }

    @Transactional
    public String log(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        String branchName = vcsStorageService.getCurrentBranch(project);
        List<String> logs = vcsStorageService.log(project, branchName);
        StringBuilder log = new StringBuilder();
        for (String currentLog : logs) {
            log.append(currentLog).append("\n\n");
        }
        return log.toString();
    }

    @Transactional
    public List<String> commit(Long projectId, Client client, String message) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        String branchName = vcsStorageService.getCurrentBranch(project);
        String currentCommit = vcsStorageService.getCurrentCommit(project, branchName);
        Map<Long, ChangeHolder> trackedChanges = vcsStorageService.commitTracked(project, branchName, client, message, currentCommit);
        List<String> changesNames = new ArrayList<>();
        for(Long change: trackedChanges.keySet()){
            changesNames.add(fileItemService.getFileById(change).getName());
        }
        return changesNames;
    }

    @Transactional
    public ResponseEntity<List<String>> commit(Long projectId, String message, String reqToken) {
        String senderEmail = jwtService.extractUsername(reqToken.replace("Bearer ", ""));
        Client client = clientService.getClientByEmail(senderEmail);
        message = message.substring(1, message.length() - 1);
        List<String> trackedChanges = commit(projectId, client, message);
        return ResponseEntity.ok(trackedChanges);
    }

    public void revert(Long projectId, String commitId) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        String branchName = vcsStorageService.getCurrentBranch(project);
        vcsStorageService.revert(project, branchName, commitId);
    }

    public boolean checkVCSProject(Long projectId){
        Project project = projectRepository.findById(projectId).orElseThrow();
        return vcsStorageService.checkVCSProject(project);
    }

    public void fork(Client client, Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        vcsStorageService.fork(project, client);
    }

    public ResponseEntity<String> fork(Long projectId, String reqToken) {
        String senderEmail = jwtService.extractUsername(reqToken.replace("Bearer ", ""));
        Client client = clientService.getClientByEmail(senderEmail);
        Project project = projectService.getProjectById(projectId);
        if (Objects.equals(project.getClient(), client)){
            return ResponseEntity.badRequest().body("Cannot fork projects you own");
        }
        fork(client, projectId);
        return ResponseEntity.ok("Forked successfully");
    }

    public void fork(Client client, Project project) {
        vcsStorageService.fork(project, client);
    }

    public ResponseEntity<String> fork(Map<String, String> body, String reqToken) {
        String ownerEmail = body.get("owner");
        String projectName = body.get("project");
        String senderEmail = jwtService.extractUsername(reqToken.replace("Bearer ", ""));
        Client client = clientService.getClientByEmail(senderEmail);
        Client owner = clientService.getClientByEmail(ownerEmail);
        Project project = projectService.getProjectByNameAndOwner(projectName, owner);
        if (Objects.equals(project.getClient(), client)){
            return ResponseEntity.badRequest().body("Cannot fork projects you own");
        }
        fork(client, project);
        return ResponseEntity.ok("Forked successfully");
    }

    public List<String> allBranches(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        return vcsStorageService.allBranches(project);
    }

    @Transactional
    public void createBranch(Long projectId, String branchName) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        String currentBranchName = vcsStorageService.getCurrentBranch(project);
        branchName = branchName.substring(1, branchName.length() - 1);
        vcsStorageService.createBranch(project, branchName, currentBranchName);
    }

    @Transactional
    public void deleteBranch(Long projectId, String branchName) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        branchName = branchName.substring(1, branchName.length() - 1);
        vcsStorageService.deleteBranch(project, branchName);
    }

    @Transactional
    public void checkout(Long projectId, String branchName) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        branchName = branchName.substring(1, branchName.length() - 1);
        vcsStorageService.checkout(project, branchName);
    }
}