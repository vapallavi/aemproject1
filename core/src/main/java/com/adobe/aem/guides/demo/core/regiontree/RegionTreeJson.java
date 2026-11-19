package com.adobe.aem.guides.demo.core.regiontree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Gson helpers for import payloads and Sling job String properties.
 */
public final class RegionTreeJson {

    private RegionTreeJson() {
    }

    @SuppressWarnings("deprecation")
    public static JsonObject parseObject(String json) {
        JsonElement element = new JsonParser().parse(json);
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("JSON root must be an object");
        }
        return element.getAsJsonObject();
    }

    static JsonArray array(JsonObject object, String name) {
        if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
            return new JsonArray();
        }
        JsonElement element = object.get(name);
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException(name + " must be an array");
        }
        return element.getAsJsonArray();
    }

    static String str(JsonObject object, String name) {
        if (object == null || !object.has(name) || object.get(name).isJsonNull()) {
            return "";
        }
        JsonElement element = object.get(name);
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return element.toString();
    }

    static String toJson(RegionTreeNode node) {
        JsonObject object = new JsonObject();
        object.addProperty(RegionTreeConstants.PN_ID, node.getId());
        object.addProperty(RegionTreeConstants.PN_NAME, node.getName());
        object.addProperty(RegionTreeConstants.PN_TITLE, node.getTitle());
        object.addProperty(RegionTreeConstants.PN_REGION_ID, node.getRegionId());
        object.addProperty(RegionTreeConstants.PN_AREA_ID, node.getAreaId());
        object.addProperty(RegionTreeConstants.PN_DAM_PATH, node.getDamPath());
        object.addProperty(RegionTreeConstants.PN_TAG_PATH, node.getTagPath());
        object.addProperty(RegionTreeConstants.PN_LAST_MODIFIED, node.getLastModified());
        object.addProperty(RegionTreeConstants.PN_LEVEL, node.getLevel());
        object.addProperty("relativePath", node.getRelativePath());
        return object.toString();
    }

    static RegionTreeNode parseNode(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Empty node payload");
        }
        JsonObject object = parseObject(payload);
        String relativePath = str(object, "relativePath");
        String level = str(object, RegionTreeConstants.PN_LEVEL);
        if (level.isEmpty()) {
            level = RegionTreeNode.inferLevel(relativePath);
        }
        return new RegionTreeNode(
                str(object, RegionTreeConstants.PN_ID),
                str(object, RegionTreeConstants.PN_NAME),
                str(object, RegionTreeConstants.PN_TITLE),
                str(object, RegionTreeConstants.PN_REGION_ID),
                str(object, RegionTreeConstants.PN_AREA_ID),
                str(object, RegionTreeConstants.PN_DAM_PATH),
                str(object, RegionTreeConstants.PN_TAG_PATH),
                str(object, RegionTreeConstants.PN_LAST_MODIFIED),
                level,
                relativePath);
    }
}
