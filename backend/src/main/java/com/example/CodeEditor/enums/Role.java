package com.example.CodeEditor.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.example.CodeEditor.enums.Permission.*;

@RequiredArgsConstructor
@Getter
public enum Role {
    ADMIN(
            Set.of(
                    ADMIN_READ,
                    ADMIN_CREATE,
                    ADMIN_UPDATE,
                    ADMIN_DELETE,
                    EDITOR_READ,
                    EDITOR_CREATE,
                    EDITOR_UPDATE,
                    EDITOR_DELETE
            )
    ),
    EDITOR(
            Set.of(
                    EDITOR_READ,
                    EDITOR_CREATE,
                    EDITOR_UPDATE,
                    EDITOR_DELETE
            )
    ),
    VIEWER(
            Collections.emptySet()
    );

    private final Set<Permission> permissions;

    public List<SimpleGrantedAuthority> getSimpleGrantedAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>(getPermissions()
                .stream()
                .map(permission -> new SimpleGrantedAuthority(permission.name()))
                .toList());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    }
}
