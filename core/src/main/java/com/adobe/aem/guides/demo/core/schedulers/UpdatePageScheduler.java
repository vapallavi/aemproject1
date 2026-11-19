package com.adobe.aem.guides.demo.core.schedulers;

import com.adobe.aem.guides.demo.core.services.ApiConfigService;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

@Component(service = UpdatePageScheduler.class, immediate = true)
public class UpdatePageScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(UpdatePageScheduler.class);
    private static final String JOB_NAME = "UpdatePageSchedulerJob";
    static final String SUBSERVICE = "page-publish-service";

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private ApiConfigService apiConfigService;

    @Activate
    protected void activate() {
        ScheduleOptions options = scheduler.EXPR("0 0/5 * * * ?");
        options.name(JOB_NAME);
        options.canRunConcurrently(false);
        scheduler.schedule(this, options);
        LOG.info("UpdatePageScheduler activated");
    }

    @Deactivate
    protected void deactivate() {
        scheduler.unschedule(JOB_NAME);
        LOG.info("UpdatePageScheduler deactivated");
    }

    @Override
    public void run() {
        Map<String, Object> authInfo = Collections.singletonMap(
                ResourceResolverFactory.SUBSERVICE, SUBSERVICE);
        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            updatePage(resolver);
        } catch (LoginException e) {
            LOG.error("Failed to get service resource resolver for {}", SUBSERVICE, e);
        } catch (Exception e) {
            LOG.error("Error updating page from scheduler", e);
        }
    }

    void updatePage(ResourceResolver resolver) throws Exception {
        String pagePath = apiConfigService.getPagePath();
        Resource pageResource = resolver.getResource(pagePath);
        if (pageResource == null) {
            LOG.warn("Invalid path: {}", pagePath);
            return;
        }

        Resource jcrContent = pageResource.getChild("jcr:content");
        if (jcrContent == null) {
            LOG.warn("jcr:content not found for path: {}", pagePath);
            return;
        }

        ModifiableValueMap properties = jcrContent.adaptTo(ModifiableValueMap.class);
        if (properties == null) {
            LOG.warn("Could not update properties on: {}", pagePath);
            return;
        }

        properties.put("clientId", apiConfigService.getClientId());
        properties.put("apiToken", apiConfigService.getApiToken());
        resolver.commit();
        LOG.info("Scheduler updated properties on: {}", pagePath);
    }
}
