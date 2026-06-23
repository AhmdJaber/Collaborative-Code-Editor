package com.example.CodeEditor.services.storage;

import com.example.CodeEditor.constants.FilesystemPaths;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.utils.FileUtil;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ClientStorageService {
    private FileUtil fileUtil;
    private FilesystemPaths filesystemPaths;

    public void createClient(Client client){
        String userPath = filesystemPaths.storageServicePath + "/" + client.getId();
        List<String> folderPaths = List.of(userPath, userPath + "/projects", userPath + "/shared", userPath + "/shared_view");

        try{
            fileUtil.createFolders(folderPaths);
            fileUtil.writeObjectOnFile(new ArrayList<>(), userPath + "/public");
        } catch (Exception e){
            fileUtil.deleteFolders(folderPaths);
            throw new IllegalStateException("Failed to create folder " + userPath, e);
        }
    }

    public void deleteClient(Long clientId){
        String userPath = filesystemPaths.storageServicePath + "/" + clientId;
        fileUtil.deleteFolder(userPath);
    }

}
