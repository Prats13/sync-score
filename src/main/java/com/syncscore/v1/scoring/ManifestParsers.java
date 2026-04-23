package com.syncscore.v1.scoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ManifestParsers {
    private static final Pattern REQ_NAME_AT_URL = Pattern.compile("^\\s*([A-Za-z0-9_.-]+)\\s*@\\s*.+$");
    private static final Pattern REQ_NAME_PREFIX = Pattern.compile("^\\s*([A-Za-z0-9_.-]+).*$");
    private static final Pattern VERSION_SPLIT = Pattern.compile("(===|==|>=|<=|~=|!=|>|<)");

    private final ObjectMapper objectMapper;

    public ManifestParsers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Set<String> parseRequirementsTxt(String content) {
        Set<String> out = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return out;
        }

        for (String rawLine : content.split("\\R")) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // pip options / includes / editable installs, etc. (out of V1 scope)
            if (line.startsWith("-")) {
                continue;
            }
            // Drop environment markers.
            int markerIdx = line.indexOf(';');
            if (markerIdx >= 0) {
                line = line.substring(0, markerIdx).strip();
            }
            if (line.isEmpty()) {
                continue;
            }

            String name;
            Matcher atUrl = REQ_NAME_AT_URL.matcher(line);
            if (atUrl.matches()) {
                name = atUrl.group(1);
            } else {
                // URL / VCS requirements without an explicit name are out of scope for V1 parsing.
                if (line.startsWith("git+") || line.contains("://")) {
                    continue;
                }
                // Strip extras: pkg[extra1,extra2]
                int extrasIdx = line.indexOf('[');
                if (extrasIdx >= 0) {
                    line = line.substring(0, extrasIdx).strip();
                }
                // Strip version constraints.
                Matcher ver = VERSION_SPLIT.matcher(line);
                if (ver.find()) {
                    line = line.substring(0, ver.start()).strip();
                }
                Matcher prefix = REQ_NAME_PREFIX.matcher(line);
                if (!prefix.matches()) {
                    continue;
                }
                name = prefix.group(1);
            }

            if (name != null && !name.isBlank()) {
                out.add(name.toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    public Set<String> parsePackageJson(String content) {
        Set<String> out = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return out;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(content);
        } catch (Exception e) {
            return out;
        }

        addKeys(root.get("dependencies"), out);
        addKeys(root.get("devDependencies"), out);
        return out;
    }

    private static void addKeys(JsonNode node, Set<String> out) {
        if (node == null || !node.isObject()) {
            return;
        }
        node.fieldNames().forEachRemaining(k -> out.add(k.toLowerCase(Locale.ROOT)));
    }
}
