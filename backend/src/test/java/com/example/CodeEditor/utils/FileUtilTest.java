package com.example.CodeEditor.utils;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileUtilTest {

    @Mock
    private EncryptionUtil encryptionUtil;

    @Spy
    @InjectMocks
    private FileUtil fileUtil;

    @TempDir
    Path tempDir;

    @Test
    void createFolderCreatesDirectory() {
        Path folder = tempDir.resolve("editor");

        fileUtil.createFolder(folder.toString());

        assertTrue(Files.isDirectory(folder));
    }

    @Test
    @Disabled
    void createFolderCreatesNestedDirectoryWhenParentDoesNotExist() {
        Path nestedFolder = tempDir.resolve("missing-parent").resolve("child");

        fileUtil.createFolder(nestedFolder.toString());

        assertTrue(Files.isDirectory(nestedFolder));
    }

    @Test
    void createFileCreatesFileAndWritesContent() throws Exception {
        Path file = tempDir.resolve("hello.txt");

        fileUtil.createFile(file.toString(), "hello world");

        assertTrue(Files.exists(file));
        assertEquals("hello world", Files.readString(file));
    }

    @Test
    void writeOnFileOverwritesExistingContent() throws Exception {
        Path file = tempDir.resolve("data.txt");
        Files.writeString(file, "old");

        fileUtil.writeOnFile(file, "new");

        assertEquals("new", Files.readString(file));
    }

    @Test
    void createLinkFileWritesEncryptedPath() throws Exception {
        Path link = tempDir.resolve("link.txt");
        when(encryptionUtil.encrypt("/original/path")).thenReturn("encrypted-value");

        fileUtil.createLinkFile("/original/path", link.toString());

        assertEquals("encrypted-value", Files.readString(link));
    }

    @Test
    void getSubFilesReturnsEmptyArrayWhenFolderDoesNotExist() {
        File[] files = fileUtil.getSubFiles(tempDir.resolve("missing").toString());

        assertEquals(0, files.length);
    }

    @Test
    void getSubFilesReturnsChildrenWhenFolderExists() throws Exception {
        Path folder = tempDir.resolve("folder");
        Files.createDirectories(folder);
        Files.createFile(folder.resolve("a.txt"));
        Files.createDirectory(folder.resolve("sub"));

        File[] files = fileUtil.getSubFiles(folder.toString());

        assertEquals(2, files.length);
    }

    @Test
    void deleteFileRemovesExistingFile() throws Exception {
        Path file = tempDir.resolve("delete-me.txt");
        Files.writeString(file, "remove");

        fileUtil.deleteFile(file.toString());

        assertFalse(Files.exists(file));
    }

    @Test
    void deleteFileThrowsForMissingFile() {
        Path file = tempDir.resolve("missing.txt");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileUtil.deleteFile(file.toString()));

        assertTrue(exception.getMessage().contains("No such file"));
    }

    @Test
    void deleteFolderRemovesNestedStructure() throws Exception {
        Path root = tempDir.resolve("root");
        Path nested = root.resolve("nested");
        Files.createDirectories(nested);
        Files.writeString(root.resolve("a.txt"), "a");
        Files.writeString(nested.resolve("b.txt"), "b");

        fileUtil.deleteFolder(root.toString());

        assertFalse(Files.exists(root));
    }

    @Test
    void deleteFolderRestoresOriginalFolderWhenDeletionFails() throws Exception {
        Path root = tempDir.resolve("rollback-root");
        Path nested = root.resolve("nested");
        Files.createDirectories(nested);
        Files.writeString(root.resolve("a.txt"), "a");
        Files.writeString(nested.resolve("b.txt"), "b");

        doThrow(new IOException("boom")).when(fileUtil).deleteRecursively(any(Path.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> fileUtil.deleteFolder(root.toString()));

        assertEquals("Error while deleting the folder, rolled back", exception.getMessage());
        assertTrue(Files.exists(root));
        assertTrue(Files.isDirectory(root));
        assertTrue(Files.exists(root.resolve("a.txt")));
        assertTrue(Files.exists(root.resolve("nested").resolve("b.txt")));
    }

    @Test
    void readFileContentsReadsText() throws Exception {
        Path file = tempDir.resolve("read.txt");
        Files.writeString(file, "read me");

        assertEquals("read me", fileUtil.readFileContents(file.toString()));
    }

    @Test
    void writeAndReadObjectRoundTripsObject() {
        Path file = tempDir.resolve("object.bin");
        List<String> data = new ArrayList<>(List.of("one", "two"));

        fileUtil.writeObjectOnFile(data, file.toString());
        Object read = fileUtil.readObjectFromFile(file.toString(), List.of());

        assertInstanceOf(List.class, read);
        assertEquals(data, read);
    }

    @Test
    void fileExistsReturnsTrueForExistingFile() throws Exception {
        Path file = tempDir.resolve("exists.txt");
        Files.writeString(file, "yes");

        assertTrue(fileUtil.fileExists(file.toString()));
    }

    @Test
    void copyDirectoryCopiesNestedFiles() throws Exception {
        Path source = tempDir.resolve("source");
        Path nested = source.resolve("nested");
        Path target = tempDir.resolve("target");
        Files.createDirectories(nested);
        Files.writeString(source.resolve("a.txt"), "alpha");
        Files.writeString(nested.resolve("b.txt"), "beta");

        fileUtil.copyDirectory(source.toString(), target.toString());

        assertEquals("alpha", Files.readString(target.resolve("a.txt")));
        assertEquals("beta", Files.readString(target.resolve("nested").resolve("b.txt")));
    }

    @Test
    void createFolderIfNotExistsCreatesMissingFolder() {
        Path folder = tempDir.resolve("new-folder");

        fileUtil.createFolderIfNotExists(folder.toString());

        assertTrue(Files.isDirectory(folder));
    }

    @Test
    void createFolderIfNotExistsCreatesNestedDirectoryWhenParentDoesNotExist() {
        Path nestedFolder = tempDir.resolve("missing-parent").resolve("child");

        fileUtil.createFolderIfNotExists(nestedFolder.toString());

        assertTrue(Files.isDirectory(nestedFolder));
    }

}
