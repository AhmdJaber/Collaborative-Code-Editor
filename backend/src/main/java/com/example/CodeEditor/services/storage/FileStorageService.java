package com.example.CodeEditor.services.storage;

import com.example.CodeEditor.model.component.files.FileItem;
import com.example.CodeEditor.model.component.files.FileNode;
import com.example.CodeEditor.model.component.files.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FileStorageService {
    @Autowired
    private ProjectStorageService projectStorageService;

    public FileStorageService(ProjectStorageService projectStorageService) {
        this.projectStorageService = projectStorageService;
    }

    public Long getFileIdByPath(Project project, String path) {
        Map<Long, FileNode> editorDir = projectStorageService.loadProjectStructure(project.getClient(), project.getId()).getTree();
        if (path.charAt(0) == '/'){
            path = path.substring(1);
        }
        String[] files = path.split("/");
        return getIdFromEditorDir(editorDir, files, 0, 0L);
    }

    private Long getIdFromEditorDir(Map<Long, FileNode> editorDir, String[] files, int currentFile, Long currentId){
        for(FileItem fileItem: editorDir.get(currentId).getChildren()){
            if (fileItem.getName().equals(files[currentFile])){
                if (currentFile == files.length - 1) {
                    return fileItem.getId();
                }
                return getIdFromEditorDir(editorDir, files, currentFile + 1, fileItem.getId());
            }
        }
        throw new IllegalArgumentException("No file with name " + files[currentFile] + "in the directory " + editorDir.get(currentId).getName());
    }
}
