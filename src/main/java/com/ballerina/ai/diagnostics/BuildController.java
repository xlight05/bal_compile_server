/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.ballerina.ai.diagnostics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.EnvironmentBuilder;
import io.ballerina.projects.repos.TempDirCompilationCache;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * Controller for building bala files.
 */
@RestController
@RequestMapping("/project")
public class BuildController {

    private static final Logger LOG = LoggerFactory.getLogger(BuildController.class);
//    private  static final String DIST_PATH = "/Library/Ballerina/distributions/ballerina-2201.8.2";
//    private static final Path BALLERINA_HOME = Paths.get(DIST_PATH);
    private static final Path BALLERINA_HOME = Paths.get(System.getenv("BALLERINA_HOME"));
//
    private static final Path USER_HOME = Paths.get(System.getenv("USER_HOME") == null ?
            System.getProperty("user.home") : System.getenv("USER_HOME"));

    @PostMapping("/diagnostics")
    public ResponseEntity<?> diagnostics(@RequestBody Request req) throws IOException {

        Path balFileSource = Files.createTempFile("balSource", ".bal");
        try (FileOutputStream fileOutputStream = new FileOutputStream(balFileSource.toFile())) {
            fileOutputStream.write(req.getSourceCode().getBytes(StandardCharsets.UTF_8));
        }
        SingleFileProject balaProject = buildBala(balFileSource);
        PackageCompilation compilation = balaProject.currentPackage().getCompilation();
        Collection<Diagnostic> diagnostics = compilation.diagnosticResult().errors();

        ObjectMapper objectMapper = new ObjectMapper();

        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (Diagnostic diagnostic : diagnostics) {
            arrayNode.add(diagnostic.toString());
        }

        ObjectNode diags = objectMapper.createObjectNode();

        diags.set("diagnostics", arrayNode);
        Files.deleteIfExists(balFileSource);

        // Set response
        return ResponseEntity.ok(diags);
    }

    @PostMapping("/fix-diags")
    public ResponseEntity<?> postProcess(@RequestBody Request req) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();

        ObjectNode resp = objectMapper.createObjectNode();
        PostProcessor postProcessor = new PostProcessor();
        DiagnosticResponse response = postProcessor.fixDiagnostics(req.getSourceCode());
        Collection<Diagnostic> diagnostics = response.diagnostics;
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (Diagnostic diagnostic : diagnostics) {
            arrayNode.add(diagnostic.toString());
        }

        resp.put("code", response.sourceCode);
        resp.set("diagnostics", arrayNode);

        // Set response
        return ResponseEntity.ok(resp);
    }

    public static SingleFileProject buildBala(Path balaPath) throws IOException {
        // Remove this condition when https://github.com/ballerina-platform/ballerina-lang/issues/29169 is resolved
        if (Files.notExists(USER_HOME)) {
            Files.createDirectories(USER_HOME);
        }

        Environment environment = EnvironmentBuilder.getBuilder()
                .setBallerinaHome(BALLERINA_HOME)
                .setUserHome(USER_HOME.resolve(".ballerina")).build();
        ProjectEnvironmentBuilder defaultBuilder = ProjectEnvironmentBuilder.getBuilder(environment);
        defaultBuilder.addCompilationCacheFactory(TempDirCompilationCache::from);
        return SingleFileProject.load(defaultBuilder, balaPath);
    }

}
