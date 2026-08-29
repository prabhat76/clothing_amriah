package com.clothing.ai.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern DASHES = Pattern.compile("-+");

    private SlugUtil() {}

    public static String slug(String input) {
        if (input == null) return "";
        String s = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll(WHITESPACE.pattern(), "-")
                .replaceAll(NONLATIN.pattern(), "")
                .replaceAll(DASHES.pattern(), "-")
                .replaceAll("^-|-$", "");
        return s;
    }
}
