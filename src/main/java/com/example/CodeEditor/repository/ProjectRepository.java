package com.example.CodeEditor.repository;

import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.model.clients.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByClient(Client client);
    Optional<Project> findByNameAndClient(String name, Client client);
}
