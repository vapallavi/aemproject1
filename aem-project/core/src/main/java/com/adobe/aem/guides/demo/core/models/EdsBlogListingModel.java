package com.adobe.aem.guides.demo.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class EdsBlogListingModel {

    private static final Logger LOG = LoggerFactory.getLogger(EdsBlogListingModel.class);
    static final String DEFAULT_ENDPOINT = "https://www.lumen.com/blog/query-index.json?limit=1000";
    private static final int DEFAULT_LIMIT = 6;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    private static volatile List<EdsBlogCard> cachedCards;
    private static volatile long cacheTimestamp;

    @ValueMapValue
    private Long numberOfCards;

    @ValueMapValue
    private String contentType;

    @ValueMapValue
    private String tagFilter;

    @ValueMapValue
    private String edsEndpoint;

    private List<EdsBlogCard> cards = Collections.emptyList();

    @PostConstruct
    protected void init() {
        cards = getCachedOrFetch();
    }
    
    static String withLimit(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return DEFAULT_ENDPOINT;
        }
        if (endpoint.contains("limit=")) {
            return endpoint;
        }
        return endpoint.contains("?") ? endpoint + "&limit=1000" : endpoint + "?limit=1000";
    }

    public List<EdsBlogCard> getCards() {
        return cards;
    }

    public String getItemCountLabel() {
        int size = cards.size();
        return size == 1 ? "1 item" : size + " items";
    }

    private List<EdsBlogCard> getCachedOrFetch() {
        long now = System.currentTimeMillis();

        if (cachedCards != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return EdsBlogIndexParser.filterSortLimit(cachedCards, contentType, tagFilter, resolveLimit());
        }

        try {
            String json = callEdsEndpoint(resolveEndpoint());
            List<EdsBlogCard> allCards = EdsBlogIndexParser.parse(json);
            cachedCards = allCards;
            cacheTimestamp = now;
            return EdsBlogIndexParser.filterSortLimit(allCards, contentType, tagFilter, resolveLimit());
        } catch (Exception e) {
            LOG.error("Failed to fetch EDS blog listing", e);
            if (cachedCards != null) {
                return EdsBlogIndexParser.filterSortLimit(cachedCards, contentType, tagFilter, resolveLimit());
            }
            return Collections.emptyList();
        }
    }

    private int resolveLimit() {
        return (numberOfCards != null && numberOfCards > 0) ? numberOfCards.intValue() : DEFAULT_LIMIT;
    }

    private String resolveEndpoint() {
        String endpoint = (edsEndpoint == null || edsEndpoint.isBlank())
                ? DEFAULT_ENDPOINT
                : edsEndpoint.trim();
        return withLimit(endpoint);
    }

    private String callEdsEndpoint(String endpoint) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("EDS endpoint returned HTTP " + response.statusCode());
        }
        return response.body();
    }
}