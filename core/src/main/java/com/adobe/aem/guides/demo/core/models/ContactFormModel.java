package com.adobe.aem.guides.demo.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ContactFormModel {

    private static final String PARENT_PATH = "/content/usergenerated/contactformdata";

    @SlingObject
    private SlingHttpServletRequest request;

    private String name = "";
    private String email = "";
    private String mobile = "";
    private String editPath = "";

    @PostConstruct
    protected void init() {
        if (request == null) {
            return;
        }
        String path = request.getParameter("editPath");
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        path = path.trim();
        if (!path.startsWith(PARENT_PATH + "/")) {
            return;
        }
        editPath = path;
        Resource resource = request.getResourceResolver().getResource(editPath);
        if (resource != null) {
            ValueMap props = resource.getValueMap();
            name = props.get("name", "");
            email = props.get("email", "");
            mobile = props.get("mobile", "");
        }
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getMobile() {
        return mobile;
    }

    public String getEditPath() {
        return editPath;
    }

    public boolean isEditMode() {
        return editPath != null && !editPath.isEmpty();
    }
}
