package com.adobe.aem.guides.demo.core.servlets;

import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Path-based servlet for contact form CRUD.
 *
 * <pre>
 * GET  /bin/contactformdata?name=&amp;email=&amp;mobile=              - create
 * GET  /bin/contactformdata?name=&amp;email=&amp;mobile=&amp;editPath=   - update
 * GET  /bin/contactformdata?cmd=delete&amp;path=                 - delete
 * POST /bin/contactformdata                                  - create/update (CSRF + Referer required)
 * DELETE /bin/contactformdata?path=                          - delete
 * </pre>
 *
 * Data is stored under {@code /content/usergenerated/contactformdata}.
 */
@Component(service = { Servlet.class })
@SlingServletPaths("/bin/contactformdata")
@ServiceDescription("Contact form CRUD servlet")
public class ContactFormServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(ContactFormServlet.class);

    static final String PARENT_PATH = "/content/usergenerated/contactformdata";
    static final String USERGENERATED_PATH = "/content/usergenerated";
    static final String PARAM_NAME = "name";
    static final String PARAM_EMAIL = "email";
    static final String PARAM_MOBILE = "mobile";
    static final String PARAM_EDIT_PATH = "editPath";
    static final String PARAM_PATH = "path";
    static final String PARAM_CMD = "cmd";
    static final String CMD_DELETE = "delete";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        if (CMD_DELETE.equalsIgnoreCase(request.getParameter(PARAM_CMD))) {
            deleteRecord(request, response);
        } else {
            saveRecord(request, response);
        }
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        saveRecord(request, response);
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        deleteRecord(request, response);
    }

    private void saveRecord(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain");

        String name = trim(request.getParameter(PARAM_NAME));
        String email = trim(request.getParameter(PARAM_EMAIL));
        String mobile = trim(request.getParameter(PARAM_MOBILE));
        String editPath = trim(request.getParameter(PARAM_EDIT_PATH));

        if (name.isEmpty() || email.isEmpty() || mobile.isEmpty()) {
            write(response, 400, "error: name, email and mobile are required");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        try {
            Resource parent = getOrCreateParent(resolver);
            Resource node;

            if (!editPath.isEmpty()) {
                if (!isChildOfParent(editPath)) {
                    write(response, 400, "error: invalid editPath");
                    return;
                }
                node = resolver.getResource(editPath);
                if (node == null) {
                    write(response, 404, "error: node not found");
                    return;
                }
                ModifiableValueMap map = node.adaptTo(ModifiableValueMap.class);
                if (map == null) {
                    write(response, 500, "error: cannot modify node");
                    return;
                }
                map.put(PARAM_NAME, name);
                map.put(PARAM_EMAIL, email);
                map.put(PARAM_MOBILE, mobile);
            } else {
                Map<String, Object> properties = new HashMap<>();
                properties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                properties.put(PARAM_NAME, name);
                properties.put(PARAM_EMAIL, email);
                properties.put(PARAM_MOBILE, mobile);
                node = resolver.create(parent, UUID.randomUUID().toString(), properties);
            }

            resolver.commit();
            LOG.info("Saved contact form data at {}", node.getPath());
            write(response, 200, "success");
        } catch (Exception e) {
            LOG.error("Failed to save contact form data", e);
            try {
                resolver.revert();
            } catch (Exception revertError) {
                LOG.debug("Resolver revert failed", revertError);
            }
            write(response, 500, "error: " + e.getMessage());
        }
    }

    private void deleteRecord(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain");

        String path = trim(request.getParameter(PARAM_PATH));
        if (!isChildOfParent(path)) {
            write(response, 400, "error: invalid path");
            return;
        }

        ResourceResolver resolver = request.getResourceResolver();
        try {
            Resource resource = resolver.getResource(path);
            if (resource == null) {
                write(response, 404, "error: node not found");
                return;
            }
            resolver.delete(resource);
            resolver.commit();
            LOG.info("Deleted contact form data at {}", path);
            write(response, 200, "deleted");
        } catch (Exception e) {
            LOG.error("Failed to delete contact form data at {}", path, e);
            try {
                resolver.revert();
            } catch (Exception revertError) {
                LOG.debug("Resolver revert failed", revertError);
            }
            write(response, 500, "error: " + e.getMessage());
        }
    }

    private Resource getOrCreateParent(ResourceResolver resolver) throws PersistenceException {
        Resource parent = resolver.getResource(PARENT_PATH);
        if (parent != null) {
            return parent;
        }

        Resource usergenerated = resolver.getResource(USERGENERATED_PATH);
        if (usergenerated == null) {
            Resource content = resolver.getResource("/content");
            if (content == null) {
                throw new PersistenceException("Repository path /content is not available");
            }
            Map<String, Object> folderProps = new HashMap<>();
            folderProps.put(JcrConstants.JCR_PRIMARYTYPE, "sling:Folder");
            usergenerated = resolver.create(content, "usergenerated", folderProps);
        }

        Map<String, Object> props = new HashMap<>();
        props.put(JcrConstants.JCR_PRIMARYTYPE, "sling:Folder");
        return resolver.create(usergenerated, "contactformdata", props);
    }

    private static boolean isChildOfParent(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String normalized = path.replace('\\', '/');
        return normalized.startsWith(PARENT_PATH + "/") && !normalized.contains("..");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static void write(SlingHttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.getWriter().write(body);
    }
}
