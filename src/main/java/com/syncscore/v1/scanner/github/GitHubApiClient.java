package com.syncscore.v1.scanner.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.syncscore.v1.scanner.GitHubScanProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GitHubApiClient {

    private final RestTemplate restTemplate;
    private final GitHubScanProperties props;

    public GitHubApiClient(RestTemplate githubRestTemplate, GitHubScanProperties props) {
        this.restTemplate = githubRestTemplate;
        this.props = props;
    }

    public List<RepoSummary> listPublicReposByPushedDesc(String username) {
        String uri = UriComponentsBuilder.fromHttpUrl(props.apiBaseUrl())
                .pathSegment("users", username, "repos")
                .queryParam("per_page", props.listReposPerPage())
                .queryParam("sort", "pushed")
                .queryParam("direction", "desc")
                .queryParam("type", "public")
                .toUriString();

        ResponseEntity<RepoSummary[]> resp = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(defaultHeaders()),
                RepoSummary[].class
        );

        RepoSummary[] body = resp.getBody();
        if (body == null) return List.of();

        // GitHub should already return sorted, but we re-sort defensively.
        return List.of(body).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RepoSummary::pushedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public TreeResponse fetchRepoTreeRecursive(String owner, String repo, String ref) {
        String uri = UriComponentsBuilder.fromHttpUrl(props.apiBaseUrl())
                .pathSegment("repos", owner, repo, "git", "trees", ref)
                .queryParam("recursive", "1")
                .toUriString();

        ResponseEntity<TreeResponse> resp = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(defaultHeaders()),
                TreeResponse.class
        );
        return resp.getBody() == null ? new TreeResponse(false, List.of()) : resp.getBody();
    }

    public String fetchFileContent(String owner, String repo, String path, String ref) {
        String uri = UriComponentsBuilder.fromHttpUrl(props.apiBaseUrl())
                .pathSegment("repos", owner, repo, "contents")
                .path("/" + path) // path may include slashes; keep as-is
                .queryParam("ref", ref)
                .toUriString();

        ResponseEntity<ContentResponse> resp = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(defaultHeaders()),
                ContentResponse.class
        );
        ContentResponse body = resp.getBody();
        if (body == null || !StringUtils.hasText(body.content)) {
            return null;
        }

        // GitHub content has newlines; decode base64.
        String sanitized = body.content.replace("\n", "");
        byte[] decoded = Base64.getDecoder().decode(sanitized);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    public int listCommitCount(String owner, String repo, Instant since) {
        String uri = UriComponentsBuilder.fromHttpUrl(props.apiBaseUrl())
                .pathSegment("repos", owner, repo, "commits")
                .queryParam("since", since.toString())
                .queryParam("per_page", 100)
                .toUriString();

        try {
            ResponseEntity<CommitSummary[]> resp = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    CommitSummary[].class
            );
            CommitSummary[] body = resp.getBody();
            return body == null ? 0 : body.length;
        } catch (Exception e) {
            return 0;
        }
    }

    public int listContributorCount(String owner, String repo) {
        String uri = UriComponentsBuilder.fromHttpUrl(props.apiBaseUrl())
                .pathSegment("repos", owner, repo, "contributors")
                .queryParam("per_page", 100)
                .queryParam("anon", "false")
                .toUriString();

        try {
            ResponseEntity<ContributorSummary[]> resp = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(defaultHeaders()),
                    ContributorSummary[].class
            );
            ContributorSummary[] body = resp.getBody();
            return body == null ? 0 : body.length;
        } catch (Exception e) {
            return 0;
        }
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setAccept(List.of(MediaType.valueOf("application/vnd.github+json")));
        h.add(HttpHeaders.USER_AGENT, "sync-score");
        if (StringUtils.hasText(props.token())) {
            h.add(HttpHeaders.AUTHORIZATION, "Bearer " + props.token());
        }
        return h;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RepoSummary(
            long id,
            String name,
            @JsonProperty("full_name") String fullName,
            Owner owner,
            @JsonProperty("html_url") String htmlUrl,
            @JsonProperty("default_branch") String defaultBranch,
            @JsonProperty("pushed_at") Instant pushedAt,
            @JsonProperty("created_at") Instant createdAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(String login) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TreeResponse(
            boolean truncated,
            List<TreeItem> tree
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TreeItem(
            String path,
            String type
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentResponse {
        @JsonProperty("content")
        public String content;
        @JsonProperty("encoding")
        public String encoding;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommitSummary(String sha) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContributorSummary(String login, int contributions) {}
}

