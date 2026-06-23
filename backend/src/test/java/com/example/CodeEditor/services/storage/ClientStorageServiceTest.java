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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

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

    @Spy
    private final FilesystemPaths filesystemPaths = new FilesystemPaths();

    @InjectMocks
    private ClientStorageService clientStorageService;

    private Client client;
    private Long clientId;

    @BeforeEach
    void setUp() {
        client = new Client();
        clientId = 25L;
        client.setId(clientId);
    }

    @Test
    void createClientCreatesRequiredFoldersAndPublicFile() {
        clientStorageService.createClient(client);

        String basePath = filesystemPaths.storageServicePath + "/" + clientId;

        verify(fileUtil).createFolder(basePath);
        verify(fileUtil).createFolder(basePath + "/projects");
        verify(fileUtil).createFolder(basePath + "/shared");
        verify(fileUtil).createFolder(basePath + "/shared_view");

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
        doThrow(new RuntimeException("boom")).when(fileUtil).createFolder(any());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> clientStorageService.createClient(client));

        assertEquals("Failed to create folder " + filesystemPaths.storageServicePath + "/" + clientId, exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception.getCause());
    }

    @Test
    void deleteClientDeletesUserFolder() {
        clientStorageService.deleteClient(clientId);

        verify(fileUtil).deleteFolder(filesystemPaths.storageServicePath + "/" + clientId);
        verifyNoMoreInteractions(fileUtil);
    }
}
