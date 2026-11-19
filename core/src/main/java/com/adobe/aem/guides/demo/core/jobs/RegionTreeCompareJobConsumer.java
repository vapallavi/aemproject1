package com.adobe.aem.guides.demo.core.jobs;

import com.adobe.aem.guides.demo.core.regiontree.RegionTreeComparison;
import com.adobe.aem.guides.demo.core.regiontree.RegionTreeConstants;
import com.adobe.aem.guides.demo.core.regiontree.RegionTreeNode;
import com.adobe.aem.guides.demo.core.regiontree.RegionTreeUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Compares {@code /conf/region-tree/temp} with {@code /conf/region-tree/actual},
 * deletes temp, and queues {@code com/jet2/region-tree/update} with the change lists.
 * <p>
 * Job properties must be String arrays: Sling JobManager rejects arbitrary Java lists.
 */
@Component(
    service = JobConsumer.class,
    immediate = true,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=" + RegionTreeConstants.JOB_TOPIC_TEMP
    }
)
public class RegionTreeCompareJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(RegionTreeCompareJobConsumer.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private JobManager jobManager;

    @Override
    public JobResult process(Job job) {
        LOG.info("Region tree compare job started ({})", job.getId());

        try (ResourceResolver resolver = RegionTreeUtils.getServiceResolver(resourceResolverFactory)) {
            Resource tempRoot = resolver.getResource(RegionTreeConstants.TEMP_PATH);
            if (tempRoot == null) {
                LOG.error("Temp tree is missing at {}", RegionTreeConstants.TEMP_PATH);
                return JobResult.FAILED;
            }

            Resource actualRoot = resolver.getResource(RegionTreeConstants.ACTUAL_PATH);
            if (actualRoot == null) {
                LOG.info("Actual tree {} is not present; every temp node will be treated as created",
                        RegionTreeConstants.ACTUAL_PATH);
            }

            Map<String, RegionTreeNode> tempNodes = RegionTreeUtils.collectNodes(tempRoot);
            Map<String, RegionTreeNode> actualNodes = RegionTreeUtils.collectNodes(actualRoot);
            LOG.info("Collected {} temp nodes and {} actual nodes", tempNodes.size(), actualNodes.size());

            RegionTreeComparison comparison = RegionTreeUtils.compare(tempNodes, actualNodes);
            LOG.info("Comparison result: created={}, updated={}, moved={}",
                    comparison.getCreated().size(),
                    comparison.getUpdated().size(),
                    comparison.getMoved().size());
            logIds("created", comparison.getCreated());
            logIds("updated", comparison.getUpdated());
            logIds("moved", comparison.getMoved());

            Map<String, Object> properties = new HashMap<>();
            properties.put(RegionTreeConstants.JOB_CREATED, comparison.createdPayloads());
            properties.put(RegionTreeConstants.JOB_UPDATED, comparison.updatedPayloads());
            properties.put(RegionTreeConstants.JOB_MOVED, comparison.movedPayloads());

            if (jobManager.addJob(RegionTreeConstants.JOB_TOPIC_UPDATE, properties) == null) {
                LOG.error("Failed to queue update job {}", RegionTreeConstants.JOB_TOPIC_UPDATE);
                return JobResult.FAILED;
            }
            LOG.info("Queued update job {}", RegionTreeConstants.JOB_TOPIC_UPDATE);

            RegionTreeUtils.deleteIfExists(resolver, RegionTreeConstants.TEMP_PATH);
            resolver.commit();
            LOG.info("Deleted temp tree {}", RegionTreeConstants.TEMP_PATH);
            return JobResult.OK;

        } catch (LoginException e) {
            LOG.error("Cannot get service ResourceResolver for region-tree compare", e);
            return JobResult.FAILED;
        } catch (Exception e) {
            LOG.error("Region tree compare job failed", e);
            return JobResult.FAILED;
        }
    }

    private static void logIds(String label, Iterable<RegionTreeNode> nodes) {
        for (RegionTreeNode node : nodes) {
            LOG.info("  {} id={} title='{}' regionId={} areaId={} path={}",
                    label, node.getId(), node.getTitle(), node.getRegionId(),
                    node.getAreaId(), node.getRelativePath());
        }
    }
}
