package com.adobe.aem.guides.demo.core.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.adobe.aem.guides.demo.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class CarouselModelTest {

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(CarouselModel.class, SlideModel.class, TagModel.class);
    }

    @Test
    void mapsNestedSlidesAndTags() {
        context.create().resource("/content/carousel",
                "sling:resourceType", "demo/components/carousel");
        context.create().resource("/content/carousel/slides/item0",
                "title", "Peonies",
                "path", "/content/dam/peonies.jpg");
        context.create().resource("/content/carousel/slides/item0/tags/item0",
                "text", "Seasonal");
        context.create().resource("/content/carousel/slides/item0/tags/item1",
                "text", "Blush");

        Resource resource = context.resourceResolver().getResource("/content/carousel");
        CarouselModel model = resource.adaptTo(CarouselModel.class);

        assertNotNull(model);
        assertEquals(1, model.getSlides().size());
        SlideModel slide = model.getSlides().get(0);
        assertEquals("Peonies", slide.getTitle());
        assertEquals("/content/dam/peonies.jpg", slide.getPath());
        assertEquals(2, slide.getTags().size());
        assertEquals("Seasonal", slide.getTags().get(0).getText());
        assertEquals("Blush", slide.getTags().get(1).getText());
    }

    @Test
    void returnsEmptyListsWhenUnconfigured() {
        context.create().resource("/content/empty");
        Resource resource = context.resourceResolver().getResource("/content/empty");
        CarouselModel model = resource.adaptTo(CarouselModel.class);

        assertNotNull(model);
        assertTrue(model.getSlides().isEmpty());

        context.create().resource("/content/slide", "title", "Rose");
        SlideModel slide = context.resourceResolver().getResource("/content/slide").adaptTo(SlideModel.class);
        assertNotNull(slide);
        assertTrue(slide.getTags().isEmpty());
    }
}
