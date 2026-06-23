package com.example.CodeEditor.services;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodeExecutionServiceTest {

    private final CodeExecutionService codeExecutionService = new CodeExecutionService();

    @Test
    void executeCodeReturnsInvalidLanguageForUnsupportedLanguages() {
        assertEquals("Invalid language", codeExecutionService.executeCode("print(1)", "ruby"));
    }

    @Test
    void extractClassNameParsesPublicClassName() throws Exception {
        Method method = CodeExecutionService.class.getDeclaredMethod("extractClassName", String.class);
        method.setAccessible(true);

        assertEquals("HelloWorld", method.invoke(codeExecutionService,
                "public class HelloWorld { public static void main(String[] args) {} }"));
    }

    @Test
    void extractClassNameThrowsWhenClassMissing() throws Exception {
        Method method = CodeExecutionService.class.getDeclaredMethod("extractClassName", String.class);
        method.setAccessible(true);

        assertThrows(Exception.class, () -> method.invoke(codeExecutionService, "class Missing {}"));
    }
}
