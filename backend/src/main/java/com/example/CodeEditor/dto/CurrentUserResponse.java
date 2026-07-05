package com.example.CodeEditor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentUserResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
}
