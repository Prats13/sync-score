package com.syncscore.v1.scanner;

import com.syncscore.v1.domain.ManifestType;
import java.util.Set;

/**
 * Scanner dependency. V1 scanner fetches manifest contents; actual parsing/normalization lives
 * in the scoring/parsing module and should be wired in later.
 */
public interface ManifestDependencyExtractor {
    Set<String> extractPackages(ManifestType type, String manifestContent);
}
