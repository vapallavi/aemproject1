package com.adobe.aem.guides.demo.core.servlets;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/checkaccess",
        "sling.servlet.methods=GET"
    }
)
public class CheckAccessServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");

        ResourceResolver resolver = request.getResourceResolver();
        String userId = resolver.getUserID();

        boolean hasAccess = false;

        try {
            UserManager userManager = resolver.adaptTo(UserManager.class);
            if (userManager != null) {
                Authorizable authorizable = userManager.getAuthorizable(userId);
                if (authorizable != null) {
                    Group accessGroup = (Group) userManager.getAuthorizable("Access");
                    if (accessGroup != null && accessGroup.isMember(authorizable)) {
                        hasAccess = true;
                    }
                }
            }
        } catch (Exception e) {
            response.getWriter().write("Error checking group membership: " + e.getMessage());
            return;
        }

        if (hasAccess) {
            response.getWriter().write(userId + " logged in has access");
        } else {
            response.getWriter().write(userId + " logged in doesn't have access");
        }
    }
}