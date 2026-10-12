package com.adobe.aem.guides.demo.core.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(
    adaptables = Resource.class,
    defaultInjectionStrategy = org.apache.sling.models.annotations.DefaultInjectionStrategy.OPTIONAL
)
public class FlowerModel {

    @ValueMapValue
    private String flowerName;

    @ValueMapValue
    private String flowerColor;

    @ValueMapValue
    private String flowerPrice;

    public String getFlowerName() {
        return flowerName;
    }

    public String getFlowerColor() {
        return flowerColor;
    }

    public String getFlowerPrice() {
        return flowerPrice;
    }
}