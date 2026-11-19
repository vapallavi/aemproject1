package com.adobe.aem.guides.demo.core.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class EdsBlogIndexParser {

    private static final String[] DATE_PATTERNS = {
            "MMMM d, yyyy",
            "MMM d, yyyy",
            "MMMM dd, yyyy",
            "MMM dd, yyyy",
            "yyyy-MM-dd"
    };

    private EdsBlogIndexParser() {
    }

    public static List<EdsBlogCard> parse(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode data = root.path("data");

        List<EdsBlogCard> result = new ArrayList<>();
        if (!data.isArray()) {
            return result;
        }

        for (JsonNode node : data) {
            EdsBlogCard card = new EdsBlogCard();

            card.setTitle(text(node, "title"));
            card.setSummary(text(node, "description"));
            card.setImageAlt(text(node, "imageAlt"));
            card.setHeroImage(firstNonBlank(text(node, "imagePath"), text(node, "image")));
            card.setLink(toLocalPath(text(node, "path")));
            card.setTags(parseValues(node.path("tags")));
            card.setCategories(parseValues(node.path("categories")));
            card.setAuthor(text(node, "blogAuthor"));
            card.setReadDuration(text(node, "blogReadDuration"));
            card.setFirstPublishedDate(parseDate(text(node, "blogPublishedDate")));

            result.add(card);
        }
        return result;
    }

    public static List<EdsBlogCard> filterSortLimit(
            List<EdsBlogCard> cards,
            String contentType,
            String tagFilter,
            int limit) {

        if (cards == null || cards.isEmpty()) {
            return List.of();
        }

        String category = normalizeFilter(contentType);
        String tag = normalizeFilter(tagFilter);
        int resolvedLimit = limit > 0 ? limit : 6;

        return cards.stream()
                .filter(card -> matches(card.getCategories(), category))
                .filter(card -> matches(card.getTags(), tag))
                .sorted(Comparator.comparing(
                        EdsBlogCard::getFirstPublishedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(resolvedLimit)
                .collect(Collectors.toList());
    }

    private static boolean matches(List<String> values, String filter) {
        if (filter == null) {
            return true;
        }
        if (values == null || values.isEmpty()) {
            return false;
        }

        String f = filter.toLowerCase(Locale.ENGLISH);
        return values.stream().anyMatch(value -> {
            if (value == null) {
                return false;
            }
            String v = value.toLowerCase(Locale.ENGLISH);
            return v.contains(f) || v.endsWith("/" + f) || v.endsWith(":" + f);
        });
    }

    private static List<String> parseValues(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || node.isNull()) {
            return values;
        }

        if (node.isTextual()) {
            for (String value : node.asText("").split(",")) {
                String cleaned = value.trim();
                if (!cleaned.isEmpty()) {
                    values.add(cleaned);
                }
            }
        }

        if (node.isArray()) {
            for (JsonNode item : node) {
                String cleaned = item.asText("").trim();
                if (!cleaned.isEmpty()) {
                    values.add(cleaned);
                }
            }
        }
        return values;
    }

    static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        String value = dateStr.trim();
        for (String pattern : DATE_PATTERNS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH);
                sdf.setLenient(false);
                return sdf.parse(value);
            } catch (ParseException ignored) {
                // try next pattern
            }
        }
        return null;
    }

    static String toLocalPath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String trimmed = path.trim();

        String[] lumenOrigins = {
                "https://www.lumen.com/blog",
                "http://www.lumen.com/blog",
                "https://www.lumen.com",
                "http://www.lumen.com"
        };

        for (String origin : lumenOrigins) {
            if (trimmed.regionMatches(true, 0, origin, 0, origin.length())) {
                trimmed = trimmed.substring(origin.length());
                break;
            }
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            int schemeEnd = trimmed.indexOf("://");
            int pathStart = trimmed.indexOf('/', schemeEnd + 3);
            trimmed = pathStart >= 0 ? trimmed.substring(pathStart) : "";
        }

        if (trimmed.isBlank()) {
            return "";
        }
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private static String normalizeFilter(String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ENGLISH);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return "";
        }
        return value.asText("").trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}