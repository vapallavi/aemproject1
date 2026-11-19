package com.adobe.aem.guides.demo.core.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "API Configuration", description = "Configuration for Client ID, API Token and Page Path")
public @interface ApiConfig {

    @AttributeDefinition(name = "Client ID", description = "Client ID value")
    String clientId() default "";

    @AttributeDefinition(name = "API Token", description = "API Token value")
    String apiToken() default "";

    @AttributeDefinition(name = "Page Path", description = "Page path to update")
    String pagePath() default "/content/demo/us/en/flower";
}