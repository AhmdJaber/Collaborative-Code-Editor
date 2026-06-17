package com.example.CodeEditor.controllers;

import com.example.CodeEditor.services.VCSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vcs")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public class VCSController {
    @Autowired
    private VCSService vcsService;


    @GetMapping("/init/{projectId}")
    public ResponseEntity<String> init(@PathVariable Long projectId) {
        vcsService.initVCS(projectId);
        return ResponseEntity.ok(".vcs directory initialized");
    }

    @DeleteMapping("/delete/{projectId}")
    public ResponseEntity<String> deleteVCS(@PathVariable Long projectId) {
        vcsService.deleteVCS(projectId);
        return ResponseEntity.ok(".vcs directory deleted");
    }

    @GetMapping("/status/{projectId}")
    public ResponseEntity<?> status(@PathVariable Long projectId) {
        Map<String, List<String>> status =  vcsService.status(projectId);
        return ResponseEntity.ok().body(status);
    }

    @PostMapping("/add/{projectId}")
    public ResponseEntity<?> add(@PathVariable Long projectId, @RequestBody Map<String, Object> body) throws Exception {
        List<String> filePaths = (List<String>) body.get("files");
        List<String> addResult = vcsService.add(projectId, filePaths);
        return ResponseEntity.ok().body(addResult);
    }

    @PostMapping("/commit/{projectId}")
    public ResponseEntity<?> commit(@PathVariable Long projectId, @RequestBody String message, @RequestHeader("Authorization") String reqToken)  {
        return vcsService.commit(projectId, message, reqToken);
    }

    @GetMapping("/log/{projectId}")
    public ResponseEntity<?> log(@PathVariable Long projectId) {
        String log = vcsService.log(projectId);
        return ResponseEntity.ok(log);
    }

    @PostMapping("/revert/{projectId}")
    public ResponseEntity<?> revert(@PathVariable Long projectId, @RequestBody String commitId) {
        vcsService.revert(projectId, commitId.replace("\"", ""));
        return ResponseEntity.ok("Reverted to commit with id: " + commitId);
    }

    @GetMapping("/check-vcs/{projectId}")
    public ResponseEntity<?> checkVCS(@PathVariable Long projectId){
        return ResponseEntity.ok().body(vcsService.checkVCSProject(projectId));
    }

    @PostMapping("/fork/{projectId}")
    public ResponseEntity<?> fork(@PathVariable Long projectId, @RequestHeader("Authorization") String reqToken) {
        return vcsService.fork(projectId, reqToken);
    }

    @PostMapping("/fork")
    public ResponseEntity<?> fork(@RequestBody Map<String, String> body, @RequestHeader("Authorization") String reqToken) {
        return vcsService.fork(body, reqToken);
    }

    @GetMapping("/branches/{projectId}")
    public ResponseEntity<?> allBranches(@PathVariable Long projectId){
        return ResponseEntity.ok(vcsService.allBranches(projectId));
    }

    @PostMapping("/create-branch/{projectId}")
    public ResponseEntity<?> createBranch(@PathVariable Long projectId, @RequestBody String branchName) {
        vcsService.createBranch(projectId, branchName);
        return ResponseEntity.ok("Created new branch: " + branchName);
    }

    @DeleteMapping("/delete-branch/{projectId}")
    public ResponseEntity<?> deleteBranch(@PathVariable Long projectId, @RequestBody String branchName) {
        vcsService.deleteBranch(projectId, branchName);
        return ResponseEntity.ok("Deleted branch: " + branchName);
    }

    @PutMapping("/checkout/{projectId}")
    public ResponseEntity<?> chekcout(@PathVariable Long projectId, @RequestBody String branchName) {
        vcsService.checkout(projectId, branchName);
        return ResponseEntity.ok("Switched to branch: " + branchName);
    }

    @PostMapping("/checkout-create/{projectId}")
    public ResponseEntity<?> checkoutCreate(@PathVariable Long projectId, @RequestBody String branchName) {
        vcsService.createBranch(projectId, branchName);
        vcsService.checkout(projectId, branchName);
        return ResponseEntity.ok("Created new branch: " + branchName + " and switched to it");
    }


}
