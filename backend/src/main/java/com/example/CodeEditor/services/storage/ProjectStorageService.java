package com.example.CodeEditor.services.storage;

import com.example.CodeEditor.constants.FilesystemPaths;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.ProjectStructure;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.repository.ClientRepository;
import com.example.CodeEditor.repository.ProjectRepository;
import com.example.CodeEditor.utils.EncryptionUtil;
import com.example.CodeEditor.utils.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectStorageService {
    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private FilesystemPaths paths;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PublicRepoStorageService publicRepoStorageService;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    public void createProject(Client client, Project project){
        String projectPath = paths.storageServicePath + File.separator + client.getId() + File.separator + "projects" + File.separator + project.getId();
        if (fileUtil.fileExists(projectPath)){
            projectRepository.delete(project);
            throw new IllegalArgumentException("Project with name " + project.getName() + " already exists");
        }
        try{
            fileUtil.createFolder(projectPath);
            fileUtil.createFolder(projectPath + File.separator + "tree");
            fileUtil.createFolder(projectPath + "/snippets");
            fileUtil.createFolder(projectPath + "/comments");
            fileUtil.createFile(projectPath + "/shared", "");
            fileUtil.writeObjectOnFile(new ArrayList<>(), projectPath + "/shared");
            fileUtil.writeObjectOnFile(new ArrayList<>(), projectPath + "/shared_view");
            saveProjectStructure(client, new ProjectStructure(), project.getId());
        } catch (Exception e){
            throw new IllegalStateException("Failed to create folders on " + projectPath, e);
        }
    }

    @Transactional
    public void deleteProject(Client client, long projectId){
        String projectPath = paths.storageServicePath + "/" + client.getId() + "/projects/" + projectId;
        try{
            List<Long> sharedWith = (List<Long>) fileUtil.readObjectFromFile(projectPath + "/shared", new ArrayList<>());
            for(Long sharedWithId : sharedWith){
                fileUtil.deleteFile(paths.storageServicePath + "/" + sharedWithId + "/shared/" + "/" + client.getId() + "_" + projectId);
            }
             publicRepoStorageService.removeProjectFromPublic(projectId);
            fileUtil.deleteFolder(projectPath);
        } catch (Exception e){
            throw new IllegalStateException("Failed to create folders on " + projectPath, e);
        }
    }

    @Transactional
    public void saveProjectStructure(Client client, ProjectStructure projectStructure, Long projectId) {
        String dirPath = paths.storageServicePath + "/" + client.getId() + "/projects/" + projectId + "/tree/";
        fileUtil.createFolderIfNotExists(dirPath);

        File file = new File(dirPath + "_treeObject.ser");
        try (FileOutputStream fileOutStream = new FileOutputStream(file);
             ObjectOutputStream objectOutStream = new ObjectOutputStream(fileOutStream)){
            objectOutStream.writeObject(projectStructure);
            System.out.println("Object written to " + file.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ProjectStructure loadProjectStructure(Client client, Long projectId) {
        String filePath = paths.storageServicePath + "/" + client.getId() + "/projects/" + projectId + "/tree/" + "_treeObject.ser";
        Path projectPath = Paths.get(filePath);
        if (Files.exists(projectPath)) {
            return (ProjectStructure) fileUtil.readObjectFromFile(filePath, new ProjectStructure());
        } else {
            projectPath = Paths.get(paths.storageServicePath + "/" + client.getId() + "/projects/" + projectId + "/tree/" + "_treeObject");
            if (Files.exists(projectPath)) {
                try{
                    return (ProjectStructure) fileUtil.readObjectFromFile(encryptionUtil.decrypt(fileUtil.readFileContents(projectPath.toString())), new ProjectStructure());
                } catch (Exception e){
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("Project directory not found " + projectPath);
            }
        }
    }

    @Transactional
    public void shareProjectWithEdit(Client clientToShareWith, Long projectId, Long ownerId){
        String listOfSharedPath = paths.storageServicePath + "/" + ownerId + "/projects/" + projectId + "/shared";
        String sharedPath = paths.storageServicePath + "/" + clientToShareWith.getId() + "/shared/";
        shareProject(clientToShareWith, projectId, ownerId, listOfSharedPath, sharedPath);
    }

    @Transactional
    public void shareProjectWithView(Client clientToShareWith, Long projectId, Long ownerId) {
        String listOfSharedPath = paths.storageServicePath + "/" + ownerId + "/projects/" + projectId + "/shared_view";
        String sharedPath = paths.storageServicePath + "/" + clientToShareWith.getId() + "/shared_view/";
        shareProject(clientToShareWith, projectId, ownerId, listOfSharedPath, sharedPath);
    }

    private void shareProject(Client clientToShareWith, Long projectId, Long ownerId, String listOfSharedPath, String sharedPath) {
        fileUtil.createFolderIfNotExists(sharedPath);
        try {
            fileUtil.createFile(sharedPath + "/" + ownerId + "_" + projectId, "");
            List<Long> sharedWith = (List<Long>) fileUtil.readObjectFromFile(listOfSharedPath, new ArrayList<>());
            sharedWith.add(clientToShareWith.getId());
            fileUtil.writeObjectOnFile(sharedWith, listOfSharedPath);
        } catch (Exception e){
            throw new IllegalStateException("Failed to create folder " + sharedPath, e);
        }
    }

    public void removesharedProject(Client clientToShareWith, Long projectId){
        Project project = projectRepository.findById(projectId).orElseThrow();
        Long ownerId = project.getClient().getId();
        String listOfSharedPath = paths.storageServicePath + "/" + ownerId + "/projects/" + projectId + "/shared";
        String sharedPath = paths.storageServicePath + "/" + clientToShareWith.getId() + "/shared/";
        fileUtil.createFolderIfNotExists(sharedPath);
        try {
            fileUtil.deleteFile(sharedPath + "/" + ownerId + "_" + projectId);
            List<Long> sharedWith = (List<Long>) fileUtil.readObjectFromFile(listOfSharedPath, new ArrayList<>());
            sharedWith.remove(clientToShareWith.getId());
            fileUtil.writeObjectOnFile(sharedWith, listOfSharedPath);
        } catch (Exception e){
            throw new IllegalStateException("Failed to delete shared project " + sharedPath, e);
        }
    }

    public List<Client> getAllSharedWith(Long ownerId, Long projectId){
        String listOfSharedPath = paths.storageServicePath + "/" + ownerId + "/projects/" + projectId + "/shared";
        List<Client> clients = new ArrayList<>();
        List<Long> sharedWith = (List<Long>) fileUtil.readObjectFromFile(listOfSharedPath, new ArrayList<>());
        for (Long sharedWithId : sharedWith){
            Client client = clientRepository.findById(sharedWithId).orElse(null);
            clients.add(client);
        }
        return clients;
    }

    public List<String> getSharedEditProjects(Client client){
        String sharedPath = paths.storageServicePath + "/" + client.getId() + "/shared/";
        return getSharedProjects(sharedPath);
    }

    public List<String> getSharedViewProjects(Client client) {
        String sharedPath = paths.storageServicePath + "/" + client.getId() + "/shared_view/";
        return getSharedProjects(sharedPath);
    }

    private List<String> getSharedProjects(String sharedPath) {
        fileUtil.createFolderIfNotExists(sharedPath);
        File[] files = fileUtil.getSubFiles(sharedPath);
        List<String> projects = new ArrayList<>();
        for (File file : files){
            projects.add(file.getName());
        }
        return projects;
    }
}
