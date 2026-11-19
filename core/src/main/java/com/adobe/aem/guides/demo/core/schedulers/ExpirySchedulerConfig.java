package com.adobe.aem.guides.demo.core.schedulers;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Page Expiry Scheduler Config", description = "Scheduler to Publish/Unpublish pages based on expiryDate")
public @interface ExpirySchedulerConfig {

    @AttributeDefinition(name = "Enabled", type = AttributeType.BOOLEAN)
    boolean enabled() default true;

    @AttributeDefinition(name = "Cron Expression", description = "Quartz Cron (e.g. 0 */3 * * * ? for every 3 min)", type = AttributeType.STRING)
    String cronExpression() default "0 */3 * * * ?";

    @AttributeDefinition(name = "Content Path", description = "Root path to search pages", type = AttributeType.STRING)
    String contentPath() default "/content";
}