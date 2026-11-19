package com.adobe.aem.guides.demo.core.listeners;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "DAM Asset Change Publish Configuration",
    description = "Detects asset changes under a DAM path and publishes them with a Sling Job"
)
public @interface DamAssetPublishConfig {

    @AttributeDefinition(
        name = "DAM Path to Watch",
        description = "Any DAM folder path. Asset add/update under this path is published via Sling Job."
    )
    String damPath() default "/content/dam/demo";

    @AttributeDefinition(
        name = "Enabled",
        description = "Enable / disable change detection"
    )
    boolean enabled() default true;
}
