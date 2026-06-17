package com.example.CodeEditor.model.component.files;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class FileNode implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private List<FileItem> children;
    private Long parentId;
}
