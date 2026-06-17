package com.example.CodeEditor.model.component.files;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class Folder extends FileItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public Folder(String name, Long parentId, Long id) {
        super.setId(id);
        super.setName(name);
        super.setParentId(parentId);
        super.setIsFolder(true);
    }

    @Override
    public String toString() {
        return "Folder{" +
                "name='" + super.getName() + '\'' +
                ", parentId=" + super.getParentId() +
                ", id=" + super.getId() + '\'' +
                '}';
    }
}