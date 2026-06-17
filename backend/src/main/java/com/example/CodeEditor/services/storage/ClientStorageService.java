package com.example.CodeEditor.services.storage;

import com.example.CodeEditor.constants.FilesystemPaths;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.utils.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class ClientStorageService {
    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private FilesystemPaths filesystemPaths;

    @Transactional
    public void createClient(Client client){
        String userPath = filesystemPaths.storageServicePath + "/" + client.getId();
        try{
            fileUtil.createFolder(userPath);
            fileUtil.createFolder(userPath + "/projects");
            fileUtil.createFolder(userPath + "/shared");
            fileUtil.createFolder(userPath + "/shared_view");
            fileUtil.writeObjectOnFile(new ArrayList<>(), userPath + "/public");
        } catch (Exception e){
            throw new IllegalStateException("Failed to create folder " + userPath, e);
        }
    }

    @Transactional
    public void deleteClient(Long clientId){
        String userPath = filesystemPaths.storageServicePath + "/" + clientId;
        fileUtil.deleteFolder(userPath);
    }

}
