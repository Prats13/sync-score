package com.syncscore.v2.scanner;

import com.syncscore.v1.scanner.github.GitHubApiClient;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommitActivityAnalyzerTest {

    @Mock
    private GitHubApiClient gitHubApi;

    @Test
    void aggregatesCommitCountAndContributorsAcrossRepos() {
        CommitActivityAnalyzer analyzer = new CommitActivityAnalyzer(gitHubApi);

        when(gitHubApi.listCommitCount(eq("org"), eq("repo1"), any(Instant.class))).thenReturn(25);
        when(gitHubApi.listCommitCount(eq("org"), eq("repo2"), any(Instant.class))).thenReturn(10);
        when(gitHubApi.listContributorCount("org", "repo1")).thenReturn(4);
        when(gitHubApi.listContributorCount("org", "repo2")).thenReturn(2);

        CommitActivityAnalyzer.Result result = analyzer.analyze("org", List.of("repo1", "repo2"));

        assertThat(result.totalCommits90d()).isEqualTo(35);
        assertThat(result.maxContributors()).isEqualTo(4);
    }

    @Test
    void returnsZeroOnEmptyRepoList() {
        CommitActivityAnalyzer analyzer = new CommitActivityAnalyzer(gitHubApi);

        CommitActivityAnalyzer.Result result = analyzer.analyze("org", List.of());

        assertThat(result.totalCommits90d()).isEqualTo(0);
        assertThat(result.maxContributors()).isEqualTo(0);
    }
}