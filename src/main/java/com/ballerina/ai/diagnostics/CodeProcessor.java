package com.ballerina.ai.diagnostics;

import io.ballerina.compiler.syntax.tree.SyntaxInfo;

/**
 * Code processor for handling code manipulations.
 */
public class CodeProcessor {


    public String replaceWordAtPosition(String sourceCode, int startLineNumber, int startOffset, int endLineNumber,
                                        int endOffset) {

        String[] lines = sourceCode.split("\n");
        if (startLineNumber >= lines.length || endLineNumber >= lines.length) {
            return sourceCode; // Return original source code if line number is out of range
        }

        String startLine = lines[startLineNumber]; // Array index starts from 0
        String endLine = lines[endLineNumber];
        if (startOffset > startLine.length() || endOffset > endLine.length()) {
            return sourceCode; // Return original source code if offset is out of range
        }

        if (startLineNumber == endLineNumber) {
            String wordToReplace = startLine.substring(startOffset, endOffset);
            if (!SyntaxInfo.isKeyword(wordToReplace.trim())) {
                return sourceCode; // Return original source code if the word is not a keyword
            }
            String newWord = "'" + wordToReplace.trim();
            startLine = startLine.substring(0, startOffset) + newWord + startLine.substring(endOffset);
            lines[startLineNumber] = startLine; // Update the line in the array
        } else {
            // Handle multi-line replacement
            String startPart = startLine.substring(startOffset); // From startOffset to the end of startLine
            String endPart = endLine.substring(0, endOffset); // From the beginning of endLine to endOffset
            String wordToReplace = startPart + "\n" + endPart;
            if (!SyntaxInfo.isKeyword(wordToReplace.trim())) {
                return sourceCode; // Return original source code if the word is not a keyword
            }
            String newWord = "'" + wordToReplace.trim();

            // Replace start and end lines
            lines[startLineNumber] = startLine.substring(0, startOffset) + newWord;
            lines[endLineNumber] = endLine.substring(endOffset);

            // Remove lines in between if there are any
            if (startLineNumber + 1 <= endLineNumber - 1) {
                String[] newLines = new String[lines.length - (endLineNumber - startLineNumber - 1)];
                System.arraycopy(lines, 0, newLines, 0, startLineNumber + 1);
                System.arraycopy(lines, endLineNumber, newLines, startLineNumber + 1,
                        lines.length - endLineNumber);
                lines = newLines;
            }
        }

        return String.join("\n", lines); // Join the lines back into a single string
    }

}
