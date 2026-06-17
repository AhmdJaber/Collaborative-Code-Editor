package com.example.CodeEditor.services;

import com.example.CodeEditor.model.component.files.FileItem;
import com.example.CodeEditor.repository.FileItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FileItemService {
    @Autowired
    private FileItemRepository fileItemRepository;

    public FileItem getFileById(Long id){
        return fileItemRepository.findById(id).orElseThrow(
                () -> new RuntimeException("File with id " + id + " not found")
        );
    }

    public FileItem createFile(FileItem fileItem){
        return fileItemRepository.save(fileItem);
    }

    public void removeFile(Long id){
        if (!fileItemRepository.existsById(id)) {
            throw new RuntimeException("File with id " + id + " not found");
        }
        fileItemRepository.deleteById(id);
    }
}