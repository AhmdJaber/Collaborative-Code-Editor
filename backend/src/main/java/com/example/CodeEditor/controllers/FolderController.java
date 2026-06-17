package com.example.CodeEditor.controllers;

import com.example.CodeEditor.model.component.files.Folder;
import com.example.CodeEditor.services.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/folder")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public class FolderController {
    @Autowired
    private FolderService folderService;

    @PostMapping("/create/{ownerId}/{projectId}")
    public Long addFolder(@RequestBody Folder folder, @PathVariable Long projectId, @PathVariable Long ownerId) {
        return folderService.createFolder(ownerId, folder, projectId);
    }

    @DeleteMapping("/delete/{ownerId}/{projectId}")
    public void deleteFolder(@RequestBody Folder folder, @PathVariable Long projectId, @PathVariable Long ownerId) {
        folderService.removeFolder(ownerId, folder, projectId);
    }
}