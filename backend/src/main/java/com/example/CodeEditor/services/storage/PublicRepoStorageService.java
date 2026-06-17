package com.example.CodeEditor.services.storage;

import com.example.CodeEditor.constants.FilesystemPaths;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.repository.ClientRepository;
import com.example.CodeEditor.repository.ProjectRepository;
import com.example.CodeEditor.utils.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PublicRepoStorageService {
    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FilesystemPaths paths;

    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    public void shareProjectToPublic(Long projectId){
        Project project = projectRepository.findById(projectId).orElseThrow();
        String userPublic = paths.storageServicePath + "/" + project.getClient().getId() + "/public";
        List<Long> publicProjects = (List<Long>) fileUtil.readObjectFromFile(userPublic, new ArrayList<>());
        publicProjects.add(projectId);
        fileUtil.writeObjectOnFile(publicProjects, userPublic);
    }

    @Transactional
    public void removeProjectFromPublic(Long projectId){
        Project project = projectRepository.findById(projectId).orElseThrow();
        String userPublic = paths.storageServicePath + "/" + project.getClient().getId() + "/public";
        List<Long> publicProjects = (List<Long>) fileUtil.readObjectFromFile(userPublic, new ArrayList<>());
        publicProjects.remove(projectId);
        fileUtil.writeObjectOnFile(publicProjects, userPublic);
    }

    @Transactional
    public List<Project> getPublicProjects(Long clientId){
        Client client = clientRepository.findById(clientId).orElseThrow();
        String userPublic = paths.storageServicePath + "/" + client.getId() + "/public";
        List<Project> publicProjets = new ArrayList<>();
        List<Long> projectsIds = (List<Long>) fileUtil.readObjectFromFile(userPublic, new ArrayList<>());
        for (Long projectId : projectsIds){
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project != null){
                publicProjets.add(project);
            }
        }
        return publicProjets;
    }

    @Transactional
    public boolean checkProjectPublic(Long projectId){
        Project project = projectRepository.findById(projectId).orElseThrow();
        String userPublic = paths.storageServicePath + "/" + project.getClient().getId() + "/public";
        List<Long> publicProjects = (List<Long>) fileUtil.readObjectFromFile(userPublic, new ArrayList<>());
        return publicProjects.contains(projectId);
    }

}
