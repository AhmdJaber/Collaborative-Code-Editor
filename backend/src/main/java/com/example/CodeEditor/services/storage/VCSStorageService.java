package com.example.CodeEditor.services.storage;

import com.example.CodeEditor.constants.FilesystemPaths;
import com.example.CodeEditor.enums.Change;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.model.component.files.ProjectStructure;
import com.example.CodeEditor.model.component.files.FileItem;
import com.example.CodeEditor.model.component.files.FileNode;
import com.example.CodeEditor.model.component.files.Project;
import com.example.CodeEditor.repository.FileItemRepository;
import com.example.CodeEditor.repository.ProjectRepository;
import com.example.CodeEditor.utils.FileUtil;
import com.example.CodeEditor.model.component.ChangeHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@Service
public class VCSStorageService {
    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private FileItemRepository fileItemRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PublicRepoStorageService publicRepoStorageService;

    @Autowired
    private ProjectStorageService projectStorageService;

    @Autowired
    private FilesystemPaths paths;

    @Transactional
    public void initVCS(Project project) {
        long ownerId = project.getClient().getId();
        String vcsPath = paths.storageServicePath + "/" + ownerId + "/projects/" + project.getId() + "/.vcs";
        if (!fileUtil.fileExists(vcsPath)){
            fileUtil.createFolderIfNotExists(vcsPath);
            fileUtil.createFile(vcsPath + "/HEAD", "main");
            fileUtil.writeObjectOnFile(new HashMap<>(), vcsPath + "/config");
            createBranch(project, "main");
            createInitialCommit(project);
        } else {
            throw new RuntimeException("Project is already a vcs directory " + project.getClient().getId());
        }
    }

    public boolean checkVCSProject(Project project) {
        String vcsPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs";
        return fileUtil.fileExists(vcsPath);
    }

    @Transactional
    protected void createInitialCommit(Project project) {
        String commitsPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches/main/commits";
        String commitId = project.getClient().getId().toString() + "%" + Instant.now().getEpochSecond() + "%" + "initial commit".hashCode();
        Map<Long, FileNode> projectStructure = projectStorageService.loadProjectStructure(project.getClient(), project.getId()).getTree();
        ProjectStructure structure = new ProjectStructure(projectStructure);
        File[] allSnippets = fileUtil.getSubFiles(paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/snippets");
        Map<Long, String> filesContent = new HashMap<>();
        for(File snippet : allSnippets){
            String name = snippet.getName();
            Long id = Long.parseLong(name.split("_")[0]);
            String content = fileUtil.readFileContents(snippet.getAbsolutePath());
            filesContent.put(id, content);
        }

        fileUtil.createFolder(commitsPath + "/" + commitId);
        fileUtil.createFolder(commitsPath + "/" + commitId + "/snippets");
        fileUtil.createFolder(commitsPath + "/" + commitId + "/tree");
        fileUtil.writeObjectOnFile(structure, commitsPath + "/" + commitId + "/tree/" + "_treeObject.ser");
        for(File snippet: allSnippets){
            Long id = Long.parseLong(snippet.getName().split("_")[0]);
            fileUtil.createFile(commitsPath + "/" + commitId + "/snippets/" + snippet.getName(), filesContent.get(id));
        }
        setCurrentCommit(project, "main", commitId);
        writeOnLog(project, project.getClient(), commitId, "main", "Initial commit");
    }

    @Transactional
    public void createBranch(Project project, String branchName) {
        String vcsPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs";
        fileUtil.createFolderIfNotExists(vcsPath + "/branches");
        File file = new File(vcsPath + "/branches/" + branchName);
        if (file.exists()){
            throw new RuntimeException("Branch " + branchName + " already exists");
        }
        fileUtil.createFolderIfNotExists(vcsPath + "/branches/" + branchName);
        fileUtil.createFolderIfNotExists(vcsPath + "/branches/" + branchName + "/commits");
        fileUtil.writeObjectOnFile(new HashMap<>(), vcsPath + "/branches/" + branchName + "/changes");
        fileUtil.writeObjectOnFile(new HashMap<>(), vcsPath + "/branches/" + branchName + "/tracked");
        fileUtil.writeObjectOnFile(new ArrayList<>(), vcsPath + "/branches/" + branchName + "/log");
        fileUtil.createFile(vcsPath + "/branches/" + branchName + "/currentCommit", "");
    }

    @Transactional
    public void createBranch(Project project, String branchName, String prevBranchName) {
        String vcsPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs";
        String prevBranchPath = vcsPath + "/branches/" + prevBranchName;
        String newBranchPath = vcsPath + "/branches/" + branchName;
        fileUtil.copyDirectory(prevBranchPath, newBranchPath);
    }

    @Transactional
    public void deleteVCS(Project project) {
        long ownerId = project.getClient().getId();
        String vcsPath = paths.storageServicePath + "/" + ownerId + "/projects/" + project.getId() + "/.vcs";
        fileUtil.deleteFolder(vcsPath);
    }

    public String getCurrentBranch(Project project)  {
        String branchPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/HEAD";
        try{
            return fileUtil.readFileContents(branchPath);
        } catch (Exception e){
            throw new IllegalStateException("Couldn't get the current branch");
        }
    }

    public String getCurrentCommit(Project project, String branchName) {
        String currentCommitPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches/" + branchName + "/currentCommit";
        return fileUtil.readFileContents(currentCommitPath);
    }

    public Map<Long, ChangeHolder> readChanges(Project project, String branchName) {
        String changesPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches/" + branchName + "/changes";
        return (Map<Long, ChangeHolder>) fileUtil.readObjectFromFile(changesPath, new HashMap<>());
    }

    @Transactional
    protected void writeChanges(Project project, String branchName, Map<Long, ChangeHolder> changes){
        String changesPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches/" + branchName + "/changes";
        fileUtil.writeObjectOnFile(changes, changesPath);
    }

    @Transactional
    public void makeChange(Project project, String branchName, char fileType, Change changeType, FileItem fileItem) {
        if (!checkVCSProject(project)){
            System.out.println("Not a .vcs project");
            return;
        }
        Map<Long, ChangeHolder> changes = readChanges(project, branchName);
        Map<Long, ChangeHolder> tracked = readTracked(project, branchName);
        String filePath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/snippets/" + fileItem.getId() + "_" + fileItem.getName();
        String content = null;
        if (fileType == '-' && changeType != Change.DELETE){
            content = fileUtil.readFileContents(filePath);
        }
        if (changeType == Change.UPDATE){
            tracked.remove(fileItem.getId());
            String currentCommit = getCurrentCommit(project, branchName);
            String currentSnippetPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches/" + branchName + "/commits/" + currentCommit + "/snippets/" + fileItem.getId() + "_" + fileItem.getName();
            String currentSnippetContent = fileUtil.readFileContents(currentSnippetPath);
            if (currentSnippetContent.equals(content)){
                changes.remove(fileItem.getId());
                writeChanges(project, branchName, changes);
                return;
            }
        }
        ChangeHolder change = ChangeHolder.builder()
                .change(changeType)
                .fileType(fileType)
                .content(content)
                .build();
        changes.put(fileItem.getId(), change);

        tracked.remove(fileItem.getId());
        writeChanges(project, branchName, changes);
        writeTracked(project, branchName, tracked);
    }

    public Map<Long, ChangeHolder> readTracked(Project project, String branchName) {
        String changesPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches/" + branchName + "/tracked";
        return (Map<Long, ChangeHolder>) fileUtil.readObjectFromFile(changesPath, new HashMap<>());
    }

    @Transactional
    protected void writeTracked(Project project, String branchName, Map<Long, ChangeHolder> tracked){
        String changesPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches/" + branchName + "/tracked";
        fileUtil.writeObjectOnFile(tracked, changesPath);
    }

    @Transactional
    public Map<Long, ChangeHolder> trackChanges(Project project, String branchName, List<Long> filesIds) {
        Map<Long, ChangeHolder> tracked = readTracked(project, branchName);
        Map<Long, ChangeHolder> changes = readChanges(project, branchName);

        for(Long id : filesIds){
            FileItem file = fileItemRepository.findById(id).orElseThrow(
                    () -> new NoSuchElementException("No such file with id: " + id)
            );
            tracked.put(file.getId(), changes.get(file.getId()));
            changes.remove(file.getId());
        }
        writeChanges(project, branchName, changes);
        writeTracked(project, branchName, tracked);
        return changes;
    }

    @Transactional
    public Map<Long, ChangeHolder> trackAllChanges(Project project, String branchName) {
        Map<Long, ChangeHolder> tracked = readTracked(project, branchName);
        Map<Long, ChangeHolder> changes = readChanges(project, branchName);
        for(Long id: changes.keySet()){
            tracked.put(id, changes.get(id));
        }
        writeChanges(project, branchName, new HashMap<>());
        writeTracked(project, branchName, tracked);
        return changes;
    }

    @Transactional
    public Map<Long, ChangeHolder> commitTracked(Project project, String branchName, Client client, String message, String prevCommitId) {
        String commitId = client.getId().toString() + "%" + Instant.now().getEpochSecond() + "%" + message.hashCode();
        String commitsPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches/" + branchName + "/commits";
        Map<Long, ChangeHolder> tracked = readTracked(project, branchName);
        ProjectStructure projectStructure = null;
        boolean structureChanged = false;
        for(Long id : tracked.keySet()){
            ChangeHolder changeHolder = tracked.get(id);
            if (changeHolder.getChange() != Change.UPDATE){
                structureChanged = true;
                break;
            }
        }
        if (structureChanged){
            projectStructure = projectStorageService.loadProjectStructure(project.getClient(), project.getId());
        }
        File[] allSnippets = fileUtil.getSubFiles(paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/snippets");
        Map<Long, String> filesContent = new HashMap<>();
        for(File snippet : allSnippets){
            String name = snippet.getName();
            Long id = Long.parseLong(name.split("_")[0]);
            if (tracked.containsKey(id)){
                String content = tracked.get(id).getContent();
                filesContent.put(id, content);
            } else {
                filesContent.put(id, null);
            }
        }

        String prevCommitPath = commitsPath + "/" + prevCommitId;
        System.out.println(prevCommitPath);
        fileUtil.createFolder(commitsPath + "/" + commitId);
        fileUtil.createFolder(commitsPath + "/" + commitId + "/snippets");
        fileUtil.createFolder(commitsPath + "/" + commitId + "/tree");
        if (structureChanged){
            fileUtil.writeObjectOnFile(projectStructure, commitsPath + "/" + commitId + "/tree/" + "_treeObject.ser");
        } else {
            File prevProj = fileUtil.getSubFiles(prevCommitPath + "/tree")[0];
            if (!prevProj.getName().endsWith(".ser")){
                fileUtil.createFile(commitsPath + "/" + commitId + "/tree/" + "_treeObject", fileUtil.readFileContents(prevProj.getAbsolutePath()));
            } else {
                fileUtil.createLinkFile(prevProj.getAbsolutePath(), commitsPath + "/" + commitId + "/tree/" + "_treeObject");
            }
        }
        for(File snippet: allSnippets){
            Long id = Long.parseLong(snippet.getName().split("_")[0]);
            if (filesContent.get(id) == null){
                String prevFilePath = prevCommitPath + "/snippets/" + snippet.getName();
                if (!Files.exists(Paths.get(prevFilePath))){
                    prevFilePath = prevCommitPath + "/snippets/" + snippet.getName().split("/.")[0];
                }
                if (!(prevFilePath.endsWith(".cpp") || prevFilePath.endsWith(".java") || prevFilePath.endsWith(".py"))){
                    fileUtil.createFile( commitsPath + "/" + commitId + "/snippets/" + snippet.getName().split("/.")[0], fileUtil.readFileContents(prevFilePath));
                } else {
                    fileUtil.createLinkFile(prevFilePath, commitsPath + "/" + commitId + "/snippets/" + snippet.getName().split("/.")[0]);
                }
            } else {
                fileUtil.createFile(commitsPath + "/" + commitId + "/snippets/" + snippet.getName(), filesContent.get(id));
            }
        }
        writeTracked(project, branchName, new HashMap<>());
        writeOnLog(project, client, commitId, branchName, message);
        setCurrentCommit(project, branchName, commitId);
        return tracked;
    }

    public void setCurrentCommit(Project project, String branchName, String commitId){
        String currentPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches/" + branchName + "/currentCommit";
        fileUtil.writeOnFile(Paths.get(currentPath), commitId);
    }

    @Transactional
    public List<String> log(Project project, String branchName){
        String logPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches/" + branchName + "/log";
        return (List<String>) fileUtil.readObjectFromFile(logPath, new HashMap<>());
    }

    @Transactional
    public void writeOnLog(Project project, Client client, String commitId, String branchName, String message){
        String newLog = "Commit Id:\t" + commitId + "\nAuthor   :\t" + client.getName() + " " + client.getEmail() + "\nDate     :\t\t" + Instant.now() + "\n\tMessage: " + message;
        String logPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches/" + branchName + "/log";
        List<String> log = (List<String>) fileUtil.readObjectFromFile(logPath, new HashMap<>());
        log.add(newLog);
        fileUtil.writeObjectOnFile(log, logPath);
    }

    @Transactional
    public void revert(Project project, String branchName, String commitId)  {
        String projectPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId();
        fileUtil.deleteFolder(projectPath + "/snippets");
        fileUtil.deleteFolder(projectPath + "/tree");
        fileUtil.createFolder(projectPath + "/snippets");

        fileUtil.createFolder(projectPath + "/tree");
        String commitPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches/" + branchName + "/commits/" + commitId;
        String projectDirPath = commitPath + "/tree";
        File projectDir = fileUtil.getSubFiles(projectDirPath)[0];
        if (!projectDir.getName().endsWith(".ser")){
            fileUtil.createFile(projectPath + "/tree/" + "_treeObject", fileUtil.readFileContents(projectDir.getAbsolutePath()));
        } else {
            fileUtil.writeObjectOnFile(fileUtil.readObjectFromFile(projectDir.getAbsolutePath(), new HashMap<>()), projectPath + "/tree/" + "_treeObject.ser");
        }
        File[] snippets = fileUtil.getSubFiles(commitPath + "/snippets");
        for(File snippet: snippets){
            String content = fileUtil.readFileContents(snippet.getAbsolutePath());
            fileUtil.createFile(projectPath + "/snippets/" + snippet.getName(), content);
        }
        setCurrentCommit(project, branchName, commitId);
        writeTracked(project, branchName, new HashMap<>());
        writeChanges(project, branchName, new HashMap<>());
    }

    @Transactional
    public void fork(Project project, Client client) {
        String projectPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId();
        Project buildProject = Project.builder()
                .name(project.getName())
                .client(client)
                .build();
        if (!publicRepoStorageService.checkProjectPublic(project.getId())){
            throw new IllegalArgumentException("Cannot fork this project");
        }
        Project newProject = projectRepository.save(buildProject);
        String clientProjectsPath = paths.storageServicePath + "/" + client.getId() + "/projects/" + newProject.getId();
        projectStorageService.createProject(client, newProject);
        fileUtil.copyDirectory(projectPath + "/tree", clientProjectsPath + "/tree");
        fileUtil.copyDirectory(projectPath + "/snippets", clientProjectsPath + "/snippets");
    }

    @Transactional
    public void deleteBranch(Project project, String branchName) {
        if (Objects.equals(branchName, "main")){
            throw new IllegalStateException("Couldn't delete the defualt branch 'main'");
        }
        String branchPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches/" + branchName;
        fileUtil.deleteFolder(branchPath);
    }

    public void checkout(Project project, String branchName) {
        String vcsPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs";
        File[] branchces = fileUtil.getSubFiles(vcsPath + "/branches");
        boolean checkBranchExists = false;
        for(File branch: branchces){
            checkBranchExists |= branch.getName().equals(branchName);
        }
        if (!checkBranchExists){
            throw new IllegalArgumentException("Branch " + branchName + " does not exist");
        }
        String branchPath = vcsPath + "/HEAD";
        fileUtil.writeOnFile(Paths.get(branchPath), branchName);
        String currentCommitId = getCurrentCommit(project, branchName);
        revert(project, branchName, currentCommitId);
    }

    public List<String> allBranches(Project project) {
        String branchesPath = paths.storageServicePath + "/" + project.getClient().getId() + "/projects/" + project.getId() + "/.vcs/branches";
        File[] branches = fileUtil.getSubFiles(branchesPath);
        List<String> branchesNamse = new ArrayList<>();
        String currentBranch = getCurrentBranch(project);
        branchesNamse.add("* " + currentBranch);
        for (File file: branches){
            if (file.getName().equals(currentBranch)){
                continue;
            }
            branchesNamse.add(file.getName());
        }

        return branchesNamse;
    }
}