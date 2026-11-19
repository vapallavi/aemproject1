package com.adobe.aem.guides.demo.core.schedulers;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component(
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = PagePublishSchedulerConfig.class)
public class PagePublishScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(PagePublishScheduler.class);
    public static final String JOB_TOPIC = "com/yourcompany/core/jobs/pagepublish";

    @Reference
    private JobManager jobManager;

    private PagePublishSchedulerConfig config;

    @Activate
    @Modified
    protected void activate(PagePublishSchedulerConfig config) {
        this.config = config;
        unscheduleExistingJobs();

        if (config.enabled()) {
            scheduleJob();
        } else {
            LOG.info("Page Publish Scheduler is disabled");
        }
    }

    @Deactivate
    protected void deactivate() {
        unscheduleExistingJobs();
    }

    private void scheduleJob() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("pagePath", config.pagePath());

        JobBuilder.ScheduleBuilder scheduleBuilder = jobManager
                .createJob(JOB_TOPIC)
                .properties(properties)
                .schedule();

        scheduleBuilder.cron(config.cronExpression());
        ScheduledJobInfo scheduledJobInfo = scheduleBuilder.add();

        if (scheduledJobInfo != null) {
            LOG.info("Successfully scheduled Page Publish Job | cron={} | path={}",
                    config.cronExpression(), config.pagePath());
        } else {
            LOG.error("Failed to schedule Page Publish Job");
        }
    }

    private void unscheduleExistingJobs() {
        Collection<ScheduledJobInfo> jobs = jobManager.getScheduledJobs(JOB_TOPIC, 0, null);
        for (ScheduledJobInfo job : jobs) {
            job.unschedule();
        }
        if (!jobs.isEmpty()) {
            LOG.info("Unscheduled {} existing Page Publish job(s)", jobs.size());
        }
    }
}