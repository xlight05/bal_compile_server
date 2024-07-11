package com.ballerina.ai.diagnostics;

import io.ballerina.tools.diagnostics.Diagnostic;

import java.util.Collection;

/**
 * Represents a DiagnosticResponse for the application.
 */
public class DiagnosticResponse {
    public DiagnosticResponse(String sourceCode, Collection<Diagnostic> diagnostics) {
        this.sourceCode = sourceCode;
        this.diagnostics = diagnostics;
    }

    public String sourceCode;
    public Collection<Diagnostic> diagnostics;
}
