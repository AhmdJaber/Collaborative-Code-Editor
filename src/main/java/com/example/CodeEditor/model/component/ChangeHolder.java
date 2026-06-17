package com.example.CodeEditor.model.component;

import com.example.CodeEditor.enums.Change;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeHolder implements Serializable {
    private Change change;
    private char fileType;
    private String content;
}
