package com.adobe.aem.guides.demo.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
    adaptables = Resource.class,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class Featureimagetext {

    @ValueMapValue
    private String id;

    @ValueMapValue
    private Boolean usePageImage;

    @ValueMapValue
    private String alternatetext;

    @ValueMapValue
    private String assetPath;

    @ValueMapValue
    private String heading;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String ctalink;

    @ValueMapValue
    private String ctatext;

    public String getId() {
        return id;
    }

    public Boolean getUsePageImage() {
        return usePageImage != null && usePageImage;
    }

    public String getAlternatetext() {
        return alternatetext;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public String getHeading() {
        return heading;
    }

    public String getDescription() {
        return description;
    }

    public String getCtalink() {
        return ctalink;
    }

    public String getCtatext() {
        return ctatext;
    }

    public boolean isCtaValid() {
        return ctalink != null && !ctalink.isEmpty()
                && ctatext != null && !ctatext.isEmpty();
    }
}