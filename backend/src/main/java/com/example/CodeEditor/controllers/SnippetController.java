package com.example.CodeEditor.controllers;

import com.example.CodeEditor.model.component.Comment;
import com.example.CodeEditor.model.component.files.Snippet;
import com.example.CodeEditor.services.SnippetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/snippet")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public class SnippetController {
    @Autowired
    private SnippetService snippetService;

    @PostMapping("/create/{ownerId}/{projectId}")
    public Snippet addSnippet(@RequestBody Snippet snippet, @PathVariable Long projectId, @PathVariable Long ownerId) throws Exception {
        snippet.setId(snippetService.createSnippet(ownerId, snippet, projectId));
        return snippet;
    }

    @DeleteMapping("/delete/{ownerId}/{projectId}")
    public void deleteSnippet(@RequestBody Snippet snippet, @PathVariable Long projectId, @PathVariable Long ownerId) throws Exception {
        snippetService.removeSnippet(ownerId, snippet, projectId);
    }

    @GetMapping("/content/{id}/{name}/{ownerId}/{projectId}")
    public String getSnippetContent(@PathVariable Long id, @PathVariable String name, @PathVariable Long projectId, @PathVariable Long ownerId) throws Exception {
        return snippetService.loadSnippet(ownerId, id, name, projectId);
    }

    @PutMapping("/update/{id}/{name}/{ownerId}/{projectId}")
    public void updateSnippet(@PathVariable Long id, @PathVariable String name, @RequestBody String content, @PathVariable Long projectId, @PathVariable Long ownerId) {
        snippetService.updateSnippet(ownerId, id, name, content, projectId);
    }

    @PostMapping("/comment/{projectId}/{snippetId}")
    public void comment(@PathVariable Long projectId, @PathVariable Long snippetId, @RequestBody Map<String, String> body,  @RequestHeader("Authorization") String reqToken) {
        snippetService.comment(projectId, snippetId, body, reqToken);
    }

    @GetMapping("/get-comments/{projectId}/{snippetId}")
    public List<Comment> getSnippetComments(@PathVariable Long projectId, @PathVariable Long snippetId)  {
        return snippetService.getSnippetComments(projectId, snippetId);
    }

    @PostMapping("/execute")
    public String executeCode(@RequestBody Map<String, String> body) {
        return snippetService.executeCode(body);
    }
}
