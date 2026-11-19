package com.adobe.aem.guides.demo.core.models;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lists the authored parent page and its direct children, then applies
 * {@code pageSize} (1-based page number) and {@code pageLimit} (items per page)
 * from the request so {@code .model.json} can return a paginated result.
 *
 * <p>Example: {@code ?pageSize=1&pageLimit=3} returns items 1-3;
 * {@code ?pageSize=2&pageLimit=3} returns items 4-6.
 */
@Model(
        adaptables = {SlingHttpServletRequest.class, Resource.class},
        adapters = {ChildPagesModel.class, ComponentExporter.class},
        resourceType = ChildPagesModel.RESOURCE_TYPE,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
@Exporter(
        name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
        extensions = ExporterConstants.SLING_MODEL_EXTENSION
)
public class ChildPagesModel implements ComponentExporter {

    public static final String RESOURCE_TYPE = "demo/components/childpages";

    @ValueMapValue
    private String pagePath;

    @SlingObject
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @SlingObject
    private ResourceResolver resourceResolver;

    private final List<ChildPageItem> pages = new ArrayList<>();
    private boolean parentPageFound;
    private Integer pageSize;
    private Integer pageLimit;

    @PostConstruct
    protected void init() {
        ResourceResolver resolver = resolveResourceResolver();
        if (resolver == null) {
            return;
        }

        if (isBlank(pagePath) && resource != null) {
            pagePath = resource.getValueMap().get("pagePath", String.class);
        }
        if (isBlank(pagePath)) {
            return;
        }
        pagePath = pagePath.trim();

        PageManager pageManager = resolver.adaptTo(PageManager.class);
        if (pageManager == null) {
            return;
        }

        Page parentPage = pageManager.getPage(pagePath);
        if (parentPage == null) {
            return;
        }
        parentPageFound = true;

        List<ChildPageItem> allPages = new ArrayList<>();
        allPages.add(toItem(parentPage));
        Iterator<Page> children = parentPage.listChildren();
        while (children.hasNext()) {
            allPages.add(toItem(children.next()));
        }

        pageSize = parsePositiveInt(requestParameter("pageSize"));
        pageLimit = parsePositiveInt(requestParameter("pageLimit"));
        pages.addAll(slice(allPages, pageSize, pageLimit));
    }

    public boolean isParentPageConfigured() {
        return !isBlank(pagePath);
    }

    public boolean isParentPageFound() {
        return parentPageFound;
    }

    public String getPagePath() {
        return pagePath;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getPageLimit() {
        return pageLimit;
    }

    public List<ChildPageItem> getPages() {
        return Collections.unmodifiableList(pages);
    }

    public List<String> getPagePaths() {
        return pages.stream().map(ChildPageItem::getPath).collect(Collectors.toList());
    }

    @Override
    public String getExportedType() {
        return RESOURCE_TYPE;
    }

    private ResourceResolver resolveResourceResolver() {
        if (resourceResolver != null) {
            return resourceResolver;
        }
        if (request != null) {
            return request.getResourceResolver();
        }
        if (resource != null) {
            return resource.getResourceResolver();
        }
        return null;
    }

    private String requestParameter(String name) {
        return request == null ? null : request.getParameter(name);
    }

    private static List<ChildPageItem> slice(List<ChildPageItem> allPages, Integer pageNumber, Integer limit) {
        if (pageNumber == null || limit == null) {
            return allPages;
        }
        int fromIndex = (pageNumber - 1) * limit;
        if (fromIndex < 0 || fromIndex >= allPages.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(fromIndex + limit, allPages.size());
        return allPages.subList(fromIndex, toIndex);
    }

    private static ChildPageItem toItem(Page page) {
        String title = page.getNavigationTitle();
        if (isBlank(title)) {
            title = page.getTitle();
        }
        if (isBlank(title)) {
            title = page.getName();
        }
        return new ChildPageItem(title, page.getPath(), page.getPath() + ".html");
    }

    private static Integer parsePositiveInt(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static class ChildPageItem {
        private final String title;
        private final String path;
        private final String url;

        public ChildPageItem(String title, String path, String url) {
            this.title = title;
            this.path = path;
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public String getPath() {
            return path;
        }

        public String getUrl() {
            return url;
        }
    }
}
