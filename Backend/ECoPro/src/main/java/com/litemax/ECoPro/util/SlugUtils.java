package com.litemax.ECoPro.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class SlugUtils {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGEHYPHEN = Pattern.compile("(^-|-$)");

    public static String createSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        String noWhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = EDGEHYPHEN.matcher(slug).replaceAll("");
        return slug.toLowerCase();
    }

    public static String createUniqueSlug(String input, java.util.function.Function<String, Boolean> exists) {
        String baseSlug = createSlug(input);
        if (!exists.apply(baseSlug)) {
            return baseSlug;
        }

        int counter = 1;
        String uniqueSlug;
        do {
            uniqueSlug = baseSlug + "-" + counter++;
        } while (exists.apply(uniqueSlug));

        return uniqueSlug;
    }
}