package com.adobe.aem.guides.demo.core.eventlisteners;

import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Listens for activation (publish) events and sets {@code changed=true} on
 * the published resource's {@code jcr:content} node.
 * <p>
 * JCR writes are done in a Sling Job, not inside {@link #handleEvent(Event)}.
 * Replication events run on the OSGi Event Admin thread; a resolver opened
 * there often cannot see the node until the replication transaction completes.
 */
@Component(
    service = { EventHandler.class, JobConsumer.class },
    immediate = true,
    property = {
        EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC,
        JobConsumer.PROPERTY_TOPICS + "=" + PagePublishEventHandler.JOB_TOPIC
    }
)
public class PagePublishEventHandler implements EventHandler, JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(PagePublishEventHandler.class);

    static final String JOB_TOPIC = "com/adobe/aem/guides/demo/jobs/page-publish-property";
    private static final String SUBSERVICE = "page-publish-service";
    private static final String JOB_PROP_PATH = "pagePath";
    private static final String JCR_CONTENT = "jcr:content";
    private static final String CHANGED_PROPERTY = "changed";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private JobManager jobManager;

    @Activate
    protected void activate() {
        LOG.info("PagePublishEventHandler activated (job-based writer)");
    }

    @Override
    public void handleEvent(Event event) {
        ReplicationAction action = ReplicationAction.fromEvent(event);
        if (action == null || action.getType() != ReplicationActionType.ACTIVATE) {
            return;
        }

        String[] paths = action.getPaths();
        if (paths == null) {
            return;
        }

        for (String path : paths) {
            if (path == null || !path.startsWith("/content/")) {
                continue;
            }

            LOG.info("Page Published: {}", path);

            Map<String, Object> jobProps = new HashMap<>();
            jobProps.put(JOB_PROP_PATH, path);
            if (jobManager.addJob(JOB_TOPIC, jobProps) == null) {
                LOG.error("Failed to queue property-update job for path: {}", path);
            }
        }
    }

    @Override
    public JobResult process(Job job) {
        String path = job.getProperty(JOB_PROP_PATH, String.class);
        if (path == null || path.isBlank()) {
            LOG.error("Job is missing pagePath. Aborting.");
            return JobResult.CANCEL;
        }

        Map<String, Object> authInfo = Collections.singletonMap(
                ResourceResolverFactory.SUBSERVICE, SUBSERVICE);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                LOG.error("Could not adapt ResourceResolver to JCR Session for path: {}", path);
                return JobResult.CANCEL;
            }

            session.refresh(true);
            String contentPath = contentPath(path);
            LOG.info("Writing changed=true as user '{}' on {}", session.getUserID(), contentPath);

            if (!session.nodeExists(contentPath)) {
                LOG.warn("jcr:content not found or not readable at {} (user={})", contentPath, session.getUserID());
                return JobResult.FAILED;
            }

            Node contentNode = session.getNode(contentPath);
            if (contentNode.isNodeType("mix:versionable") && !contentNode.isCheckedOut()) {
                session.getWorkspace().getVersionManager().checkout(contentPath);
            }
            contentNode.setProperty(CHANGED_PROPERTY, true);
            session.save();
            LOG.info("Property changed=true added under: {}", contentPath);
            return JobResult.OK;

        } catch (LoginException e) {
            LOG.error("Failed to obtain service ResourceResolver for subservice {}", SUBSERVICE, e);
            return JobResult.CANCEL;
        } catch (Exception e) {
            LOG.error("Error updating published path: {}", path, e);
            return JobResult.FAILED;
        }
    }

    private String contentPath(String path) {
        if (path.endsWith("/" + JCR_CONTENT)) {
            return path;
        }
        return path + "/" + JCR_CONTENT;
    }
}
