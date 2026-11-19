package com.adobe.aem.guides.demo.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class FormDataTableModel {

    private static final Logger LOG = LoggerFactory.getLogger(FormDataTableModel.class);
    private static final String PARENT_PATH = "/content/usergenerated/contactformdata";
    private static final String QUERY =
            "SELECT * FROM [nt:base] AS node "
                    + "WHERE ISDESCENDANTNODE(node, '" + PARENT_PATH + "') "
                    + "AND node.[name] IS NOT NULL";

    @SlingObject
    private ResourceResolver resourceResolver;

    private final List<FormData> formDataList = new ArrayList<>();

    @PostConstruct
    protected void init() {
        if (resourceResolver == null) {
            return;
        }

        try {
            Iterator<Resource> results = resourceResolver.findResources(QUERY, Query.JCR_SQL2);
            while (results.hasNext()) {
                addRow(results.next());
            }
        } catch (Exception e) {
            LOG.warn("JCR query for contact form data failed, falling back to child iteration", e);
        }

        if (formDataList.isEmpty()) {
            Resource parent = resourceResolver.getResource(PARENT_PATH);
            if (parent != null) {
                for (Resource child : parent.getChildren()) {
                    addRow(child);
                }
            }
        }
    }

    private void addRow(Resource child) {
        if (child == null || PARENT_PATH.equals(child.getPath())) {
            return;
        }
        ValueMap props = child.getValueMap();
        String name = props.get("name", "");
        if (name == null || name.isEmpty()) {
            return;
        }
        FormData data = new FormData();
        data.setName(name);
        data.setEmail(props.get("email", ""));
        data.setMobile(props.get("mobile", ""));
        data.setPath(child.getPath());
        formDataList.add(data);
    }

    public List<FormData> getFormDataList() {
        return formDataList;
    }
}
