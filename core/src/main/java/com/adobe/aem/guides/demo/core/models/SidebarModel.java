package com.adobe.aem.guides.demo.core.models;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
    adaptables = Resource.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class SidebarModel {

    @Self
    private Resource resource;

    @ValueMapValue
    private String logoPath;

    @ValueMapValue
    private String logoMobileImage;

    @ValueMapValue
    private String logoLink;

    @ValueMapValue
    private Boolean enableSwitch;

    @ValueMapValue
    private String country;

    private List<HeaderLink> headerLinks = new ArrayList<>();
    private List<SidebarNavigation> sidebarNavigation = new ArrayList<>();

    @PostConstruct
    protected void init() {
        headerLinks = adaptChildren(resource, "headerLinks", HeaderLink.class);
        sidebarNavigation = adaptChildren(resource, "sidebarNavigation", SidebarNavigation.class);
    }

    public String getLogoPath() {
        return logoPath;
    }

    public String getLogoMobileImage() {
        return logoMobileImage;
    }

    public String getLogoLink() {
        return logoLink;
    }

    public Boolean getEnableSwitch() {
        return enableSwitch == null ? Boolean.TRUE : enableSwitch;
    }

    public List<HeaderLink> getHeaderLinks() {
        return headerLinks;
    }

    public List<SidebarNavigation> getSidebarNavigation() {
        return sidebarNavigation;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryLabel() {
        if (country == null || country.trim().isEmpty()) {
            return "";
        }
        if ("us".equalsIgnoreCase(country)) {
            return "US";
        }
        if ("india".equalsIgnoreCase(country)) {
            return "India";
        }
        String value = country.trim();
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }

    private static <T> List<T> adaptChildren(Resource parent, String childName, Class<T> type) {
        List<T> list = new ArrayList<>();
        if (parent == null) {
            return list;
        }
        Resource child = parent.getChild(childName);
        if (child == null) {
            return list;
        }
        for (Resource item : child.getChildren()) {
            T adapted = item.adaptTo(type);
            if (adapted != null) {
                list.add(adapted);
            }
        }
        return list;
    }

    @Model(
        adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
    )
    public static class HeaderLink {

        @ValueMapValue
        private String name;

        @ValueMapValue
        private String image;

        public String getName() {
            return name;
        }

        public String getImage() {
            return image;
        }
    }

    @Model(
        adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
    )
    public static class SidebarNavigation {

        @Self
        private Resource resource;

        @ValueMapValue
        private String desktopIcon;

        @ValueMapValue
        private String mobileIcon;

        private List<NavigationLink> navigationLinks = new ArrayList<>();

        @PostConstruct
        protected void init() {
            navigationLinks = adaptChildren(resource, "navigationLinks", NavigationLink.class);
        }

        public String getDesktopIcon() {
            return desktopIcon;
        }

        public String getMobileIcon() {
            return mobileIcon;
        }

        public List<NavigationLink> getNavigationLinks() {
            return navigationLinks;
        }
    }

    @Model(
        adaptables = Resource.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
    )
    public static class NavigationLink {

        @ValueMapValue
        private String title;

        @ValueMapValue
        private String navigationURL;

        public String getNavigationURL() {
            return navigationURL;
        }

        public String getTitle() {
            if (title != null && !title.trim().isEmpty() && !looksLikePath(title)) {
                return title.trim();
            }
            return getLabel();
        }

        public String getLabel() {
            return humanizePath(navigationURL);
        }

        private boolean looksLikePath(String value) {
            String trimmed = value.trim();
            return trimmed.startsWith("/content")
                || trimmed.startsWith("/content/dam")
                || trimmed.startsWith("http://")
                || trimmed.startsWith("https://");
        }

        private String humanizePath(String path) {
            if (path == null || path.trim().isEmpty()) {
                return "";
            }
            String value = path.trim();
            if (value.endsWith(".html")) {
                value = value.substring(0, value.length() - 5);
            }
            while (value.endsWith("/") && value.length() > 1) {
                value = value.substring(0, value.length() - 1);
            }
            int hash = value.lastIndexOf('#');
            if (hash >= 0 && hash < value.length() - 1) {
                return humanize(value.substring(hash + 1));
            }
            int slash = value.lastIndexOf('/');
            if (slash >= 0 && slash < value.length() - 1) {
                return humanize(value.substring(slash + 1));
            }
            return humanize(value);
        }

        private String humanize(String slug) {
            String[] parts = slug.replace(".html", "").split("[-_]");
            StringBuilder builder = new StringBuilder();
            for (String part : parts) {
                if (part.isEmpty()) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    builder.append(part.substring(1).toLowerCase());
                }
            }
            return builder.toString();
        }
    }
}
