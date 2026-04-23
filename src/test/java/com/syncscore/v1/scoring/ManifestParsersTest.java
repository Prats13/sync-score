package com.syncscore.v1.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ManifestParsersTest {

    private final ManifestParsers parsers = new ManifestParsers(new ObjectMapper());

    @Test
    void parseRequirementsTxt_stripsVersionsExtrasAndMarkers() {
        String req = """
                # comment
                langgraph[all]~=0.1.0
                requests>=2.0; python_version >= '3.8'
                openai @ git+https://github.com/openai/openai-python.git
                -r other.txt
                git+https://example.com/something.git
                """;

        Set<String> pkgs = parsers.parseRequirementsTxt(req);
        assertThat(pkgs).contains("langgraph", "requests", "openai");
        assertThat(pkgs).doesNotContain("git");
    }

    @Test
    void parsePackageJson_readsDependenciesAndDevDependencies() {
        String pkg = """
                {
                  "dependencies": { "OpenAI": "^4.0.0", "@Scope/Thing": "1.0.0" },
                  "devDependencies": { "Langfuse": "2.0.0" }
                }
                """;
        Set<String> pkgs = parsers.parsePackageJson(pkg);
        assertThat(pkgs).contains("openai", "@scope/thing", "langfuse");
    }
}

