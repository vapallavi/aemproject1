package com.adobe.aem.guides.demo.core.jobs;

import com.adobe.aem.guides.demo.core.regiontree.RegionTreeComparison;
import com.adobe.aem.guides.demo.core.regiontree.RegionTreeConstants;
import com.adobe.aem.guides.demo.core.regiontree.RegionTreeUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies created / updated / moved lists to {@code /conf/region-tree/actual}.
 * Consumes {@code com/jet2/region-tree/update} fired by {@link RegionTreeCompareJobConsumer}.
 */
@Component(
    service = JobConsumer.class,
    immediate = true,
    property = {
        JobConsumer.PROPERTY_TOPICS + "=" + RegionTreeConstants.JOB_TOPIC_UPDATE
    }
)
public class RegionTreeUpdateJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(RegionTreeUpdateJobConsumer.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public JobResult process(Job job) {
        LOG.info("Region tree update job started ({})", job.getId());
        try (ResourceResolver resolver = RegionTreeUtils.getServiceResolver(resourceResolverFactory)) {
            RegionTreeComparison comparison = RegionTreeUtils.fromJob(job);
            LOG.info("Applying created={}, updated={}, moved={} to {}",
                    comparison.getCreated().size(),
                    comparison.getUpdated().size(),
                    comparison.getMoved().size(),
                    RegionTreeConstants.ACTUAL_PATH);
            RegionTreeUtils.applyChanges(resolver, comparison);
            resolver.commit();
            LOG.info("Region tree actual folder updated at {}", RegionTreeConstants.ACTUAL_PATH);
            return JobResult.OK;
        } catch (LoginException e) {
            LOG.error("Cannot get service ResourceResolver for region-tree update", e);
            return JobResult.FAILED;
        } catch (Exception e) {
            LOG.error("Region tree update job failed", e);
            return JobResult.FAILED;
        }
    }
}
