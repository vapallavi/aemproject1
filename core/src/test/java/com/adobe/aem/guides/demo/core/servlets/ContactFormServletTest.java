package com.adobe.aem.guides.demo.core.servlets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
class ContactFormServletTest {

    private final ContactFormServlet fixture = new ContactFormServlet();

    private static void seedParent(AemContext context) {
        context.create().resource("/content");
        context.create().resource("/content/usergenerated", "jcr:primaryType", "sling:Folder");
        context.create().resource(ContactFormServlet.PARENT_PATH, "jcr:primaryType", "sling:Folder");
    }

    @Test
    void doGetCreatesNodeUnderParent(AemContext context) throws IOException {
        seedParent(context);
        context.request().addRequestParameter("name", "Nani");
        context.request().addRequestParameter("email", "nani@gmail.com");
        context.request().addRequestParameter("mobile", "8930392098");

        fixture.doGet(context.request(), context.response());

        assertEquals(200, context.response().getStatus());
        assertEquals("success", context.response().getOutputAsString());

        Resource parent = context.resourceResolver().getResource(ContactFormServlet.PARENT_PATH);
        Resource child = firstChild(parent);
        assertNotNull(child);
        ValueMap props = child.getValueMap();
        assertEquals("Nani", props.get("name", String.class));
        assertEquals("nani@gmail.com", props.get("email", String.class));
        assertEquals("8930392098", props.get("mobile", String.class));
    }

    @Test
    void doGetUpdatesExistingNode(AemContext context) throws IOException {
        seedParent(context);
        Resource existing = context.create().resource(
                ContactFormServlet.PARENT_PATH + "/row-1",
                "jcr:primaryType", "nt:unstructured",
                "name", "Nani",
                "email", "nani@gmail.com",
                "mobile", "8930392098");

        context.request().addRequestParameter("name", "Delux");
        context.request().addRequestParameter("email", "delux@gmail.com");
        context.request().addRequestParameter("mobile", "8494930393");
        context.request().addRequestParameter("editPath", existing.getPath());

        fixture.doGet(context.request(), context.response());

        assertEquals("success", context.response().getOutputAsString());
        ValueMap props = context.resourceResolver().getResource(existing.getPath()).getValueMap();
        assertEquals("Delux", props.get("name", String.class));
        assertEquals("delux@gmail.com", props.get("email", String.class));
        assertEquals("8494930393", props.get("mobile", String.class));
    }

    @Test
    void doGetDeleteRemovesNode(AemContext context) throws IOException {
        seedParent(context);
        Resource existing = context.create().resource(
                ContactFormServlet.PARENT_PATH + "/row-2",
                "jcr:primaryType", "nt:unstructured",
                "name", "Nani",
                "email", "nani@gmail.com",
                "mobile", "8930392098");

        context.request().addRequestParameter("cmd", "delete");
        context.request().addRequestParameter("path", existing.getPath());

        fixture.doGet(context.request(), context.response());

        assertEquals("deleted", context.response().getOutputAsString());
        assertTrue(context.resourceResolver().getResource(ContactFormServlet.PARENT_PATH + "/row-2") == null);
    }

    @Test
    void missingFieldsReturnBadRequest(AemContext context) throws IOException {
        seedParent(context);
        context.request().addRequestParameter("name", "Nani");

        fixture.doGet(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("required"));
    }

    @Test
    void deleteOutsideParentIsRejected(AemContext context) throws IOException {
        seedParent(context);
        context.request().addRequestParameter("cmd", "delete");
        context.request().addRequestParameter("path", "/content/demo");

        fixture.doGet(context.request(), context.response());

        assertEquals(400, context.response().getStatus());
        assertTrue(context.response().getOutputAsString().contains("invalid path"));
    }

    private static Resource firstChild(Resource parent) {
        return StreamSupport.stream(parent.getChildren().spliterator(), false).findFirst().orElse(null);
    }
}
