package com.adobe.aem.guides.demo.core.regiontree;

import com.day.cq.commons.jcr.JcrConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traversal, import, comparison, and apply helpers for the region tree.
 */
public final class RegionTreeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(RegionTreeUtils.class);

    private RegionTreeUtils() {
    }

    public static ResourceResolver getServiceResolver(ResourceResolverFactory factory) throws LoginException {
        Map<String, Object> authInfo = Collections.singletonMap(
                ResourceResolverFactory.SUBSERVICE, RegionTreeConstants.SUBSERVICE);
        return factory.getServiceResourceResolver(authInfo);
    }

    public static String nowIso() {
        return Instant.now().toString();
    }

    public static Resource getOrCreateFolder(ResourceResolver resolver, String path)
            throws PersistenceException {
        Resource existing = resolver.getResource(path);
        if (existing != null) {
            return existing;
        }
        int slash = path.lastIndexOf('/');
        if (slash <= 0) {
            throw new PersistenceException("Cannot create folder at " + path);
        }
        Resource parent = getOrCreateFolder(resolver, path.substring(0, slash));
        LOG.info("Creating missing folder {}", path);
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(JcrConstants.JCR_PRIMARYTYPE, "sling:Folder");
        return resolver.create(parent, path.substring(slash + 1), props);
    }

    public static void deleteIfExists(ResourceResolver resolver, String path) throws PersistenceException {
        Resource resource = resolver.getResource(path);
        if (resource != null) {
            LOG.info("Deleting existing resource {}", path);
            resolver.delete(resource);
        }
    }

    /**
     * Builds {@code /conf/region-tree/temp} from {@code regions → areas → resorts}.
     * Node names are the JSON {@code id} values.
     */
    public static int writeTempTree(ResourceResolver resolver, Resource tempRoot, JsonObject payload,
            String lastModified) throws PersistenceException {
        JsonArray regions = RegionTreeJson.array(payload, "regions");
        int count = 0;
        for (JsonElement regionEl : regions) {
            if (!regionEl.isJsonObject()) {
                continue;
            }
            JsonObject region = regionEl.getAsJsonObject();
            String regionId = RegionTreeJson.str(region, RegionTreeConstants.PN_ID);
            Resource regionNode = createEntity(resolver, tempRoot, region, regionId, "",
                    RegionTreeConstants.LEVEL_REGION, regionId, lastModified);
            count++;

            for (JsonElement areaEl : RegionTreeJson.array(region, "areas")) {
                if (!areaEl.isJsonObject()) {
                    continue;
                }
                JsonObject area = areaEl.getAsJsonObject();
                String areaId = RegionTreeJson.str(area, RegionTreeConstants.PN_ID);
                Resource areaNode = createEntity(resolver, regionNode, area, regionId, areaId,
                        RegionTreeConstants.LEVEL_AREA, regionId + "/" + areaId, lastModified);
                count++;

                for (JsonElement resortEl : RegionTreeJson.array(area, "resorts")) {
                    if (!resortEl.isJsonObject()) {
                        continue;
                    }
                    JsonObject resort = resortEl.getAsJsonObject();
                    String resortId = RegionTreeJson.str(resort, RegionTreeConstants.PN_ID);
                    createEntity(resolver, areaNode, resort, regionId, areaId,
                            RegionTreeConstants.LEVEL_RESORT,
                            regionId + "/" + areaId + "/" + resortId, lastModified);
                    count++;
                }
            }
        }
        return count;
    }

    public static void writeDataNode(ResourceResolver resolver, String rawJson, String startIso, String endIso)
            throws PersistenceException {
        Resource root = getOrCreateFolder(resolver, RegionTreeConstants.ROOT_PATH);
        Resource data = resolver.getResource(RegionTreeConstants.DATA_PATH);
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        props.put(RegionTreeConstants.PN_REGION_TREE_JSON, rawJson);
        props.put(RegionTreeConstants.PN_START_DATE_IMPORT, startIso);
        props.put(RegionTreeConstants.PN_END_DATE_IMPORT, endIso);
        if (data == null) {
            resolver.create(root, "data", props);
            LOG.info("Created data node {}", RegionTreeConstants.DATA_PATH);
            return;
        }
        ModifiableValueMap map = data.adaptTo(ModifiableValueMap.class);
        if (map == null) {
            throw new PersistenceException("Cannot modify " + RegionTreeConstants.DATA_PATH);
        }
        map.put(RegionTreeConstants.PN_REGION_TREE_JSON, rawJson);
        map.put(RegionTreeConstants.PN_START_DATE_IMPORT, startIso);
        map.put(RegionTreeConstants.PN_END_DATE_IMPORT, endIso);
        LOG.info("Updated data node {}", RegionTreeConstants.DATA_PATH);
    }

    public static Map<String, RegionTreeNode> collectNodes(Resource treeRoot) {
        Map<String, RegionTreeNode> nodes = new LinkedHashMap<>();
        if (treeRoot == null) {
            return nodes;
        }
        collectRecursive(treeRoot, treeRoot.getPath(), nodes);
        return nodes;
    }

    public static RegionTreeComparison compare(Map<String, RegionTreeNode> tempNodes,
            Map<String, RegionTreeNode> actualNodes) {
        List<RegionTreeNode> created = new ArrayList<>();
        List<RegionTreeNode> updated = new ArrayList<>();
        List<RegionTreeNode> moved = new ArrayList<>();

        for (RegionTreeNode temp : tempNodes.values()) {
            RegionTreeNode actual = actualNodes.get(temp.getId());
            if (actual == null) {
                created.add(temp);
            } else if (!temp.sameLocation(actual)) {
                moved.add(temp);
            } else if (!temp.sameAuthoredProperties(actual)) {
                updated.add(temp);
            }
        }
        return new RegionTreeComparison(created, updated, moved);
    }

    public static void applyChanges(ResourceResolver resolver, RegionTreeComparison comparison)
            throws PersistenceException {
        Resource actualRoot = getOrCreateFolder(resolver, RegionTreeConstants.ACTUAL_PATH);
        List<RegionTreeNode> created = new ArrayList<>(comparison.getCreated());
        created.sort((a, b) -> Integer.compare(depth(a.getRelativePath()), depth(b.getRelativePath())));
        for (RegionTreeNode node : created) {
            createOrReplace(resolver, actualRoot, node);
            LOG.info("Applied created node id={} path={}", node.getId(), node.getRelativePath());
        }
        for (RegionTreeNode node : comparison.getUpdated()) {
            Resource target = resolver.getResource(RegionTreeConstants.ACTUAL_PATH + "/" + node.getRelativePath());
            if (target == null) {
                createOrReplace(resolver, actualRoot, node);
            } else {
                writeProperties(target, node);
            }
            LOG.info("Applied updated node id={} path={}", node.getId(), node.getRelativePath());
        }
        Map<String, RegionTreeNode> actualById = collectNodes(actualRoot);
        for (RegionTreeNode node : comparison.getMoved()) {
            RegionTreeNode previous = actualById.get(node.getId());
            createOrReplace(resolver, actualRoot, node);
            if (previous != null && !previous.getRelativePath().equals(node.getRelativePath())) {
                Resource old = resolver.getResource(RegionTreeConstants.ACTUAL_PATH + "/" + previous.getRelativePath());
                if (old != null) {
                    resolver.delete(old);
                }
            }
            LOG.info("Applied moved node id={} to {}", node.getId(), node.getRelativePath());
        }
    }

    public static String[] stringArray(Job job, String key) {
        Object value = job.getProperty(key);
        if (value instanceof String[]) {
            return (String[]) value;
        }
        if (value instanceof String) {
            return new String[] {(String) value};
        }
        return new String[0];
    }

    public static RegionTreeComparison fromJob(Job job) {
        return new RegionTreeComparison(
                parsePayloads(stringArray(job, RegionTreeConstants.JOB_CREATED)),
                parsePayloads(stringArray(job, RegionTreeConstants.JOB_UPDATED)),
                parsePayloads(stringArray(job, RegionTreeConstants.JOB_MOVED)));
    }

    private static List<RegionTreeNode> parsePayloads(String[] payloads) {
        List<RegionTreeNode> nodes = new ArrayList<>();
        for (String payload : payloads) {
            nodes.add(RegionTreeNode.fromJobPayload(payload));
        }
        return nodes;
    }

    private static void collectRecursive(Resource resource, String treeRootPath,
            Map<String, RegionTreeNode> nodes) {
        for (Resource child : resource.getChildren()) {
            String id = child.getValueMap().get(RegionTreeConstants.PN_ID, String.class);
            if (id != null && !id.isEmpty()) {
                RegionTreeNode snapshot = RegionTreeNode.from(child, treeRootPath);
                if (nodes.containsKey(id)) {
                    LOG.warn("Duplicate id {} at {}; keeping first occurrence", id, child.getPath());
                } else {
                    nodes.put(id, snapshot);
                }
            }
            collectRecursive(child, treeRootPath, nodes);
        }
    }

    private static Resource createEntity(ResourceResolver resolver, Resource parent, JsonObject json,
            String regionId, String areaId, String level, String relativePath, String lastModified)
            throws PersistenceException {
        String id = RegionTreeJson.str(json, RegionTreeConstants.PN_ID);
        if (id.isEmpty()) {
            throw new PersistenceException("Entity is missing id under " + parent.getPath());
        }
        String name = RegionTreeJson.str(json, RegionTreeConstants.PN_NAME);
        String title = RegionTreeJson.str(json, RegionTreeConstants.PN_TITLE);
        if (title.isEmpty()) {
            title = name;
        }
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        props.put(RegionTreeConstants.PN_ID, id);
        props.put(RegionTreeConstants.PN_NAME, name);
        props.put(RegionTreeConstants.PN_TITLE, title);
        props.put(RegionTreeConstants.PN_REGION_ID, regionId);
        props.put(RegionTreeConstants.PN_AREA_ID, areaId);
        props.put(RegionTreeConstants.PN_DAM_PATH, RegionTreeJson.str(json, RegionTreeConstants.PN_DAM_PATH));
        props.put(RegionTreeConstants.PN_TAG_PATH, RegionTreeJson.str(json, RegionTreeConstants.PN_TAG_PATH));
        props.put(RegionTreeConstants.PN_LAST_MODIFIED, lastModified);
        props.put(RegionTreeConstants.PN_LEVEL, level);
        Resource created = resolver.create(parent, id, props);
        LOG.debug("Created {} node {} ({})", level, created.getPath(), relativePath);
        return created;
    }

    private static void createOrReplace(ResourceResolver resolver, Resource actualRoot, RegionTreeNode node)
            throws PersistenceException {
        String relative = node.getRelativePath();
        Resource parent = ensureUnstructuredParent(resolver, actualRoot, relative);
        String name = relative.contains("/") ? relative.substring(relative.lastIndexOf('/') + 1) : relative;
        Resource existing = parent.getChild(name);
        if (existing != null) {
            writeProperties(existing, node);
            return;
        }
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        fillProperties(props, node);
        resolver.create(parent, name, props);
    }

    private static void writeProperties(Resource resource, RegionTreeNode node) throws PersistenceException {
        ModifiableValueMap map = resource.adaptTo(ModifiableValueMap.class);
        if (map == null) {
            throw new PersistenceException("Cannot modify " + resource.getPath());
        }
        fillProperties(map, node);
    }

    private static void fillProperties(Map<String, Object> map, RegionTreeNode node) {
        map.put(RegionTreeConstants.PN_ID, node.getId());
        map.put(RegionTreeConstants.PN_NAME, node.getName());
        map.put(RegionTreeConstants.PN_TITLE, node.getTitle());
        map.put(RegionTreeConstants.PN_REGION_ID, node.getRegionId());
        map.put(RegionTreeConstants.PN_AREA_ID, node.getAreaId());
        map.put(RegionTreeConstants.PN_DAM_PATH, node.getDamPath());
        map.put(RegionTreeConstants.PN_TAG_PATH, node.getTagPath());
        map.put(RegionTreeConstants.PN_LAST_MODIFIED, node.getLastModified());
        map.put(RegionTreeConstants.PN_LEVEL, node.getLevel());
    }

    private static Resource ensureUnstructuredParent(ResourceResolver resolver, Resource actualRoot,
            String relativePath) throws PersistenceException {
        if (relativePath == null || !relativePath.contains("/")) {
            return actualRoot;
        }
        String parentRel = relativePath.substring(0, relativePath.lastIndexOf('/'));
        Resource current = actualRoot;
        for (String segment : parentRel.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            Resource child = current.getChild(segment);
            if (child == null) {
                Map<String, Object> props = new LinkedHashMap<>();
                props.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                props.put(RegionTreeConstants.PN_ID, segment);
                child = resolver.create(current, segment, props);
            }
            current = child;
        }
        return current;
    }

    private static int depth(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return 0;
        }
        int depth = 1;
        for (int i = 0; i < relativePath.length(); i++) {
            if (relativePath.charAt(i) == '/') {
                depth++;
            }
        }
        return depth;
    }
}
