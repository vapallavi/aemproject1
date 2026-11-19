package com.adobe.aem.guides.demo.core.schedulers;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component(
    service = JobConsumer.class,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=" + PagePublishScheduler.JOB_TOPIC
    }
)
public class PagePublishJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(PagePublishJobConsumer.class);
    private static final String SUBSERVICE = "page-publish-service";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Replicator replicator;

    @Override
    public JobResult process(Job job) {
        String pagePath = job.getProperty("pagePath", String.class);
        if (pagePath == null || pagePath.isBlank()) {
            LOG.error("pagePath is missing in the job. Aborting.");
            return JobResult.CANCEL;
        }

        LOG.info("Starting Page Publish Job for path: {}", pagePath);

        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, SUBSERVICE);

        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(authInfo)) {
            Resource root = resolver.getResource(pagePath);
            if (root == null) {
                LOG.warn("Configured path does not exist: {}", pagePath);
                return JobResult.OK;
            }

            Session session = resolver.adaptTo(Session.class);
            int publishedCount = publishPages(root, session);

            LOG.info("Page Publish Job completed. Total pages published: {}", publishedCount);
            return JobResult.OK;

        } catch (LoginException e) {
            LOG.error("Failed to obtain service ResourceResolver", e);
            return JobResult.FAILED;
        } catch (Exception e) {
            LOG.error("Unexpected error during page publishing", e);
            return JobResult.FAILED;
        }
    }

    private int publishPages(Resource resource, Session session) {
        int count = 0;
        if (resource == null) return count;

        // Publish if it is a page
        if ("cq:Page".equals(resource.getResourceType())) {
            String path = resource.getPath();
            try {
                replicator.replicate(session, ReplicationActionType.ACTIVATE, path);
                LOG.info("Published page: {}", path);
                count++;
            } catch (ReplicationException e) {
                LOG.error("Failed to publish page: {}", path, e);
            }
        }

        // Recurse into children
        Iterator<Resource> children = resource.listChildren();
        while (children.hasNext()) {
            count += publishPages(children.next(), session);
        }
        return count;
    }
}