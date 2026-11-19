package com.adobe.aem.guides.demo.core.servlets;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.resourceTypes=profiles/datasources/images",
        "sling.servlet.methods=GET"
    }
)
public class ImagesDatasourceServlet extends SlingSafeMethodsServlet {

    private static final String IMAGES_FOLDER = "/content/dam/images";

    @Override  
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        ResourceResolver resolver = request.getResourceResolver();
        List<Resource> options = new ArrayList<>();

        Resource folder = resolver.getResource(IMAGES_FOLDER);
        if (folder != null) {
            for (Resource child : folder.getChildren()) {
                if (child.isResourceType("dam:Asset")) {
                    ValueMap vm = new ValueMapDecorator(new HashMap<>());
                    vm.put("value", child.getPath());   // path is stored
                    vm.put("text", child.getName());    // name is shown in dropdown
                    options.add(new ValueMapResource(resolver, "", "", vm));
                }
            }
        }

        DataSource ds = new SimpleDataSource(options.iterator());
        request.setAttribute(DataSource.class.getName(), ds);
    }
}