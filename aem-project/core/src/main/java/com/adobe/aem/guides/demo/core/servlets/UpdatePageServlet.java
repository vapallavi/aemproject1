package com.adobe.aem.guides.demo.core.servlets;

import com.adobe.aem.guides.demo.core.services.ApiConfigService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/updatepage",
        "sling.servlet.methods=GET"
    }
)
public class UpdatePageServlet extends SlingAllMethodsServlet {

    @Reference
    private ApiConfigService apiConfigService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        ResourceResolver resolver = request.getResourceResolver();

        String pagePath = apiConfigService.getPagePath();
        String clientId = apiConfigService.getClientId();
        String apiToken = apiConfigService.getApiToken();

        Resource pageResource = resolver.getResource(pagePath);
        if (pageResource == null) {
            response.getWriter().write("Invalid path: " + pagePath);
            return;
        }

        Resource jcrContent = pageResource.getChild("jcr:content");
        if (jcrContent == null) {
            response.getWriter().write("jcr:content not found for path: " + pagePath);
            return;
        }

        ModifiableValueMap properties = jcrContent.adaptTo(ModifiableValueMap.class);
        if (properties != null) {
            properties.put("clientId", clientId);
            properties.put("apiToken", apiToken);
            resolver.commit();
            response.getWriter().write("Successfully updated properties on: " + pagePath);
        } else {
            response.getWriter().write("Could not update properties");
        }
    }
}