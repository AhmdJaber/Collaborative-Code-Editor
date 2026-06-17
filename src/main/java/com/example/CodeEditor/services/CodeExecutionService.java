package com.example.CodeEditor.services;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {

    public String executeCode(String code, String language) {
        return switch (language) {
            case "cpp" -> executeCppCode(code);
            case "java" -> executeJavaCode(code);
            case "python" -> executePythonCode(code);
            default -> "Invalid language";
        };
    }

    private String executeJavaCode(String code) {
        code = normalizeCode(code);
        Path tempDirectory = null;

        try {
            String className = extractClassName(code);
            tempDirectory = Files.createTempDirectory("javaCodeExecution");
            Path javaFilePath = tempDirectory.resolve(className + ".java");
            Files.writeString(javaFilePath, code);

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "docker", "run", "--rm", "-v", tempDirectory + ":/app",
                    "eclipse-temurin:17", "sh", "-c",
                    "javac /app/" + className + ".java && java -cp /app " + className
            );

            return executeProcess(processBuilder);
        } catch (Exception e) {
            throw new RuntimeException("Java code execution error: " + e.getMessage());
        } finally {
            cleanupDirectory(tempDirectory);
        }
    }

    private String executeCppCode(String code) {
        code = normalizeCode(code);
        Path tempDirectory = null;

        try {
            tempDirectory = Files.createTempDirectory("cppCodeExecution");
            Path cppFilePath = tempDirectory.resolve("CppCode.cpp");
            Files.writeString(cppFilePath, code);

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "docker", "run", "--rm", "-v", tempDirectory + ":/app",
                    "gcc:latest", "sh", "-c",
                    "g++ /app/CppCode.cpp -o /app/CppCode && /app/CppCode"
            );

            return executeProcess(processBuilder);
        } catch (Exception e) {
            throw new RuntimeException("C++ code execution error: " + e.getMessage());
        } finally {
            cleanupDirectory(tempDirectory);
        }
    }

    private String executePythonCode(String code) {
        code = normalizeCode(code);
        Path tempDirectory = null;

        try {
            tempDirectory = Files.createTempDirectory("pythonCodeExecution");
            Path pythonFilePath = tempDirectory.resolve("TempCode.py");
            Files.writeString(pythonFilePath, code);

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "docker", "run", "--rm", "-v", tempDirectory + ":/app",
                    "python:3", "python", "/app/TempCode.py"
            );

            return executeProcess(processBuilder);
        } catch (Exception e) {
            throw new RuntimeException("Python code execution error: " + e.getMessage());
        } finally {
            cleanupDirectory(tempDirectory);
        }
    }

    private String executeProcess(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        Process process = processBuilder.start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroy();
            return "Execution timed out";
        }

        String output = new String(process.getInputStream().readAllBytes());
        String errorOutput = new String(process.getErrorStream().readAllBytes());

        return errorOutput.isEmpty() ? output : "Execution failed: " + errorOutput;
    }

    private void cleanupDirectory(Path directory) {
        if (directory != null && Files.exists(directory)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        cleanupDirectory(entry);
                    } else {
                        Files.deleteIfExists(entry);
                    }
                }
            } catch (IOException ignored) {}
            try {
                Files.deleteIfExists(directory);
            } catch (IOException ignored) {}
        }
    }

    private String extractClassName(String code) {
        String[] words = code.split(" ");
        for (int i = 0; i < words.length - 2; i++) {
            if (words[i].equals("public") && words[i + 1].equals("class")) {
                return words[i + 2];
            }
        }
        throw new RuntimeException("Class not found");
    }

    private String normalizeCode(String code) {
        return code.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"");
    }
}
