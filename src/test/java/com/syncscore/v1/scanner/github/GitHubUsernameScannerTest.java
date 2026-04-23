package com.syncscore.v1.scanner.github;

import com.syncscore.v1.domain.ManifestType;
import com.syncscore.v1.scanner.GitHubScanProperties;
import com.syncscore.v1.scanner.GitHubScanResult;
import com.syncscore.v1.scanner.GitHubUsernameScanner;
import com.syncscore.v1.scanner.ManifestDependencyExtractor;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class GitHubUsernameScannerTest {

    @Test
    void scanUsername_discoversManifestsAndAggregatesPackages() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        GitHubScanProperties props = new GitHubScanProperties(
                "http://github.test",
                "token123",
                10,
                15,
                100
        );

        GitHubApiClient client = new GitHubApiClient(restTemplate, props);
        GitHubUsernameScanner scanner = new GitHubUsernameScanner(client, props);

        String reposUri = UriComponentsBuilder.fromHttpUrl(props.apiBaseUrl())
                .pathSegment("users", "testuser", "repos")
                .queryParam("per_page", 100)
                .queryParam("sort", "pushed")
                .queryParam("direction", "desc")
                .queryParam("type", "public")
                .toUriString();

        server.expect(requestTo(reposUri))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token123"))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": 1,
                            "name": "repo1",
                            "full_name": "testuser/repo1",
                            "owner": {"login": "testuser"},
                            "html_url": "https://github.com/testuser/repo1",
                            "default_branch": "main",
                            "pushed_at": "2026-04-10T00:00:00Z"
                          },
                          {
                            "id": 2,
                            "name": "repo2",
                            "full_name": "testuser/repo2",
                            "owner": {"login": "testuser"},
                            "html_url": "https://github.com/testuser/repo2",
                            "default_branch": "main",
                            "pushed_at": "2026-04-09T00:00:00Z"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        String treeRepo1 = UriComponentsBuilder.fromHttpUrl(props.apiBaseUrl())
                .pathSegment("repos", "testuser", "repo1", "git", "trees", "main")
                .queryParam("recursive", "1")
                .toUriString();

        server.expect(requestTo(treeRepo1))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token123"))
                .andRespond(withSuccess("""
                        {
                          "truncated": false,
                          "tree": [
                            {"path": "package.json", "type": "blob"},
                            {"path": "backend/requirements.txt", "type": "blob"},
                            {"path": "src/index.js", "type": "blob"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        String pkgJson = """
                {
                  "dependencies": {"openai": "^4.0.0"},
                  "devDependencies": {"langfuse": "^3.0.0"}
                }
                """;
        String reqTxt = "chromadb==0.4.0\n# comment\n";

        String pkgJsonB64 = Base64.getEncoder().encodeToString(pkgJson.getBytes(StandardCharsets.UTF_8));
        String reqTxtB64 = Base64.getEncoder().encodeToString(reqTxt.getBytes(StandardCharsets.UTF_8));

        String pkgUri = UriComponentsBuilder.fromHttpUrl(props.apiBaseUrl())
                .pathSegment("repos", "testuser", "repo1", "contents")
                .path("/package.json")
                .queryParam("ref", "main")
                .toUriString();

        server.expect(requestTo(pkgUri))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token123"))
                .andRespond(withSuccess("""
                        {"content": "%s", "encoding": "base64"}
                        """.formatted(pkgJsonB64), MediaType.APPLICATION_JSON));

        String reqUri = UriComponentsBuilder.fromHttpUrl(props.apiBaseUrl())
                .pathSegment("repos", "testuser", "repo1", "contents")
                .path("/backend/requirements.txt")
                .queryParam("ref", "main")
                .toUriString();

        server.expect(requestTo(reqUri))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token123"))
                .andRespond(withSuccess("""
                        {"content": "%s", "encoding": "base64"}
                        """.formatted(reqTxtB64), MediaType.APPLICATION_JSON));

        String treeRepo2 = UriComponentsBuilder.fromHttpUrl(props.apiBaseUrl())
                .pathSegment("repos", "testuser", "repo2", "git", "trees", "main")
                .queryParam("recursive", "1")
                .toUriString();

        server.expect(requestTo(treeRepo2))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token123"))
                .andRespond(withSuccess("""
                        {
                          "truncated": false,
                          "tree": [
                            {"path": "README.md", "type": "blob"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        ManifestDependencyExtractor extractor = (type, content) -> {
            // Stub parser: prove orchestration + aggregation without owning parsing logic here.
            if (type == ManifestType.PACKAGE_JSON) return Set.of("openai", "langfuse");
            if (type == ManifestType.REQUIREMENTS_TXT) return Set.of("chromadb");
            return Set.of();
        };

        GitHubScanResult result = scanner.scanUsername("testuser", 10, extractor);

        server.verify();
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.repoLimitApplied()).isEqualTo(10);
        assertThat(result.scannedRepos()).hasSize(2);
        assertThat(result.scannedRepos().getFirst().manifestsFound()).hasSize(2);
        assertThat(result.packagesFound()).containsExactlyInAnyOrder("openai", "langfuse", "chromadb");
    }

    @Test
    void scanUsername_clampsRepoLimitToMax() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        GitHubScanProperties props = new GitHubScanProperties(
                "http://github.test",
                null,
                10,
                15,
                100
        );

        GitHubApiClient client = new GitHubApiClient(restTemplate, props);
        GitHubUsernameScanner scanner = new GitHubUsernameScanner(client, props);

        String reposUri = UriComponentsBuilder.fromHttpUrl(props.apiBaseUrl())
                .pathSegment("users", "testuser", "repos")
                .queryParam("per_page", 100)
                .queryParam("sort", "pushed")
                .queryParam("direction", "desc")
                .queryParam("type", "public")
                .toUriString();

        server.expect(requestTo(reposUri))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        ManifestDependencyExtractor extractor = (type, content) -> Set.of();
        GitHubScanResult result = scanner.scanUsername("testuser", 999, extractor);

        server.verify();
        assertThat(result.repoLimitApplied()).isEqualTo(15);
        assertThat(result.scannedRepos()).isEmpty();
    }
}
