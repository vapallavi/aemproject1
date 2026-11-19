package com.adobe.aem.guides.demo.core.schedulers;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "Page Publish Scheduler Configuration",
    description = "Automatically publishes all pages under a configured path based on a cron expression"
)
public @interface PagePublishSchedulerConfig {

    @AttributeDefinition(
        name = "Enabled",
        description = "Enable or disable the scheduler"
    )
    boolean enabled() default false;

    @AttributeDefinition(
        name = "Cron Expression",
        description = "Quartz cron expression (e.g. 0 0 2 * * ? = every day at 2:00 AM)"
    )
    String cronExpression() default "0 0 2 * * ?";

    @AttributeDefinition(
        name = "Page Path",
        description = "Root path under which all pages will be published (e.g. /content/mysite/en)"
    )
    String pagePath() default "/content/mysite";
}