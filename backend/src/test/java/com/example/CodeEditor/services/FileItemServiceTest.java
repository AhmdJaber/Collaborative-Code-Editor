package com.example.CodeEditor.services;

import com.example.CodeEditor.model.component.files.FileItem;
import com.example.CodeEditor.repository.FileItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileItemServiceTest {

    @Mock
    private FileItemRepository fileItemRepository;

    @InjectMocks
    private FileItemService fileItemService;

    @Test
    void getFileByIdReturnsFile() {
        FileItem fileItem = fileItem(1L, "src");
        when(fileItemRepository.findById(1L)).thenReturn(Optional.of(fileItem));

        assertEquals(fileItem, fileItemService.getFileById(1L));
    }

    @Test
    void createFileSavesItem() {
        FileItem fileItem = fileItem(1L, "src");
        when(fileItemRepository.save(fileItem)).thenReturn(fileItem);

        assertEquals(fileItem, fileItemService.createFile(fileItem));
        verify(fileItemRepository).save(fileItem);
    }

    @Test
    void removeFileThrowsWhenMissing() {
        when(fileItemRepository.existsById(1L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> fileItemService.removeFile(1L));
    }

    @Test
    void removeFileDeletesExistingFile() {
        when(fileItemRepository.existsById(1L)).thenReturn(true);

        fileItemService.removeFile(1L);

        verify(fileItemRepository).deleteById(1L);
    }

    private FileItem fileItem(Long id, String name) {
        FileItem fileItem = new FileItem();
        fileItem.setId(id);
        fileItem.setName(name);
        return fileItem;
    }
}
