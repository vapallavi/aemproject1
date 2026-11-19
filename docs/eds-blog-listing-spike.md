# EDS Blog Listing Spike

POC for dynamically retrieving EDS Blog articles and rendering them in an AEM listing component, without copying article content into AEM.

## Recommended approach

Use the EDS `query-index.json` as the source of truth for the listing.

1. Author drops **EDS Blog Listing** on a page.
2. The Sling Model calls the EDS index (`https://www.lumen.com/blog/query-index.json?limit=1000` by default).
3. Articles are filtered by content type and optional tag/keyword.
4. Results are sorted by first published date, newest first.
5. Each card renders title (via EDS card hero), published date, summary/abstract, thumbnail, and a Read article CTA.

This is the right POC path because EDS already publishes a structured index with title, description, image, path, tags, categories, and `blogPublishedDate`. AEM does not need to duplicate those pages.

Production follow-up should move the HTTP call out of the Sling Model into a cacheable OSGi service.

## Acceptance coverage

| Criterion | How the POC meets it |
| --- | --- |
| Display EDS Blog articles in AEM | Component fetches `query-index.json` and renders cards |
| Filter by content type/tag | Dialog: Content Type select + Tag/keyword filter |
| Required metadata | Date, summary, hero thumbnail, article CTA |
| Newest first | Sort on `blogPublishedDate`, fallback to `lastModified` |
| Cludo viability | Documented below |
| URL rewrite / SEO | Documented below |
| Implementation stories | Documented below |

## Cludo viability

Cludo can sit beside this listing. It should not replace the EDS index for a simple newest-first card grid.

**Fit**

- Site search, type-ahead, and faceted findability across AEM + EDS.
- Tag/topic filters that authors and visitors share.
- Tracking of search queries that later inform which listings to promote.

**Not a fit for this listing**

- A static 6-card newest-first grid does not need a search engine.
- Extra vendor dependency, crawl delay, and ranking logic for a sorted index query.
- EDS already exposes tags, categories, dates, and summaries in JSON.

**Recommendation**

Keep EDS `query-index.json` as the listing source. Use Cludo for global search / “find an article” experiences. If Cludo is already licensed, a later story can add a search-backed listing variant (query, facets, pagination) without changing this POC’s card UI.

## URL rewrite / SEO opportunity

The listing CTA does **not** send the browser to `www.lumen.com`. Read article opens an in-AEM panel with title, date, summary, and hero already returned by `query-index.json`.

EDS article URLs still exist as `https://www.lumen.com/blog/en-us/<slug>`. The POC stores only the local path (`/en-us/<slug>`) and never uses it as an href.

Later options if a full article body is required on AEM:

1. **In-page panel (current POC).** No navigation off the AEM page.
2. **AEM article page.** Map `/en-us/<slug>` to an AEM page that renders EDS metadata or `.plain.html` through AEM, not a lumen.com redirect.
3. **Dispatcher rewrite.** Serve EDS HTML on the AEM host. Users still never land on lumen.com.
4. **Canonical + hreflang.** Needed only if both hosts ever serve the same article.
5. **Card image SEO.** Hero `alt` already uses EDS `imageAlt` (fallback to title).

**Recommendation**

Keep article opens on AEM. Do not link out to lumen.com. Add a dedicated AEM article route only if the panel is not enough for the body copy.

## Implementation stories

1. **EDS Blog Listing – production client**  
   Extract the HTTP call into an OSGi service with connect/read timeouts, circuit breaking, and a 5–15 minute cache keyed by endpoint + filters.

2. **Authoring filters**  
   Replace free-text tag filter with a datasource of EDS tags/categories. Persist selected tags as multi-value properties.

3. **Listing UI hardening**  
   Pagination or “Load more”, empty/error states in author mode, and a design-token pass against the Lumen card spec.

4. **SEO / URL decision**  
   Choose link-out vs dispatcher rewrite. Add canonical, Open Graph, and robots handling for the chosen host.

5. **Cludo (optional)**  
   If search-driven listings are required, add a Cludo-backed variant with query, facets, and analytics. Keep the EDS newest-first listing as the default.

6. **QA / contract tests**  
   Wire-mock the EDS index. Assert sort, filters, date formats, absolute URLs, and limit. Add a Playwright check for the 3-column card grid.
