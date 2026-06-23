package com.example.CodeEditor.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileUtil {
    @Autowired
    private EncryptionUtil encryptionUtil;

    public void createFolder(String folderPath){
        System.out.println("Creating folder: " + folderPath);
        File folder = new File(folderPath);
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                System.out.println(folderPath);
                throw new IllegalStateException("Something Went Wrong While Creating The Folder");
            }
        }
    }

    public void createFile(String filePath, String content){
        Path fileFullPath = Paths.get(filePath);
        try{
            if (!Files.exists(fileFullPath)) {
                Files.createFile(fileFullPath);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not create the file " + fileFullPath);
        }
        writeOnFile(fileFullPath, content);
    }

    public void writeOnFile(Path fileFullPath, String content){
        try{
            Files.write(fileFullPath, content.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not write on the file " + fileFullPath);
        }
    }


    public void createLinkFile(String originalPath, String linkPath){
        Path link = Paths.get(linkPath);
        String ecryptedOriginalPath;
        try{
            ecryptedOriginalPath = encryptionUtil.encrypt(originalPath);
            Files.write(link, ecryptedOriginalPath.getBytes());
        } catch (Exception e){
            throw new IllegalArgumentException("Could not encrypt the file " + linkPath);
        }

    }

    public File[] getSubFiles(String folderPath){
        File folder = new File(folderPath);
        if (!folder.exists()) {
            return new File[]{};
        }
        return folder.listFiles();
    }

    public void deleteFile(String filePath){
        Path fileFullPath = Paths.get(filePath);
        if(!Files.exists(fileFullPath)){
            throw new IllegalArgumentException("No such file " + fileFullPath);
        }
        try{
            Files.delete(fileFullPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not delete the file " + fileFullPath);
        }
    }

    public void deleteFolder(String folderPath){
        File folder = new File(folderPath);

        if(folder.exists() && folder.isDirectory()){
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteFolder(file.getAbsolutePath());
                    }
                    else if (!file.delete()) {
                        throw new IllegalStateException("Something Went Wrong While Deleting The File " + file.getAbsolutePath());
                    }
                }
            }

            if (!folder.delete()) {
                throw new IllegalStateException("Something Went Wrong While Deleting The Folder " + folder.getAbsolutePath());
            }
        }
    }

    public String readFileContents(String fullPath) {
        Path filePath = Path.of(fullPath);
        try {
            return Files.readString(filePath);
        } catch (Exception e){
            throw new IllegalArgumentException("Could not read the file " + fullPath);
        }
    }

    public void writeObjectOnFile(Object object, String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not create the file " + filePath);
            }
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(object);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to write the object on the path " + filePath);
        }
    }

    public Object readObjectFromFile(String filePath, Object object) {
        if (!Files.exists(Paths.get(filePath))){
            writeObjectOnFile(object, filePath);
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException("Error deserializing the Object from path " + filePath);
        }
    }

    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    public void copyDirectory(String source, String target) {
        File sourceFile = new File(source);
        File targetFile = new File(target);
        try{
            copyDirectory(sourceFile, targetFile);
        } catch (IOException e){
            throw new RuntimeException("Failed to copy directory from " + source + " to " + target);
        }
    }

    private void copyDirectory(File sourceDirectory, File destinationDirectory) throws IOException {
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
        }
        for (String f : sourceDirectory.list()) {
            copyDirectoryCompatibityMode(new File(sourceDirectory, f), new File(destinationDirectory, f));
        }
    }

    public void copyDirectoryCompatibityMode(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            copyDirectory(source, destination);
        } else {
            copyFile(source, destination);
        }
    }

    private void copyFile(File sourceFile, File destinationFile)
            throws IOException {
        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = new FileOutputStream(destinationFile)) {
            byte[] buf = new byte[1024];
            int length;
            while ((length = in.read(buf)) > 0) {
                out.write(buf, 0, length);
            }
        }
    }

    public void createFolderIfNotExists(String path){
        File file = new File(path);
        if (!file.exists()){
            if (!file.mkdirs()){
                throw new IllegalStateException("Failed to create folder " + path);
            } else {
                System.out.println("Folder " + path + " created!");
            }
        }
    }
}
