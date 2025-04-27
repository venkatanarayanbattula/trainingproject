package com.training.project.core.models;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.day.cq.replication.Replicator;
import com.day.cq.replication.ReplicationActionType;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

@Component(service = WorkflowProcess.class,
        property = {"process.label=Activate Page Workflow Process"})
public class ActivatePageWorkflowProcess implements WorkflowProcess {

    private static final Logger log = LoggerFactory.getLogger(ActivatePageWorkflowProcess.class);

    @Reference
    private Replicator replicator;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args) throws WorkflowException {
        String payloadPath = workItem.getWorkflowData().getPayload().toString();
        log.info("Activating payload path: {}", payloadPath);

        ResourceResolver resourceResolver = null;
        try {
            // Get administrative service user session
            resourceResolver = resolverFactory.getServiceResourceResolver(
                    java.util.Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, "pagepublisher-user")
            );

            Session session = resourceResolver.adaptTo(Session.class);

            if (session != null) {
                replicator.replicate(session, ReplicationActionType.ACTIVATE, payloadPath);
                log.info("Page activated: {}", payloadPath);
            } else {
                log.error("Session is null, cannot activate page: {}", payloadPath);
            }

        } catch (Exception e) {
            log.error("Exception while activating page: {}", payloadPath, e);
            throw new WorkflowException("Failed to activate page", e);
        } finally {
            if (resourceResolver != null && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
    }
}
