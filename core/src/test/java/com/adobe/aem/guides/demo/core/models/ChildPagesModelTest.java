package com.adobe.aem.guides.demo.core.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.adobe.aem.guides.demo.core.testcontext.AppAemContext;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class ChildPagesModelTest {

    private static final String PARENT_PATH = "/content/andrew/us/en";
    private static final String COMPONENT_PATH = PARENT_PATH + "/jcr:content/childpages";
    private static final List<String> CHILD_NAMES = Arrays.asList(
            "hi", "hello", "mixed", "all", "about", "contact",
            "products", "services", "blog", "news", "faq");

    private final AemContext context = AppAemContext.newAemContext();

    @BeforeEach
    void setUp() {
        context.addModelsForClasses(ChildPagesModel.class);
        context.create().page("/content/andrew");
        context.create().page("/content/andrew/us");
        context.create().page(PARENT_PATH, "template", "en");
        for (String name : CHILD_NAMES) {
            context.create().page(PARENT_PATH + "/" + name, "template", capitalize(name));
        }
        context.create().resource(COMPONENT_PATH,
                "sling:resourceType", ChildPagesModel.RESOURCE_TYPE,
                "pagePath", PARENT_PATH);
    }

    @Test
    void adaptsFromResourceAndListsParentThenChildren() {
        Resource resource = context.resourceResolver().getResource(COMPONENT_PATH);
        ChildPagesModel model = resource.adaptTo(ChildPagesModel.class);

        assertNotNull(model);
        assertTrue(model.isParentPageConfigured());
        assertTrue(model.isParentPageFound());
        assertEquals(12, model.getPages().size());
        assertEquals(PARENT_PATH, model.getPagePaths().get(0));
        assertEquals(PARENT_PATH + "/hi", model.getPagePaths().get(1));
        assertEquals(PARENT_PATH + "/faq", model.getPagePaths().get(11));
        assertEquals(ChildPagesModel.RESOURCE_TYPE, model.getExportedType());
    }

    @Test
    void pageSizeOneReturnsFirstThreePages() {
        ChildPagesModel model = adaptWithParams("1", "3");

        assertNotNull(model);
        assertEquals(Integer.valueOf(1), model.getPageSize());
        assertEquals(Integer.valueOf(3), model.getPageLimit());
        assertEquals(Arrays.asList(
                PARENT_PATH,
                PARENT_PATH + "/hi",
                PARENT_PATH + "/hello"), model.getPagePaths());
    }

    @Test
    void pageSizeTwoStartsAtFourthPage() {
        ChildPagesModel model = adaptWithParams("2", "3");

        assertNotNull(model);
        assertEquals(Arrays.asList(
                PARENT_PATH + "/mixed",
                PARENT_PATH + "/all",
                PARENT_PATH + "/about"), model.getPagePaths());
    }

    @Test
    void lastPageReturnsRemainingItems() {
        ChildPagesModel model = adaptWithParams("4", "3");

        assertNotNull(model);
        assertEquals(Arrays.asList(
                PARENT_PATH + "/blog",
                PARENT_PATH + "/news",
                PARENT_PATH + "/faq"), model.getPagePaths());
    }

    @Test
    void pageBeyondRangeReturnsEmptyList() {
        ChildPagesModel model = adaptWithParams("5", "3");

        assertNotNull(model);
        assertTrue(model.getPages().isEmpty());
        assertTrue(model.getPagePaths().isEmpty());
    }

    @Test
    void missingParentPathDoesNotFailAdaptation() {
        context.create().resource("/content/empty-childpages",
                "sling:resourceType", ChildPagesModel.RESOURCE_TYPE);
        Resource resource = context.resourceResolver().getResource("/content/empty-childpages");
        ChildPagesModel model = resource.adaptTo(ChildPagesModel.class);

        assertNotNull(model);
        assertFalse(model.isParentPageConfigured());
        assertFalse(model.isParentPageFound());
        assertTrue(model.getPages().isEmpty());
    }

    @Test
    void unknownParentPathIsConfiguredButNotFound() {
        context.create().resource("/content/missing-parent",
                "sling:resourceType", ChildPagesModel.RESOURCE_TYPE,
                "pagePath", "/content/does-not-exist");
        ChildPagesModel model = context.resourceResolver()
                .getResource("/content/missing-parent")
                .adaptTo(ChildPagesModel.class);

        assertNotNull(model);
        assertTrue(model.isParentPageConfigured());
        assertFalse(model.isParentPageFound());
        assertTrue(model.getPages().isEmpty());
    }

    private ChildPagesModel adaptWithParams(String pageSize, String pageLimit) {
        context.currentResource(COMPONENT_PATH);
        context.request().addRequestParameter("pageSize", pageSize);
        context.request().addRequestParameter("pageLimit", pageLimit);
        return context.request().adaptTo(ChildPagesModel.class);
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
