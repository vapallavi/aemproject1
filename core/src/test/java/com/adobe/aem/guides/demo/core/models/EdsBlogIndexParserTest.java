package com.adobe.aem.guides.demo.core.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class EdsBlogIndexParserTest {

    private static final String SAMPLE_INDEX =
        "{"
        + "\"total\":3,"
        + "\"data\":["
        + "{"
        + "\"path\":\"/en-us/older-article\","
        + "\"title\":\"Older Article | Lumen\","
        + "\"description\":\"An older summary.\","
        + "\"tags\":\"blog-and-news:categories/topics/network-security\","
        + "\"keywords\":\"Security\","
        + "\"imagePath\":\"https://assets.lumen.com/older.jpg\","
        + "\"imageAlt\":\"Older hero\","
        + "\"blogPublishedDate\":\"Feb 6, 2023\","
        + "\"categories\":\"blog-and-news:categories/services/security-services\","
        + "\"lastModified\":1700000000"
        + "},"
        + "{"
        + "\"path\":\"/en-us/newest-article\","
        + "\"title\":\"Newest Article | Lumen\","
        + "\"description\":\"A newest summary.\","
        + "\"tags\":\"blog-and-news:categories/topics/naas\","
        + "\"keywords\":\"NaaS\","
        + "\"image\":\"https://assets.lumen.com/newest.jpg\","
        + "\"imageAlt\":\"Newest hero\","
        + "\"blogPublishedDate\":\"October 12, 2026\","
        + "\"categories\":\"blog-and-news:categories/customer-stories\","
        + "\"lastModified\":1790000000"
        + "},"
        + "{"
        + "\"path\":\"/en-us/mid-article\","
        + "\"title\":\"Mid Article\","
        + "\"description\":\"A mid summary.\","
        + "\"tags\":\"blog-and-news:categories/topics/leadership-perspectives\","
        + "\"keywords\":\"Leadership\","
        + "\"imagePath\":\"https://assets.lumen.com/mid.jpg\","
        + "\"blogPublishedDate\":\"June 23, 2026\","
        + "\"categories\":\"blog-and-news:categories/industries/technology\","
        + "\"lastModified\":1780000000"
        + "}"
        + "]"
        + "}";

    @Test
    void parsesMetadataAndAbsoluteLinks() throws Exception {
        List<EdsBlogCard> cards = EdsBlogIndexParser.parse(SAMPLE_INDEX);
        assertEquals(3, cards.size());

        EdsBlogCard newest = cards.get(1);
        assertEquals("Newest Article | Lumen", newest.getTitle());
        assertEquals("A newest summary.", newest.getSummary());
        assertEquals("https://assets.lumen.com/newest.jpg", newest.getHeroImage());
        assertEquals("Newest hero", newest.getImageAlt());
        assertEquals("/en-us/newest-article", newest.getLink());
        assertEquals("October 12, 2026", newest.getFormattedDate());
    }

    @Test
    void sortsNewestFirstAndLimits() throws Exception {
        List<EdsBlogCard> cards = EdsBlogIndexParser.parse(SAMPLE_INDEX);
        List<EdsBlogCard> limited = EdsBlogIndexParser.filterSortLimit(cards, "all", null, 2);

        assertEquals(2, limited.size());
        assertEquals("Newest Article | Lumen", limited.get(0).getTitle());
        assertEquals("Mid Article", limited.get(1).getTitle());
        assertEquals("June 23, 2026", limited.get(1).getFormattedDate());
    }

    @Test
    void filtersByContentTypeAndTag() throws Exception {
        List<EdsBlogCard> cards = EdsBlogIndexParser.parse(SAMPLE_INDEX);

        List<EdsBlogCard> customerStories = EdsBlogIndexParser.filterSortLimit(cards, "customer-stories", null, 6);
        assertEquals(1, customerStories.size());
        assertEquals("Newest Article | Lumen", customerStories.get(0).getTitle());

        List<EdsBlogCard> naas = EdsBlogIndexParser.filterSortLimit(cards, "all", "naas", 6);
        assertEquals(1, naas.size());
        assertEquals("Newest Article | Lumen", naas.get(0).getTitle());

        List<EdsBlogCard> leadership = EdsBlogIndexParser.filterSortLimit(cards, null, "leadership", 6);
        assertEquals(1, leadership.size());
        assertEquals("Mid Article", leadership.get(0).getTitle());

        List<EdsBlogCard> technology = EdsBlogIndexParser.filterSortLimit(cards, "technology", null, 6);
        assertEquals(1, technology.size());
        assertEquals("Mid Article", technology.get(0).getTitle());
    }

    @Test
    void parsesShortAndFullMonthDates() {
        assertEquals("February 6, 2023", dateLabel("Feb 6, 2023"));
        assertEquals("June 23, 2026", dateLabel("June 23, 2026"));
        assertEquals("October 12, 2026", dateLabel("October 12, 2026"));
        assertEquals("November 4, 2024", dateLabel("Nov 4, 2024"));
    }

    @Test
    void keepsLocalPathsAndStripsLumenHost() {
        assertEquals("/en-us/sample", EdsBlogIndexParser.toLocalPath("/en-us/sample"));
        assertEquals("/en-us/sample", EdsBlogIndexParser.toLocalPath("https://www.lumen.com/blog/en-us/sample"));
        assertEquals("/en-us/insights/sample.html",
                EdsBlogIndexParser.toLocalPath("https://www.lumen.com/en-us/insights/sample.html"));
    }

    private static String dateLabel(String raw) {
        EdsBlogCard card = new EdsBlogCard();
        card.setPublishedDate(EdsBlogIndexParser.parseDate(raw));
        return card.getFormattedDate();
    }
}