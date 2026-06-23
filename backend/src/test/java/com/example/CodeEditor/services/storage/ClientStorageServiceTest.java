package com.example.CodeEditor.services.storage;

import com.example.CodeEditor.constants.FilesystemPaths;
import com.example.CodeEditor.model.clients.Client;
import com.example.CodeEditor.utils.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ClientStorageServiceTest {

    @Mock
    private FileUtil fileUtil;

    @Mock
    private FilesystemPaths filesystemPaths;

    @InjectMocks
    private ClientStorageService clientStorageService;

    private Client client;
    private Long clientId;

    @BeforeEach
    void setUp() {
        client = new Client();
        clientId = 25L;
        client.setId(clientId);
        filesystemPaths.storageServicePath = "backend/src/main/resources/editors";
    }

    @Test
    void createClientCreatesRequiredFoldersAndPublicFile() {
        clientStorageService.createClient(client);

        String basePath = filesystemPaths.storageServicePath + "/" + clientId;
        ArgumentCaptor<List<String>> foldersCaptor = ArgumentCaptor.forClass(List.class);

        verify(fileUtil).createFolders(foldersCaptor.capture());
        assertEquals(List.of(
                basePath,
                basePath + "/projects",
                basePath + "/shared",
                basePath + "/shared_view"
        ), foldersCaptor.getValue());

        ArgumentCaptor<Object> contentCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(fileUtil).writeObjectOnFile(contentCaptor.capture(), pathCaptor.capture());

        assertEquals(basePath + "/public", pathCaptor.getValue());
        assertInstanceOf(ArrayList.class, contentCaptor.getValue());
        assertEquals(0, ((ArrayList<?>) contentCaptor.getValue()).size());

        verifyNoMoreInteractions(fileUtil);
    }

    @Test
    void createClientWrapsFileUtilFailure() {
        doThrow(new RuntimeException("boom")).when(fileUtil).createFolders(any());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> clientStorageService.createClient(client));

        assertEquals("Failed to create folder " + filesystemPaths.storageServicePath + "/" + clientId, exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception.getCause());
        verify(fileUtil).deleteFolders(List.of(
                filesystemPaths.storageServicePath + "/" + clientId,
                filesystemPaths.storageServicePath + "/" + clientId + "/projects",
                filesystemPaths.storageServicePath + "/" + clientId + "/shared",
                filesystemPaths.storageServicePath + "/" + clientId + "/shared_view"
        ));
    }

    @Test
    void deleteClientDeletesUserFolder() {
        clientStorageService.deleteClient(clientId);

        verify(fileUtil).deleteFolder(filesystemPaths.storageServicePath + "/" + clientId);
        verifyNoMoreInteractions(fileUtil);
    }
}
