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

    // pyproject.toml: quoted dep specs like "langchain>=0.1" or 'openai'
    private static final Pattern PYPROJECT_QUOTED_DEP = Pattern.compile("[\"']([A-Za-z0-9][A-Za-z0-9_.-]*)");
    // pyproject.toml: poetry-style "pkg = ..." key lines
    private static final Pattern PYPROJECT_KV_DEP = Pattern.compile("^([A-Za-z0-9][A-Za-z0-9_.-]+)\\s*=");

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

    /**
     * Parses pyproject.toml without a full TOML library.
     * Handles [project] dependencies lists, [tool.poetry.dependencies],
     * [tool.poetry.group.*.dependencies], and [dependency-groups] sections.
     */
    public Set<String> parsePyprojectToml(String content) {
        Set<String> out = new LinkedHashSet<>();
        if (content == null || content.isBlank()) return out;

        boolean inDepsSection = false;
        boolean inList = false;

        for (String rawLine : content.split("\\R")) {
            String line = rawLine.strip();

            // Detect section headers
            if (line.startsWith("[")) {
                String section = line.toLowerCase(Locale.ROOT);
                inDepsSection = section.contains("dependencies")
                        || section.contains("dependency-groups")
                        || section.contains("requires");
                inList = false;
                continue;
            }

            if (!inDepsSection) continue;
            if (line.isEmpty() || line.startsWith("#")) continue;

            // List format (project.dependencies / dependency-groups):  "package>=1.0",
            if (line.startsWith("\"") || line.startsWith("'") || line.startsWith("-")) {
                Matcher m = PYPROJECT_QUOTED_DEP.matcher(line);
                if (m.find()) {
                    String name = normalizePep503(m.group(1));
                    if (!name.isBlank()) out.add(name);
                }
                continue;
            }

            // Key = value format (poetry): package-name = "^1.0"
            Matcher kv = PYPROJECT_KV_DEP.matcher(line);
            if (kv.find()) {
                String name = normalizePep503(kv.group(1));
                // Skip TOML meta-keys like "python", "name", "version", "description"
                if (!name.equals("python") && !name.equals("name") && !name.equals("version")
                        && !name.equals("description") && !name.equals("authors")) {
                    out.add(name);
                }
            }
        }
        return out;
    }

    /** Parses setup.cfg [options] install_requires / extras_require sections. */
    public Set<String> parseSetupCfg(String content) {
        Set<String> out = new LinkedHashSet<>();
        if (content == null || content.isBlank()) return out;

        boolean inRequires = false;
        for (String rawLine : content.split("\\R")) {
            String line = rawLine.strip();
            if (line.startsWith("[")) {
                inRequires = false;
                continue;
            }
            if (line.toLowerCase(Locale.ROOT).startsWith("install_requires")
                    || line.toLowerCase(Locale.ROOT).startsWith("requires")) {
                inRequires = true;
                // Handle inline: install_requires = pkg1 pkg2
                String after = line.contains("=") ? line.substring(line.indexOf('=') + 1).strip() : "";
                for (String part : after.split("[,\\s]+")) {
                    String name = extractReqName(part);
                    if (!name.isBlank()) out.add(normalizePep503(name));
                }
                continue;
            }
            if (inRequires) {
                if (line.isEmpty() || line.startsWith("[") || (!line.startsWith(" ") && !line.startsWith("\t") && !Character.isLetterOrDigit(line.charAt(0)))) {
                    inRequires = false;
                    continue;
                }
                String name = extractReqName(line);
                if (!name.isBlank()) out.add(normalizePep503(name));
            }
        }
        return out;
    }

    /** Parses Pipfile [packages] and [dev-packages] sections. */
    public Set<String> parsePipfile(String content) {
        Set<String> out = new LinkedHashSet<>();
        if (content == null || content.isBlank()) return out;

        boolean inPackages = false;
        for (String rawLine : content.split("\\R")) {
            String line = rawLine.strip();
            if (line.startsWith("[")) {
                String lower = line.toLowerCase(Locale.ROOT);
                inPackages = lower.contains("packages");
                continue;
            }
            if (!inPackages || line.isEmpty() || line.startsWith("#")) continue;
            Matcher kv = PYPROJECT_KV_DEP.matcher(line);
            if (kv.find()) {
                String name = normalizePep503(kv.group(1));
                if (!name.equals("python_version") && !name.equals("python_full_version")) {
                    out.add(name);
                }
            }
        }
        return out;
    }

    /** PEP 503: normalize underscores, dots, and hyphens to hyphen, lowercase. */
    static String normalizePep503(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[_.]", "-");
    }

    private static String extractReqName(String line) {
        line = line.strip();
        if (line.isEmpty() || line.startsWith("#") || line.startsWith("-")
                || line.startsWith("git+") || line.contains("://")) return "";
        int markerIdx = line.indexOf(';');
        if (markerIdx >= 0) line = line.substring(0, markerIdx).strip();
        int extrasIdx = line.indexOf('[');
        if (extrasIdx >= 0) line = line.substring(0, extrasIdx).strip();
        Matcher ver = VERSION_SPLIT.matcher(line);
        if (ver.find()) line = line.substring(0, ver.start()).strip();
        Matcher m = REQ_NAME_PREFIX.matcher(line);
        return m.matches() ? m.group(1) : "";
    }
}
