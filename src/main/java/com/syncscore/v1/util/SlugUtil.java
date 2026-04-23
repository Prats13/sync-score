package com.syncscore.v1.util;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtil {

    private SlugUtil() {}

    public static String slugify(String input) {
        if (input == null) return "agency";
        String s = input.trim();
        if (s.isEmpty()) return "agency";

        // Normalize and strip accents; then keep only [a-z0-9-]
        s = Normalizer.normalize(s, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "");
        s = s.toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("(^-+|-+$)", "");
        s = s.replaceAll("-{2,}", "-");

        return s.isEmpty() ? "agency" : s;
    }
}

