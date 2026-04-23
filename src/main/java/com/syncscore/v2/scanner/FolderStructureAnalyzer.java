package com.syncscore.v2.scanner;

import com.syncscore.v1.scanner.github.GitHubApiClient.TreeItem;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class FolderStructureAnalyzer {

    private static final Set<String> SOURCE_EXTENSIONS = Set.of(
        ".py", ".ts", ".js", ".tsx", ".jsx", ".go", ".java", ".rb", ".rs"
    );

    public Result analyze(List<TreeItem> tree) {
        if (tree == null || tree.isEmpty()) {
            return new Result(0, 0, 0);
        }

        int maxDepth = 0;
        int sourceFileCount = 0;
        Set<String> serviceDirs = new HashSet<>();

        for (TreeItem item : tree) {
            if (item == null || item.path() == null || !"blob".equalsIgnoreCase(item.type())) continue;

            String path = item.path();
            String[] parts = path.split("/");
            if (parts.length > maxDepth) {
                maxDepth = parts.length;
            }

            String lower = path.toLowerCase(Locale.ROOT);
            for (String ext : SOURCE_EXTENSIONS) {
                if (lower.endsWith(ext)) {
                    sourceFileCount++;
                    break;
                }
            }

            // A "service" is any immediate subdirectory of a top-level "services/", "apps/", or "packages/" folder.
            if (parts.length >= 2) {
                String topDir = parts[0].toLowerCase(Locale.ROOT);
                if (topDir.equals("services") || topDir.equals("apps") || topDir.equals("packages")) {
                    serviceDirs.add(topDir + "/" + parts[1]);
                }
            }
        }

        return new Result(maxDepth, serviceDirs.size(), sourceFileCount);
    }

    public record Result(int maxDepth, int serviceCount, int sourceFileCount) {}
}
