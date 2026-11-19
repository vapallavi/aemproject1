package com.adobe.aem.guides.demo.core.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;

/**
 * House of Flowers mega-menu header. Every label, link, section, and
 * featured card comes from the dialog. Empty values stay empty.
 */
@Model(
        adaptables = Resource.class,
        adapters = {HeaderModel.class, ComponentExporter.class},
        resourceType = {
                HeaderModel.RESOURCE_TYPE,
                HeaderModel.RESOURCE_TYPE_ALIAS,
                HeaderModel.RESOURCE_TYPE_DEMO},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
@Exporter(
        name = ExporterConstants.SLING_MODEL_EXPORTER_NAME,
        extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class HeaderModel implements ComponentExporter {

    public static final String RESOURCE_TYPE = "houseofflowers/components/header";
    public static final String RESOURCE_TYPE_ALIAS = "houseofflower/components/header";
    public static final String RESOURCE_TYPE_DEMO = "demo/components/header";

    @Self
    private Resource resource;

    @ValueMapValue
    private String logoText;

    @ValueMapValue
    private String logoImage;

    @ValueMapValue
    private String logoAlt;

    @ValueMapValue
    private String logoLink;

    private List<NavItem> navItems = new ArrayList<>();
    private String idPrefix = "hof";

    @PostConstruct
    protected void init() {
        if (resource != null && resource.getPath() != null) {
            idPrefix = "hof-" + Integer.toHexString(resource.getPath().hashCode());
        }
        navItems = adaptChildren(resource, "navItems", NavItem.class);
    }

    public String getLogoText() {
        return trimToEmpty(logoText);
    }

    public String getLogoImage() {
        return blankToNull(logoImage);
    }

    public String getLogoAlt() {
        return firstNonBlank(logoAlt, getLogoText());
    }

    public String getLogoLink() {
        return rewriteLink(logoLink);
    }

    public boolean isHasLogo() {
        return getLogoImage() != null || !getLogoText().isEmpty();
    }

    public List<NavItem> getNavItems() {
        return Collections.unmodifiableList(navItems);
    }

    public boolean isHasNav() {
        return !navItems.isEmpty();
    }

    public boolean isConfigured() {
        return isHasLogo() || isHasNav();
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    public String getNavId() {
        return idPrefix + "-nav";
    }

    @Override
    public String getExportedType() {
        if (resource != null) {
            String type = resource.getResourceType();
            if (type != null && !type.isEmpty()) {
                return type;
            }
        }
        return RESOURCE_TYPE;
    }

    static String rewriteLink(String value) {
        if (value == null) {
            return "";
        }
        String link = value.trim();
        if (link.isEmpty()) {
            return "";
        }
        if (link.startsWith("/content")
                && !link.contains("?")
                && !link.contains("#")
                && !link.contains(".")) {
            return link + ".html";
        }
        return link;
    }

    static String firstNonBlank(String value, String fallback) {
        String trimmed = trimToEmpty(value);
        return trimmed.isEmpty() ? trimToEmpty(fallback) : trimmed;
    }

    static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    static String blankToNull(String value) {
        String trimmed = trimToEmpty(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final Pattern TRAILING_INDEX = Pattern.compile("(\\d+)$");

    static <T> List<T> adaptChildren(Resource parent, String childName, Class<T> type) {
        List<T> list = new ArrayList<>();
        if (parent == null) {
            return list;
        }
        Resource child = parent.getChild(childName);
        if (child == null) {
            return list;
        }
        List<Resource> resources = new ArrayList<>();
        child.getChildren().forEach(resources::add);
        resources.sort(Comparator.comparing(Resource::getName, HeaderModel::compareChildNames));
        for (Resource item : resources) {
            T adapted = item.adaptTo(type);
            if (adapted != null) {
                list.add(adapted);
            }
        }
        return list;
    }

    static int compareChildNames(String left, String right) {
        int byIndex = Integer.compare(itemIndex(left), itemIndex(right));
        if (byIndex != 0) {
            return byIndex;
        }
        String safeLeft = left == null ? "" : left;
        String safeRight = right == null ? "" : right;
        return String.CASE_INSENSITIVE_ORDER.compare(safeLeft, safeRight);
    }

    static int itemIndex(String name) {
        if (name == null) {
            return Integer.MAX_VALUE;
        }
        Matcher matcher = TRAILING_INDEX.matcher(name);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }

    @Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
    public static class NavItem {

        @Self
        private Resource resource;

        @ValueMapValue
        private String label;

        @ValueMapValue
        private String link;

        @ValueMapValue
        private Boolean opensMegaMenu;

        @ValueMapValue
        private String introHeadline;

        @ValueMapValue
        private String introText;

        @ValueMapValue
        private String introCtaLabel;

        @ValueMapValue
        private String introCtaLink;

        @ValueMapValue
        private String featuredHeading;

        private List<MegaColumn> megaColumns = new ArrayList<>();
        private List<FeaturedCard> featuredCards = new ArrayList<>();

        @PostConstruct
        protected void init() {
            megaColumns = adaptChildren(resource, "megaColumns", MegaColumn.class);
            featuredCards = adaptChildren(resource, "featuredCards", FeaturedCard.class);
        }

        public String getLabel() {
            return trimToEmpty(label);
        }

        public String getLink() {
            return rewriteLink(link);
        }

        public boolean isMegaMenu() {
            return Boolean.TRUE.equals(opensMegaMenu);
        }

        public String getIntroHeadline() {
            return trimToEmpty(introHeadline);
        }

        public String getIntroText() {
            return trimToEmpty(introText);
        }

        public String getIntroCtaLabel() {
            return trimToEmpty(introCtaLabel);
        }

        public String getIntroCtaLink() {
            return rewriteLink(introCtaLink);
        }

        public boolean isHasIntro() {
            return !getIntroHeadline().isEmpty()
                    || !getIntroText().isEmpty()
                    || !getIntroCtaLabel().isEmpty();
        }

        public String getFeaturedHeading() {
            return trimToEmpty(featuredHeading);
        }

        public List<MegaColumn> getMegaColumns() {
            return Collections.unmodifiableList(megaColumns);
        }

        public boolean isHasSections() {
            return !megaColumns.isEmpty();
        }

        public List<FeaturedCard> getFeaturedCards() {
            return Collections.unmodifiableList(featuredCards);
        }

        public boolean isHasFeatured() {
            return !featuredCards.isEmpty();
        }
    }

    @Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
    public static class MegaColumn {

        @Self
        private Resource resource;

        @ValueMapValue
        private String heading;

        private List<MegaLink> links = new ArrayList<>();

        @PostConstruct
        protected void init() {
            links = adaptChildren(resource, "links", MegaLink.class);
        }

        public String getHeading() {
            return trimToEmpty(heading);
        }

        public List<MegaLink> getLinks() {
            return Collections.unmodifiableList(links);
        }
    }

    @Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
    public static class MegaLink {

        @ValueMapValue
        private String label;

        @ValueMapValue
        private String link;

        public String getLabel() {
            return trimToEmpty(label);
        }

        public String getLink() {
            return rewriteLink(link);
        }
    }

    @Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
    public static class FeaturedCard {

        @ValueMapValue
        private String title;

        @ValueMapValue
        private String image;

        @ValueMapValue
        private String link;

        @ValueMapValue
        private String alt;

        @ValueMapValue
        private String label;

        public String getTitle() {
            return trimToEmpty(title);
        }

        public String getImage() {
            return blankToNull(image);
        }

        public String getLink() {
            return rewriteLink(link);
        }

        public String getAlt() {
            return firstNonBlank(alt, getTitle());
        }

        public String getLabel() {
            return trimToEmpty(label);
        }
    }
}
