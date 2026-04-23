package com.syncscore.v2.scanner;

import com.syncscore.v1.scanner.github.GitHubApiClient;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CommitActivityAnalyzer {

    private final GitHubApiClient gitHubApi;

    public CommitActivityAnalyzer(GitHubApiClient gitHubApi) {
        this.gitHubApi = gitHubApi;
    }

    public Result analyze(String owner, List<String> repoNames) {
        if (repoNames == null || repoNames.isEmpty()) {
            return new Result(0, 0);
        }
        Instant since = Instant.now().minus(90, ChronoUnit.DAYS);

        int totalCommits = 0;
        int maxContributors = 0;

        for (String repo : repoNames) {
            totalCommits += gitHubApi.listCommitCount(owner, repo, since);
            int contributors = gitHubApi.listContributorCount(owner, repo);
            if (contributors > maxContributors) {
                maxContributors = contributors;
            }
        }
        return new Result(totalCommits, maxContributors);
    }

    public record Result(int totalCommits90d, int maxContributors) {}
}