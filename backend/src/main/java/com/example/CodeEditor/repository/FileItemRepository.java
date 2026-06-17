package com.example.CodeEditor.repository;

import com.example.CodeEditor.model.component.files.FileItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileItemRepository extends JpaRepository<FileItem, Long> {
}
