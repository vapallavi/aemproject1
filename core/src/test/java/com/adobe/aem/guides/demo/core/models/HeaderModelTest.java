package com.adobe.aem.guides.demo.core.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.adobe.aem.guides.demo.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class HeaderModelTest {

    private static final String PATH = "/content/header";

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(
                HeaderModel.class,
                HeaderModel.NavItem.class,
                HeaderModel.MegaColumn.class,
                HeaderModel.MegaLink.class,
                HeaderModel.FeaturedCard.class);
    }

    @Test
    void emptyResourceHasNoAuthoredContent() {
        context.create().resource(PATH, "sling:resourceType", HeaderModel.RESOURCE_TYPE);
        HeaderModel model = adapt();

        assertNotNull(model);
        assertEquals("", model.getLogoText());
        assertNull(model.getLogoImage());
        assertEquals("", model.getLogoAlt());
        assertEquals("", model.getLogoLink());
        assertFalse(model.isHasLogo());
        assertFalse(model.isHasNav());
        assertFalse(model.isConfigured());
        assertTrue(model.getNavItems().isEmpty());
        assertEquals(HeaderModel.RESOURCE_TYPE, model.getExportedType());
        assertTrue(model.getIdPrefix().startsWith("hof-"));
        assertTrue(model.getNavId().endsWith("-nav"));
    }

    @Test
    void authoredNavItemOwnsMegaMenu() {
        context.build().resource(PATH,
                "sling:resourceType", HeaderModel.RESOURCE_TYPE,
                "logoText", "  Maison des Fleurs  ",
                "logoImage", "/content/dam/hof/logo.png",
                "logoAlt", "Maison mark",
                "logoLink", "/content/demo/us/en/house-of-flowers");

        context.build().resource(PATH + "/navItems/item0",
                "label", "Boutique",
                "link", "/content/demo/us/en/shop",
                "opensMegaMenu", false);
        context.build().resource(PATH + "/navItems/item1",
                "label", "Maison",
                "link", "/content/demo/us/en/about",
                "opensMegaMenu", true,
                "introHeadline", "The Maison",
                "introText", "Custom intro",
                "introCtaLabel", "Read our story",
                "introCtaLink", "/content/demo/us/en/about",
                "featuredHeading", "In Bloom");
        context.build().resource(PATH + "/navItems/item1/megaColumns/col0", "heading", "Maison");
        context.build().resource(PATH + "/navItems/item1/megaColumns/col0/links/link0",
                "label", "Atelier",
                "link", "/content/demo/us/en/atelier");
        context.build().resource(PATH + "/navItems/item1/featuredCards/card0",
                "title", "Spring Garden",
                "image", "/content/dam/hof/spring.jpg",
                "link", "/content/demo/us/en/spring",
                "alt", "Spring bouquet",
                "label", "Collection");

        HeaderModel model = adapt();

        assertEquals("Maison des Fleurs", model.getLogoText());
        assertEquals("/content/dam/hof/logo.png", model.getLogoImage());
        assertEquals("Maison mark", model.getLogoAlt());
        assertEquals("/content/demo/us/en/house-of-flowers.html", model.getLogoLink());
        assertTrue(model.isHasLogo());
        assertTrue(model.isConfigured());

        assertEquals(2, model.getNavItems().size());
        HeaderModel.NavItem boutique = model.getNavItems().get(0);
        assertEquals("Boutique", boutique.getLabel());
        assertEquals("/content/demo/us/en/shop.html", boutique.getLink());
        assertFalse(boutique.isMegaMenu());
        assertFalse(boutique.isHasIntro());
        assertFalse(boutique.isHasSections());
        assertFalse(boutique.isHasFeatured());

        HeaderModel.NavItem maison = model.getNavItems().get(1);
        assertTrue(maison.isMegaMenu());
        assertEquals("The Maison", maison.getIntroHeadline());
        assertEquals("Custom intro", maison.getIntroText());
        assertEquals("Read our story", maison.getIntroCtaLabel());
        assertEquals("/content/demo/us/en/about.html", maison.getIntroCtaLink());
        assertTrue(maison.isHasIntro());
        assertEquals("In Bloom", maison.getFeaturedHeading());
        assertEquals(1, maison.getMegaColumns().size());
        assertEquals("Maison", maison.getMegaColumns().get(0).getHeading());
        assertEquals("/content/demo/us/en/atelier.html", maison.getMegaColumns().get(0).getLinks().get(0).getLink());
        assertEquals(1, maison.getFeaturedCards().size());
        assertEquals("Spring Garden", maison.getFeaturedCards().get(0).getTitle());
        assertEquals("/content/dam/hof/spring.jpg", maison.getFeaturedCards().get(0).getImage());
        assertEquals("/content/demo/us/en/spring.html", maison.getFeaturedCards().get(0).getLink());
        assertEquals("Spring bouquet", maison.getFeaturedCards().get(0).getAlt());
        assertEquals("Collection", maison.getFeaturedCards().get(0).getLabel());
    }

    @Test
    void aliasResourceTypeAdapts() {
        context.create().resource(PATH, "sling:resourceType", HeaderModel.RESOURCE_TYPE_ALIAS);
        HeaderModel model = adapt();
        assertNotNull(model);
        assertEquals("", model.getLogoText());
        assertEquals(HeaderModel.RESOURCE_TYPE_ALIAS, model.getExportedType());
    }

    @Test
    void navItemsFollowItemIndexWhenJcrOrderDiffers() {
        context.build().resource(PATH, "sling:resourceType", HeaderModel.RESOURCE_TYPE, "logoText", "Hof");
        context.build().resource(PATH + "/navItems/item2", "label", "About", "opensMegaMenu", true);
        context.build().resource(PATH + "/navItems/item0", "label", "Shop", "opensMegaMenu", false);
        context.build().resource(PATH + "/navItems/item1", "label", "Collections", "opensMegaMenu", false);

        HeaderModel model = adapt();
        assertEquals(3, model.getNavItems().size());
        assertEquals("Shop", model.getNavItems().get(0).getLabel());
        assertEquals("Collections", model.getNavItems().get(1).getLabel());
        assertEquals("About", model.getNavItems().get(2).getLabel());
        assertTrue(model.getNavItems().get(2).isMegaMenu());
    }

    @Test
    void rewriteLinkLeavesAnchorsAndUrls() {
        assertEquals("#shop", HeaderModel.rewriteLink("#shop"));
        assertEquals("https://houseofflowers.example/", HeaderModel.rewriteLink("https://houseofflowers.example/"));
        assertEquals("/content/demo/us/en.html", HeaderModel.rewriteLink("/content/demo/us/en"));
        assertEquals("/content/demo/us/en.html", HeaderModel.rewriteLink("/content/demo/us/en.html"));
        assertEquals("", HeaderModel.rewriteLink("  "));
        assertEquals("", HeaderModel.rewriteLink(null));
    }

    private HeaderModel adapt() {
        Resource resource = context.resourceResolver().getResource(PATH);
        assertNotNull(resource);
        return resource.adaptTo(HeaderModel.class);
    }
}
