package com.ballerina.ai.diagnostics;

import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.text.LineRange;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ballerina.ai.diagnostics.BuildController.buildBala;

/**
 * Post processor for fixing diagnostics.
 */
public class PostProcessor {

    int compilations = 0;

    //TODO properly add or remove nodes without string manipulation
    public DiagnosticResponse fixDiagnostics(String sourceCode) throws Exception {

        Collection<Diagnostic> result = getCompileAndGetDiagnostics(sourceCode);
        if (result.isEmpty()) {
            return new DiagnosticResponse(sourceCode, result);
        }
        // Remove unused imports
        List<String> importsToRemove = new ArrayList<>();
        List<String> importsToAdd = new ArrayList<>();
        List<LineRange> lineRanges = new ArrayList<>();
        for (Diagnostic diagnostic : result) {
            DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
            if (diagnosticInfo.code().equals("BCE2002") || diagnosticInfo.code().equals("BCE2004")) {
                importsToRemove.add(getLineContent(sourceCode,
                        diagnostic.location().lineRange().startLine().line() + 1) + "\n");
            }
            if (diagnosticInfo.code().equals("BCE2000")) {
                boolean modNotFondExist = false;
                for (Diagnostic diag : result) {
                    //TODO: Check the exact alias
                    if (diag.diagnosticInfo().code().equals("BCE2003")) {
                        String lib = extractContentInsideSingleQuotes(diagnostic.message());
                        if (lib.equals("runtime")) {
                            importsToRemove.add(getLineContent(sourceCode,
                                    diagnostic.location().lineRange().startLine().line() + 1) + "\n");
                        } else {
                            //alias
                            modNotFondExist = true;
                        }
                    }
                }
                if (!modNotFondExist) {
                    String lib = extractContentInsideSingleQuotes(diagnostic.message());
                    if (lib.equals("io") || lib.equals("http") || lib.equals("log")) {
                        importsToAdd.add("import ballerina/" + lib + ";");
                    }
                    if (lib.equals("runtime")) {
                        importsToAdd.add("import ballerina/lang.runtime;");
                    }
                }
            }
            if (diagnosticInfo.code().equals("BCE0600")) {
                lineRanges.add(diagnostic.location().lineRange());
            }
            //TODO Refactor see if we can handle ones where we get missing identifiers.
        }

        for (String importToRemove : importsToRemove) {
            sourceCode = sourceCode.replaceFirst(importToRemove, "");
        }

        for (String importToAdd : importsToAdd) {
            sourceCode = importToAdd + "\n" + sourceCode;
        }

        CodeProcessor codeProcessor = new CodeProcessor();
        //TODO: Index should only be used in the same line
        for (int i = 0; i < lineRanges.size(); i++) {
            LineRange lineRange = lineRanges.get(i);
            int startLine = lineRange.startLine().line();
            int startoffset = lineRange.startLine().offset() + i;

            int endLine = lineRange.endLine().line();
            int endoffset = lineRange.endLine().offset() + i;

            sourceCode = codeProcessor.replaceWordAtPosition(sourceCode, startLine, startoffset, endLine, endoffset);
        }

        this.compilations++;
        //TODO See if we can get diagnostics all at once.
        if (compilations < 2) {
            sourceCode = fixDiagnostics(sourceCode).sourceCode;
        }
        // Compile again
        Collection<Diagnostic> diags = getCompileAndGetDiagnostics(sourceCode);

        return new DiagnosticResponse(sourceCode, diags);
    }

    private static Collection<Diagnostic> getCompileAndGetDiagnostics(String sourceCode) throws IOException {

        Path balFileSource = Files.createTempFile("balSource", ".bal");
        try (FileOutputStream fileOutputStream = new FileOutputStream(balFileSource.toFile())) {
            fileOutputStream.write(sourceCode.getBytes(StandardCharsets.UTF_8));
        }

        SingleFileProject balaProject = buildBala(balFileSource);
        PackageCompilation compilation = balaProject.currentPackage().getCompilation();
        Files.deleteIfExists(balFileSource);
        return compilation.diagnosticResult().errors();
    }

    public static String extractContentInsideSingleQuotes(String input) {

        String patternString = "'([^']*)'";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null; // Return null if no match is found
    }

    public static String getLineContent(String multiLineString, int lineNumber) throws Exception {

        String[] lines = multiLineString.split("\n");

        if (lineNumber < 1 || lineNumber > lines.length) {
            throw new Exception("Invalid line number: " + lineNumber);
        }

        return lines[lineNumber - 1];
    }
}
