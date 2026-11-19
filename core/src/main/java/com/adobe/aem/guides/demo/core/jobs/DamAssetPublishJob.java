package com.adobe.aem.guides.demo.core.jobs;

import com.adobe.aem.guides.demo.core.listeners.DamAssetChangeListener;
import com.day.cq.dam.api.Asset;
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
import java.util.Collections;
import java.util.Map;

/**
 * Sling {@link JobConsumer} that publishes a DAM asset. Publishing is done here,
 * not in an EventHandler / EventListener.
 */
@Component(
    service = JobConsumer.class,
    immediate = true,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=" + DamAssetChangeListener.JOB_TOPIC
    }
)
public class DamAssetPublishJob implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(DamAssetPublishJob.class);
    static final String SUBSERVICE = "dam-publish-service";

    @Reference
    private Replicator replicator;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public JobResult process(Job job) {
        String assetPath = job.getProperty(DamAssetChangeListener.ASSET_PATH, String.class);
        String fileName = job.getProperty(DamAssetChangeListener.FILE_NAME, String.class);

        if (assetPath == null || assetPath.isEmpty()) {
            LOG.error("DAM publish job is missing assetPath. Cancelling.");
            return JobResult.CANCEL;
        }

        Map<String, Object> authInfo = Collections.singletonMap(
                ResourceResolverFactory.SUBSERVICE, SUBSERVICE);

        try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            String publishPath = toAssetPath(assetPath);
            Resource resource = resolver.getResource(publishPath);
            if (resource == null) {
                LOG.warn("Asset no longer exists, skipping publish: {}", publishPath);
                return JobResult.OK;
            }

            Asset asset = resource.adaptTo(Asset.class);
            if (asset == null) {
                LOG.debug("Path is not a dam:Asset, skipping: {}", publishPath);
                return JobResult.OK;
            }

            publishPath = asset.getPath();

            Session session = resolver.adaptTo(Session.class);
            if (session == null) {
                LOG.error("Could not adapt resolver to JCR Session (user={})", resolver.getUserID());
                return JobResult.FAILED;
            }

            LOG.info("Publishing DAM asset via Sling Job → path={}, file={}, user={}",
                    publishPath, fileName, session.getUserID());
            replicator.replicate(session, ReplicationActionType.ACTIVATE, publishPath);
            LOG.info("Successfully published DAM asset → path={}, file={}", publishPath, fileName);
            return JobResult.OK;

        } catch (LoginException e) {
            LOG.error("Cannot get service ResourceResolver. Check OSGi mapping "
                            + "'aem-guides-demo.core:{}=[{}]' and that user {} exists.",
                    SUBSERVICE, SUBSERVICE, SUBSERVICE, e);
            return JobResult.CANCEL;
        } catch (ReplicationException e) {
            LOG.error("Replication failed for asset {}", assetPath, e);
            return JobResult.FAILED;
        } catch (Exception e) {
            LOG.error("Unexpected error publishing asset {}", assetPath, e);
            return JobResult.FAILED;
        }
    }

    static String toAssetPath(String path) {
        if (path == null) {
            return null;
        }
        int jcrContentIdx = path.indexOf("/jcr:content");
        return jcrContentIdx > 0 ? path.substring(0, jcrContentIdx) : path;
    }
}
