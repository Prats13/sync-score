package com.syncscore.v2.scanner;

import com.syncscore.v1.scanner.github.GitHubApiClient.TreeItem;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FolderStructureAnalyzerTest {

    private final FolderStructureAnalyzer analyzer = new FolderStructureAnalyzer();

    @Test
    void countsDepthAndServicesForTypicalMonorepo() {
        List<TreeItem> tree = List.of(
            new TreeItem("services/agent/src/main.py", "blob"),
            new TreeItem("services/api/src/app.py", "blob"),
            new TreeItem("services/worker/requirements.txt", "blob"),
            new TreeItem("infra/terraform/main.tf", "blob"),
            new TreeItem("README.md", "blob")
        );

        FolderStructureAnalyzer.Result result = analyzer.analyze(tree);

        assertThat(result.maxDepth()).isEqualTo(4);   // services/agent/src/main.py = 4 parts
        assertThat(result.serviceCount()).isEqualTo(3); // services/agent, services/api, services/worker
        assertThat(result.sourceFileCount()).isGreaterThan(0);
    }

    @Test
    void singleFlatRepoHasLowDepthAndNoServices() {
        List<TreeItem> tree = List.of(
            new TreeItem("main.py", "blob"),
            new TreeItem("requirements.txt", "blob")
        );

        FolderStructureAnalyzer.Result result = analyzer.analyze(tree);

        assertThat(result.maxDepth()).isEqualTo(1);
        assertThat(result.serviceCount()).isEqualTo(0);
    }

    @Test
    void emptyTreeReturnsZeros() {
        FolderStructureAnalyzer.Result result = analyzer.analyze(List.of());

        assertThat(result.maxDepth()).isEqualTo(0);
        assertThat(result.serviceCount()).isEqualTo(0);
        assertThat(result.sourceFileCount()).isEqualTo(0);
    }
}