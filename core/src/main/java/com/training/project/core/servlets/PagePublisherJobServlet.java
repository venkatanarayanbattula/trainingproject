package com.training.project.core.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.paths=/bin/triggerPagePublisherJob",
        "sling.servlet.methods=GET"
    }
)
public class PagePublisherJobServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(PagePublisherJobServlet.class);
    private static final String JOB_TOPIC = "com/example/jobs/pagepublisher";

    @Reference
    private JobManager jobManager;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        String rootPath = request.getParameter("rootPath");

        if (rootPath == null || rootPath.isEmpty()) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing 'rootPath' parameter.");
            return;
        }

        Map<String, Object> jobProps = new HashMap<>();
        jobProps.put("rootPath", rootPath);

        jobManager.addJob(JOB_TOPIC, jobProps);

        response.setContentType("application/json");
        response.getWriter().write("{\"status\":\"Job triggered\",\"rootPath\":\"" + rootPath + "\"}");
    }
}
