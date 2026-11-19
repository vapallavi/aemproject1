package com.adobe.aem.guides.demo.core.regiontree;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import java.util.Objects;

/**
 * Snapshot of one region, area, or resort node used for comparison and job payloads.
 * Sling jobs cannot carry arbitrary objects, so {@link #toJobPayload()} produces a String.
 */
public final class RegionTreeNode {

    private final String id;
    private final String name;
    private final String title;
    private final String regionId;
    private final String areaId;
    private final String damPath;
    private final String tagPath;
    private final String lastModified;
    private final String level;
    private final String relativePath;

    public RegionTreeNode(String id, String name, String title, String regionId, String areaId,
            String damPath, String tagPath, String lastModified, String level, String relativePath) {
        this.id = nullToEmpty(id);
        this.name = nullToEmpty(name);
        this.title = nullToEmpty(title);
        this.regionId = nullToEmpty(regionId);
        this.areaId = nullToEmpty(areaId);
        this.damPath = nullToEmpty(damPath);
        this.tagPath = nullToEmpty(tagPath);
        this.lastModified = nullToEmpty(lastModified);
        this.level = nullToEmpty(level);
        this.relativePath = nullToEmpty(relativePath);
    }

    public static RegionTreeNode from(Resource resource, String treeRootPath) {
        ValueMap map = resource.getValueMap();
        String path = resource.getPath();
        String relative = path.startsWith(treeRootPath + "/")
                ? path.substring(treeRootPath.length() + 1)
                : path;
        return new RegionTreeNode(
                map.get(RegionTreeConstants.PN_ID, ""),
                map.get(RegionTreeConstants.PN_NAME, ""),
                map.get(RegionTreeConstants.PN_TITLE, ""),
                map.get(RegionTreeConstants.PN_REGION_ID, ""),
                map.get(RegionTreeConstants.PN_AREA_ID, ""),
                map.get(RegionTreeConstants.PN_DAM_PATH, ""),
                map.get(RegionTreeConstants.PN_TAG_PATH, ""),
                map.get(RegionTreeConstants.PN_LAST_MODIFIED, ""),
                map.get(RegionTreeConstants.PN_LEVEL, inferLevel(relative)),
                relative);
    }

    public static RegionTreeNode fromJobPayload(String payload) {
        return RegionTreeJson.parseNode(payload);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getRegionId() {
        return regionId;
    }

    public String getAreaId() {
        return areaId;
    }

    public String getDamPath() {
        return damPath;
    }

    public String getTagPath() {
        return tagPath;
    }

    public String getLastModified() {
        return lastModified;
    }

    public String getLevel() {
        return level;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public boolean sameLocation(RegionTreeNode other) {
        return other != null
                && Objects.equals(regionId, other.regionId)
                && Objects.equals(areaId, other.areaId);
    }

    /**
     * Location is compared separately; lastModified is an import stamp, not an authored change.
     */
    public boolean sameAuthoredProperties(RegionTreeNode other) {
        return other != null
                && Objects.equals(title, other.title)
                && Objects.equals(name, other.name)
                && Objects.equals(damPath, other.damPath)
                && Objects.equals(tagPath, other.tagPath);
    }

    public String toJobPayload() {
        return RegionTreeJson.toJson(this);
    }

    static String inferLevel(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return "";
        }
        int segments = 1;
        for (int i = 0; i < relativePath.length(); i++) {
            if (relativePath.charAt(i) == '/') {
                segments++;
            }
        }
        if (segments == 1) {
            return RegionTreeConstants.LEVEL_REGION;
        }
        if (segments == 2) {
            return RegionTreeConstants.LEVEL_AREA;
        }
        return RegionTreeConstants.LEVEL_RESORT;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
