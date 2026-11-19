package com.adobe.aem.guides.demo.core.servlets;

import com.adobe.aem.guides.demo.core.regiontree.RegionTreeConstants;
import com.adobe.aem.guides.demo.core.regiontree.RegionTreeJson;
import com.adobe.aem.guides.demo.core.regiontree.RegionTreeUtils;
import com.day.cq.commons.jcr.JcrConstants;
import com.google.gson.JsonObject;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Imports a region tree JSON document into {@code /conf/region-tree/temp} and queues
 * comparison job {@code com/jet2/region-tree/temp}.
 *
 * <pre>
 * POST /bin/regiontree/import
 * Content-Type: application/json
 *
 * Success: {"status":200,"message":"The temp folder is created correctly!!"}
 * Failure: {"status":500,"message":"Error during the creation of the temp folder!!!"}
 * </pre>
 *
 * Writes use the {@code region-tree-service} service user, not the request session.
 *
 * Sample cURL (PowerShell) against local author:
 * <pre>
 * $login = Invoke-WebRequest http://localhost:4502/libs/granite/core/content/login.html `
 *     -SessionVariable s -Headers @{ Authorization = "Basic YWRtaW46YWRtaW4=" }
 * $token = (Invoke-RestMethod http://localhost:4502/libs/granite/csrf/token.json `
 *     -WebSession $s -Headers @{ Authorization = "Basic YWRtaW46YWRtaW4=" }).token
 * Invoke-RestMethod http://localhost:4502/bin/regiontree/import -Method POST -WebSession $s `
 *     -Headers @{ Authorization = "Basic YWRtaW46YWRtaW4="; "CSRF-Token" = $token;
 *                 Referer = "http://localhost:4502/" } `
 *     -ContentType "application/json" `
 *     -Body '{"regions":[{"id":"1","name":"Austria","areas":[{"id":"49","name":"Salzburg","resorts":[{"id":"180","name":"Salzburg City"}]},{"id":"50","name":"Vienna","resorts":[{"id":"181","name":"Vienna City"}]}]}]}'
 * </pre>
 */
@Component(service = Servlet.class, immediate = true)
@SlingServletPaths("/bin/regiontree/import")
@ServiceDescription("Region tree JSON import servlet")
public class RegionTreeImportServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(RegionTreeImportServlet.class);

    @Reference
    private transient ResourceResolverFactory resourceResolverFactory;

    @Reference
    private transient JobManager jobManager;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        String rawJson;
        try {
            rawJson = readBody(request);
        } catch (IOException e) {
            LOG.error("Failed to read import request body", e);
            writeFailure(response);
            return;
        }

        if (rawJson == null || rawJson.isBlank()) {
            LOG.error("Region tree import body is empty");
            writeFailure(response);
            return;
        }

        JsonObject payload;
        try {
            payload = RegionTreeJson.parseObject(rawJson);
        } catch (RuntimeException e) {
            LOG.error("Region tree import JSON is invalid", e);
            writeFailure(response);
            return;
        }

        String startIso = RegionTreeUtils.nowIso();
        LOG.info("Region tree import started at {}", startIso);

        try (ResourceResolver resolver = RegionTreeUtils.getServiceResolver(resourceResolverFactory)) {
            RegionTreeUtils.getOrCreateFolder(resolver, RegionTreeConstants.ROOT_PATH);
            RegionTreeUtils.deleteIfExists(resolver, RegionTreeConstants.TEMP_PATH);
            resolver.commit();

            Resource root = resolver.getResource(RegionTreeConstants.ROOT_PATH);
            if (root == null) {
                throw new IllegalStateException("Missing " + RegionTreeConstants.ROOT_PATH);
            }
            Map<String, Object> tempProps = new HashMap<>();
            tempProps.put(JcrConstants.JCR_PRIMARYTYPE, "sling:Folder");
            Resource temp = resolver.create(root, "temp", tempProps);

            String stamp = RegionTreeUtils.nowIso();
            int written = RegionTreeUtils.writeTempTree(resolver, temp, payload, stamp);
            String endIso = RegionTreeUtils.nowIso();
            RegionTreeUtils.writeDataNode(resolver, rawJson, startIso, endIso);
            resolver.commit();

            Map<String, Object> jobProps = new HashMap<>();
            jobProps.put("tempPath", RegionTreeConstants.TEMP_PATH);
            jobProps.put("actualPath", RegionTreeConstants.ACTUAL_PATH);
            jobProps.put(RegionTreeConstants.PN_START_DATE_IMPORT, startIso);
            jobProps.put(RegionTreeConstants.PN_END_DATE_IMPORT, endIso);
            if (jobManager.addJob(RegionTreeConstants.JOB_TOPIC_TEMP, jobProps) == null) {
                LOG.error("Temp tree was written ({} nodes) but job {} was not queued",
                        written, RegionTreeConstants.JOB_TOPIC_TEMP);
                writeFailure(response);
                return;
            }

            LOG.info("Temp folder created with {} nodes; queued job {}", written,
                    RegionTreeConstants.JOB_TOPIC_TEMP);
            writeJson(response, 200, RegionTreeConstants.MSG_SUCCESS);
        } catch (LoginException e) {
            LOG.error("Cannot get service ResourceResolver. Check mapping "
                            + "'aem-guides-demo.core:{}=[{}]'.",
                    RegionTreeConstants.SUBSERVICE, RegionTreeConstants.SUBSERVICE, e);
            writeFailure(response);
        } catch (Exception e) {
            LOG.error("Error during the creation of the temp folder", e);
            writeFailure(response);
        }
    }

    private static String readBody(SlingHttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (body.length() > 0) {
                    body.append('\n');
                }
                body.append(line);
            }
        }
        return body.toString();
    }

    private static void writeFailure(SlingHttpServletResponse response) throws IOException {
        writeJson(response, 500, RegionTreeConstants.MSG_FAILURE);
    }

    private static void writeJson(SlingHttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
