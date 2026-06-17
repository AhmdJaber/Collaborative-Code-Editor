package com.example.CodeEditor.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Permission {

    ADMIN_READ("admin:read"),
    ADMIN_CREATE("admin:create"),
    ADMIN_DELETE("admin:delete"),
    ADMIN_UPDATE("admin:update"),

    EDITOR_READ("editor:read"),
    EDITOR_CREATE("editor:create"),
    EDITOR_DELETE("editor:delete"),
    EDITOR_UPDATE("editor:update");

    private final String permission;
}
