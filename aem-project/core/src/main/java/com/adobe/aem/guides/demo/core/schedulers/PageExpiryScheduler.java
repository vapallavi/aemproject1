package com.adobe.aem.guides.demo.core.schedulers;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.Replicator;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component(service = Runnable.class, immediate = true)
@Designate(ocd = ExpirySchedulerConfig.class)
public class PageExpiryScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PageExpiryScheduler.class);
    private static final String SERVICE_USER = "expiry-service";

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private QueryBuilder queryBuilder;

    @Reference
    private Replicator replicator;

    private String cronExpression;
    private String contentPath;
    private boolean enabled;

    @Activate
    @Modified
    protected void activate(ExpirySchedulerConfig config) {
        this.enabled = config.enabled();
        this.cronExpression = config.cronExpression();
        this.contentPath = config.contentPath();
        removeScheduler();
        if (enabled) {
            addScheduler();
        }
    }

    @Deactivate
    protected void deactivate() {
        removeScheduler();
    }

    private void addScheduler() {
        ScheduleOptions options = scheduler.EXPR(cronExpression);
        options.name("PageExpiryScheduler");
        options.canRunConcurrently(false);
        options.onLeaderOnly(true);
        scheduler.schedule(this, options);
        LOG.info("Page Expiry Scheduler started with cron: {}", cronExpression);
    }

    private void removeScheduler() {
        scheduler.unschedule("PageExpiryScheduler");
    }

    @Override
    public void run() {
        LOG.info("Page Expiry Scheduler running...");
        Map<String, Object> authInfo = Collections.singletonMap(
                ResourceResolverFactory.SUBSERVICE, SERVICE_USER);
        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            Session session = resolver.adaptTo(Session.class);
            processPages(resolver, session);
        } catch (Exception e) {
            LOG.error("Failed to get service resource resolver for {}", SERVICE_USER, e);
        }
    }

    private void processPages(ResourceResolver resolver, Session session) {
        Map<String, String> map = new HashMap<>();
        map.put("path", contentPath);
        map.put("type", "cq:Page");
        map.put("property", "jcr:content/expiryDate");
        map.put("property.operation", "exists");
        map.put("p.limit", "-1");

        Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
        SearchResult result = query.getResult();

        Calendar now = Calendar.getInstance();
        Calendar windowStart = (Calendar) now.clone();
        windowStart.add(Calendar.MINUTE, -3);

        for (Hit hit : result.getHits()) {
            try {
                Resource pageRes = hit.getResource();
                Resource content = pageRes.getChild("jcr:content");
                if (content == null) {
                    continue;
                }

                Calendar expiry = content.getValueMap().get("expiryDate", Calendar.class);
                if (expiry == null) {
                    continue;
                }

                String path = pageRes.getPath();

                // Current window → Publish
                if (expiry.after(windowStart) && !expiry.after(now)) {
                    LOG.info("Publishing page (expiry matches current): {}", path);
                    replicator.replicate(session, ReplicationActionType.ACTIVATE, path);
                }
                // Past → Unpublish
                else if (expiry.before(now)) {
                    LOG.info("Unpublishing page (expiry in past): {}", path);
                    replicator.replicate(session, ReplicationActionType.DEACTIVATE, path);
                }
            } catch (Exception e) {
                LOG.error("Error processing page", e);
            }
        }
    }
}