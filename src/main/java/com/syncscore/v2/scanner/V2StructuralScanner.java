package com.syncscore.v2.scanner;

import com.syncscore.v1.scanner.github.GitHubApiClient;
import com.syncscore.v1.scanner.github.GitHubApiClient.RepoSummary;
import com.syncscore.v1.scanner.github.GitHubApiClient.TreeItem;
import com.syncscore.v1.scanner.GitHubScanProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class V2StructuralScanner {

    private final GitHubApiClient gitHubApi;
    private final GitHubScanProperties props;
    private final CommitActivityAnalyzer commitAnalyzer;
    private final FolderStructureAnalyzer folderAnalyzer;
    private final ManifestConsistencyChecker consistencyChecker;
    private final StructuralSignalAggregator aggregator;

    public V2StructuralScanner(GitHubApiClient gitHubApi,
                               GitHubScanProperties props,
                               CommitActivityAnalyzer commitAnalyzer,
                               FolderStructureAnalyzer folderAnalyzer,
                               ManifestConsistencyChecker consistencyChecker,
                               StructuralSignalAggregator aggregator) {
        this.gitHubApi = gitHubApi;
        this.props = props;
        this.commitAnalyzer = commitAnalyzer;
        this.folderAnalyzer = folderAnalyzer;
        this.consistencyChecker = consistencyChecker;
        this.aggregator = aggregator;
    }

    public StructuralSignalAggregator.AggregatedSignals scan(String githubUsername, int detectedPackageCount) {
        List<RepoSummary> repos = gitHubApi.listPublicReposByPushedDesc(githubUsername);
        int limit = Math.min(repos.size(), props.repoScanDefaultLimit());

        List<RepoStructuralSignals> repoSignals = new ArrayList<>();

        for (RepoSummary repo : repos.subList(0, limit)) {
            if (repo == null || repo.owner() == null || !StringUtils.hasText(repo.owner().login())) continue;

            String owner = repo.owner().login();
            String name = repo.name();
            String branch = StringUtils.hasText(repo.defaultBranch()) ? repo.defaultBranch() : "main";

            var tree = gitHubApi.fetchRepoTreeRecursive(owner, name, branch);
            List<TreeItem> treeItems = tree.tree() != null ? tree.tree() : List.of();

            FolderStructureAnalyzer.Result folderResult = folderAnalyzer.analyze(treeItems);

            Instant since90d = Instant.now().minus(90, ChronoUnit.DAYS);
            int commitCount = gitHubApi.listCommitCount(owner, name, since90d);
            int contributorCount = gitHubApi.listContributorCount(owner, name);

            repoSignals.add(new RepoStructuralSignals(
                    owner + "/" + name,
                    commitCount,
                    contributorCount,
                    repo.createdAt(),
                    folderResult.maxDepth(),
                    folderResult.serviceCount(),
                    folderResult.sourceFileCount(),
                    detectedPackageCount
            ));
        }

        return aggregator.aggregate(repoSignals);
    }
}