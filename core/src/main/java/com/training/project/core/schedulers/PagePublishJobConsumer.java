package com.training.project.core.schedulers;

import com.day.cq.replication.Replicator;
import com.day.cq.replication.ReplicationActionType;
import org.apache.sling.api.resource.*;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;

@Component(
        service = JobConsumer.class,
        immediate = true,
        property = {
                JobConsumer.PROPERTY_TOPICS + "=com/trainingproject/core/pagepublishJob"
        }
)
public class PagePublishJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(PagePublishJobConsumer.class);
    private static final String SUBSERVICE_NAME = "pagepublisher-user";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private Replicator replicator;

    @Override
    public JobResult process(Job job) {
        String pagePath = job.getProperty("pagePath", String.class);

        if (pagePath == null || pagePath.isEmpty()) {
            LOG.error("Job did not contain a valid 'pagePath' property.");
            return JobResult.FAILED;
        }

        try (ResourceResolver resolver = getServiceResolver()) {
            Resource pageResource = resolver.getResource(pagePath);
            if (pageResource != null) {
                replicator.replicate(resolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE, pagePath);
                LOG.info("Page '{}' successfully published via JobConsumer.", pagePath);
                return JobResult.OK;
            } else {
                LOG.warn("Resource not found at path: {}", pagePath);
            }
        } catch (Exception e) {
            LOG.error("Exception while publishing page at path: {}", pagePath, e);
        }

        return JobResult.FAILED;
    }

    private ResourceResolver getServiceResolver() throws LoginException {
        Map<String, Object> authMap = new HashMap<>();
        authMap.put(ResourceResolverFactory.SUBSERVICE, SUBSERVICE_NAME);
        return resolverFactory.getServiceResourceResolver(authMap);
    }
}
