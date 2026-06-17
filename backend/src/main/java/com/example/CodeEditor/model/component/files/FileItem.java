package com.example.CodeEditor.model.component.files;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Table
@Entity
@NoArgsConstructor
public class FileItem implements Serializable{
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Long parentId;
    private Boolean isFolder;

    public FileItem(String name, Long parentId) {
        this.name = name;
        this.parentId = parentId;
    }
}
