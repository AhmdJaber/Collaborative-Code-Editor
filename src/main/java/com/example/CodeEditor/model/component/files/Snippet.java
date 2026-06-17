package com.example.CodeEditor.model.component.files;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class Snippet extends FileItem implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public Snippet(String name, Long parentId, Long id) {
        super.setId(id);
        super.setName(name);
        super.setParentId(parentId);
        super.setIsFolder(false);
    }

    @Override
    public String toString() {
        return "Snippet{" +
                "name='" + super.getName() + '\'' +
                "parentId='" + super.getParentId() + '\'' +
                "id='" + super.getId() + '\'' +
                '}';
    }
}