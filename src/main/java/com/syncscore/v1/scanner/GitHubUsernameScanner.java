package com.syncscore.v1.scanner;

import com.syncscore.v1.domain.ManifestType;
import com.syncscore.v1.scanner.github.GitHubApiClient;
import com.syncscore.v1.scanner.github.GitHubApiClient.RepoSummary;
import com.syncscore.v1.scanner.github.GitHubApiClient.TreeItem;
import com.syncscore.v1.scanner.github.GitHubApiClient.TreeResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GitHubUsernameScanner {

    private final GitHubApiClient gitHubApi;
    private final GitHubScanProperties props;

    public GitHubUsernameScanner(GitHubApiClient gitHubApi, GitHubScanProperties props) {
        this.gitHubApi = gitHubApi;
        this.props = props;
    }

    public GitHubScanResult scanUsername(String username, Integer requestedRepoLimit, ManifestDependencyExtractor extractor) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("username is required");
        }
        if (extractor == null) {
            throw new IllegalArgumentException("extractor is required");
        }

        int limit = normalizeRepoLimit(requestedRepoLimit);
        List<RepoSummary> repos = gitHubApi.listPublicReposByPushedDesc(username);
        List<RepoSummary> selected = repos.stream().limit(limit).toList();

        List<GitHubScanResult.ScannedRepo> scannedRepos = new ArrayList<>();
        Set<String> allPackages = new HashSet<>();

        for (RepoSummary repo : selected) {
            if (repo == null || repo.owner() == null || !StringUtils.hasText(repo.owner().login())) continue;
            String owner = repo.owner().login();
            String name = repo.name();
            String branch = StringUtils.hasText(repo.defaultBranch()) ? repo.defaultBranch() : "main";

            TreeResponse tree = gitHubApi.fetchRepoTreeRecursive(owner, name, branch);
            List<String> packageJsonPaths = new ArrayList<>();
            List<String> requirementsPaths = new ArrayList<>();
            List<String> pyprojectPaths = new ArrayList<>();
            List<String> setupCfgPaths = new ArrayList<>();
            List<String> pipfilePaths = new ArrayList<>();

            for (TreeItem item : tree.tree() == null ? List.<TreeItem>of() : tree.tree()) {
                if (item == null || !"blob".equalsIgnoreCase(item.type()) || item.path() == null) continue;
                String lower = item.path().toLowerCase(Locale.ROOT);
                if (lower.endsWith("package.json")) {
                    packageJsonPaths.add(item.path());
                } else if (lower.endsWith("requirements.txt") || lower.matches(".*requirements[-_][\\w]+\\.txt")) {
                    requirementsPaths.add(item.path());
                } else if (lower.endsWith("pyproject.toml")) {
                    pyprojectPaths.add(item.path());
                } else if (lower.endsWith("setup.cfg")) {
                    setupCfgPaths.add(item.path());
                } else if (lower.endsWith("pipfile") && !lower.endsWith(".lock")) {
                    pipfilePaths.add(item.path());
                }
            }

            List<GitHubScanResult.ManifestFile> manifestsFound = new ArrayList<>();

            for (String path : packageJsonPaths) {
                String content = gitHubApi.fetchFileContent(owner, name, path, branch);
                if (content != null) {
                    Set<String> pkgs = extractor.extractPackages(ManifestType.PACKAGE_JSON, content);
                    manifestsFound.add(new GitHubScanResult.ManifestFile(ManifestType.PACKAGE_JSON, path, pkgs));
                    allPackages.addAll(pkgs);
                }
            }

            for (String path : requirementsPaths) {
                String content = gitHubApi.fetchFileContent(owner, name, path, branch);
                if (content != null) {
                    Set<String> pkgs = extractor.extractPackages(ManifestType.REQUIREMENTS_TXT, content);
                    manifestsFound.add(new GitHubScanResult.ManifestFile(ManifestType.REQUIREMENTS_TXT, path, pkgs));
                    allPackages.addAll(pkgs);
                }
            }

            for (String path : pyprojectPaths) {
                String content = gitHubApi.fetchFileContent(owner, name, path, branch);
                if (content != null) {
                    Set<String> pkgs = extractor.extractPackages(ManifestType.PYPROJECT_TOML, content);
                    manifestsFound.add(new GitHubScanResult.ManifestFile(ManifestType.PYPROJECT_TOML, path, pkgs));
                    allPackages.addAll(pkgs);
                }
            }

            for (String path : setupCfgPaths) {
                String content = gitHubApi.fetchFileContent(owner, name, path, branch);
                if (content != null) {
                    Set<String> pkgs = extractor.extractPackages(ManifestType.SETUP_CFG, content);
                    manifestsFound.add(new GitHubScanResult.ManifestFile(ManifestType.SETUP_CFG, path, pkgs));
                    allPackages.addAll(pkgs);
                }
            }

            for (String path : pipfilePaths) {
                String content = gitHubApi.fetchFileContent(owner, name, path, branch);
                if (content != null) {
                    Set<String> pkgs = extractor.extractPackages(ManifestType.PIPFILE, content);
                    manifestsFound.add(new GitHubScanResult.ManifestFile(ManifestType.PIPFILE, path, pkgs));
                    allPackages.addAll(pkgs);
                }
            }

            scannedRepos.add(new GitHubScanResult.ScannedRepo(
                    owner,
                    name,
                    repo.htmlUrl(),
                    branch,
                    manifestsFound
            ));
        }

        return new GitHubScanResult(username, limit, scannedRepos, allPackages);
    }

    @Async("v1ScannerExecutor")
    public CompletableFuture<GitHubScanResult> scanUsernameAsync(String username, Integer requestedRepoLimit, ManifestDependencyExtractor extractor) {
        return CompletableFuture.completedFuture(scanUsername(username, requestedRepoLimit, extractor));
    }

    private int normalizeRepoLimit(Integer requestedRepoLimit) {
        int limit = requestedRepoLimit == null ? props.repoScanDefaultLimit() : requestedRepoLimit;
        if (limit <= 0) limit = props.repoScanDefaultLimit();
        if (limit > props.repoScanMaxLimit()) limit = props.repoScanMaxLimit();
        return limit;
    }
}
