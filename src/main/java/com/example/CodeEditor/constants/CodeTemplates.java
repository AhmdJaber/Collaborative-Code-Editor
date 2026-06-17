package com.example.CodeEditor.constants;

import org.springframework.stereotype.Service;

@Service
public class CodeTemplates {
    public final String cppTemplate = "#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n\t//Start Coding\n\tcout << \"Hello World!\";\n}";
    public final String javaTemplate = "public class HelloWorld {\n\n\tpublic static void main(String[] args) {\n\t\t//Start Coding\n\t\tSystem.out.println(\"Hello World!\");\n\t}\n}";
    public final String pyTemplate = "#Start Coding\nprint(\"Hello World!\")";
}
