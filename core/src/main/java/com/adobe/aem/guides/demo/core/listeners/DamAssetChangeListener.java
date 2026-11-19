package com.adobe.aem.guides.demo.core.listeners;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Detects DAM asset changes under a configurable path using
 * {@link ResourceChangeListener} (not an OSGi EventHandler / JCR EventListener)
 * and queues a Sling Job. {@link com.adobe.aem.guides.demo.core.jobs.DamAssetPublishJob}
 * performs the actual publish.
 */
@Component(
    service = ResourceChangeListener.class,
    immediate = true,
    property = {
        ResourceChangeListener.PATHS + "=/content/dam",
        ResourceChangeListener.CHANGES + "=ADDED",
        ResourceChangeListener.CHANGES + "=CHANGED"
    }
)
@Designate(ocd = DamAssetPublishConfig.class)
public class DamAssetChangeListener implements ResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(DamAssetChangeListener.class);

    public static final String JOB_TOPIC = "com/adobe/aem/guides/demo/core/jobs/dam-asset-publish";
    public static final String ASSET_PATH = "assetPath";
    public static final String FILE_NAME = "fileName";

    @Reference
    private JobManager jobManager;

    private String damPath;
    private boolean enabled;

    @Activate
    @Modified
    protected void activate(DamAssetPublishConfig config) {
        this.damPath = trimTrailingSlash(config.damPath());
        this.enabled = config.enabled();
        LOG.info("DAM Asset Change Listener activated. enabled={}, damPath={}", enabled, damPath);
    }

    @Override
    public void onChange(List<ResourceChange> changes) {
        if (!enabled || changes == null || changes.isEmpty()) {
            return;
        }

        Set<String> queued = new HashSet<>();
        for (ResourceChange change : changes) {
            String rawPath = change.getPath();
            // Replication writes cq:lastReplicated on jcr:content; ignore those so we
            // do not queue another publish. Real edits land on renditions or metadata.
            if (rawPath != null
                    && rawPath.endsWith("/jcr:content")
                    && change.getType() == ResourceChange.ChangeType.CHANGED) {
                continue;
            }

            String assetPath = resolveAssetPath(rawPath);
            if (assetPath == null || !queued.add(assetPath)) {
                continue;
            }

            String fileName = assetPath.substring(assetPath.lastIndexOf('/') + 1);

            Map<String, Object> properties = new HashMap<>();
            properties.put(ASSET_PATH, assetPath);
            properties.put(FILE_NAME, fileName);

            if (jobManager.addJob(JOB_TOPIC, properties) == null) {
                LOG.error("Failed to queue DAM publish job for path {}", assetPath);
            } else {
                LOG.info("Asset change detected ({}), queued publish job → path={}, file={}",
                        change.getType(), assetPath, fileName);
            }
        }
    }

    /**
     * Map a descendant path (jcr:content, renditions, metadata) to the dam:Asset path.
     * Returns null when the change is outside the configured DAM folder.
     */
    String resolveAssetPath(String path) {
        if (path == null || damPath == null || damPath.isEmpty()) {
            return null;
        }
        if (!path.equals(damPath) && !path.startsWith(damPath + "/")) {
            return null;
        }
        String assetPath = path;
        int jcrContentIdx = path.indexOf("/jcr:content");
        if (jcrContentIdx > 0) {
            assetPath = path.substring(0, jcrContentIdx);
        }
        if (assetPath.equals(damPath) || assetPath.equals("/content/dam")) {
            return null;
        }
        return assetPath;
    }

    private static String trimTrailingSlash(String path) {
        if (path == null || path.length() <= 1 || !path.endsWith("/")) {
            return path;
        }
        return path.substring(0, path.length() - 1);
    }
}
