package com.adobe.aem.guides.demo.core.models;

import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class SlideModel {

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String path;

    @ChildResource(name = "tags")
    private List<TagModel> tags;

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public List<TagModel> getTags() {
        return tags != null ? tags : Collections.emptyList();
    }
}
