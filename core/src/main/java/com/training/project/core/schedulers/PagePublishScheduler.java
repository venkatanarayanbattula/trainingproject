package com.training.project.core.schedulers;

import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobBuilder.ScheduleBuilder;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = PagePublishScheduler.class
)
@Designate(ocd = PagePublishScheduler.PagePublishSchedulerConfig.class)
public class PagePublishScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PagePublishScheduler.class);
    private static final String TOPIC = "com/trainingproject/core/pagepublishJob";

    @Reference
    private JobManager jobManager;

    private String cronExpression;
    private String pagePath;
    private boolean isEnabled;

    @ObjectClassDefinition(name = "Page Publish Scheduler Config")
    public @interface PagePublishSchedulerConfig {

        @AttributeDefinition(
                name = "Enable Scheduler",
                description = "Enable or disable the page publish scheduler",
                defaultValue = "false"
        )
        boolean enable();

        @AttributeDefinition(
                name = "Cron Expression",
                description = "Quartz cron expression to define job schedule (e.g., every 2 minutes: 0 0/2 * 1/1 * ? *)",
                defaultValue = "0 0/2 * 1/1 * ? *"
        )
        String scheduler_expression();

        @AttributeDefinition(
                name = "Page Path",
                description = "Absolute path of the page to publish (e.g., /content/trainingproject/us)"
        )
        String page_path();
    }

    @Activate
    @Modified
    protected void activate(PagePublishSchedulerConfig config) {
        this.isEnabled = config.enable();
        this.cronExpression = config.scheduler_expression();
        this.pagePath = config.page_path();

        logger.info("PagePublishScheduler config - enabled: {}, cron: {}, path: {}", isEnabled, cronExpression, pagePath);

        stopScheduledJob();

        if (isEnabled) {
            startScheduledJob();
        } else {
            logger.info("PagePublishScheduler is disabled in the configuration.");
        }
    }

    private void stopScheduledJob() {
        Collection<ScheduledJobInfo> jobs = jobManager.getScheduledJobs(TOPIC, 10, null);
        for (ScheduledJobInfo job : jobs) {
            job.unschedule();
            logger.info("Unscheduled existing job for topic: {}", TOPIC);
        }
    }

    private void startScheduledJob() {
        Collection<ScheduledJobInfo> existingJobs = jobManager.getScheduledJobs(TOPIC, 1, null);

        if (existingJobs.isEmpty()) {
            Map<String, Object> jobProps = new HashMap<>();
            jobProps.put("pagePath", pagePath);

            JobBuilder builder = jobManager.createJob(TOPIC).properties(jobProps);
            ScheduleBuilder scheduleBuilder = builder.schedule();

            if (cronExpression != null && !cronExpression.isEmpty()) {
                scheduleBuilder.cron(cronExpression);
            } else {
                logger.error("Cron expression is missing. Cannot schedule the job for page: {}", pagePath);
                return;
            }

            if (scheduleBuilder.add() != null) {
                logger.info("Scheduled job for page '{}' with cron expression: {}", pagePath, cronExpression);
            } else {
                logger.error("Failed to schedule job for page: {}", pagePath);
            }
        } else {
            logger.info("Scheduled job already exists for topic: {} (page: {})", TOPIC, pagePath);
        }
    }
}
