package com.adobe.aem.guides.demo.core.regiontree;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionTreeUtilsTest {

    @Test
    void createdWhenIdIsOnlyInTemp() {
        Map<String, RegionTreeNode> temp = new LinkedHashMap<>();
        Map<String, RegionTreeNode> actual = new LinkedHashMap<>();
        temp.put("180", node("180", "Salzburg City", "1", "49", "1/49/180"));

        RegionTreeComparison comparison = RegionTreeUtils.compare(temp, actual);

        assertEquals(1, comparison.getCreated().size());
        assertEquals("180", comparison.getCreated().get(0).getId());
        assertTrue(comparison.getUpdated().isEmpty());
        assertTrue(comparison.getMoved().isEmpty());
    }

    @Test
    void updatedWhenTitleChangesAtSameLocation() {
        Map<String, RegionTreeNode> temp = new LinkedHashMap<>();
        Map<String, RegionTreeNode> actual = new LinkedHashMap<>();
        temp.put("49", node("49", "Salzburg City", "1", "49", "1/49"));
        actual.put("49", node("49", "Salzburg", "1", "49", "1/49"));

        RegionTreeComparison comparison = RegionTreeUtils.compare(temp, actual);

        assertEquals(1, comparison.getUpdated().size());
        assertEquals("Salzburg City", comparison.getUpdated().get(0).getTitle());
        assertTrue(comparison.getCreated().isEmpty());
        assertTrue(comparison.getMoved().isEmpty());
    }

    @Test
    void movedWhenAreaIdChanges() {
        Map<String, RegionTreeNode> temp = new LinkedHashMap<>();
        Map<String, RegionTreeNode> actual = new LinkedHashMap<>();
        temp.put("180", node("180", "Salzburg City", "1", "50", "1/50/180"));
        actual.put("180", node("180", "Salzburg City", "1", "49", "1/49/180"));

        RegionTreeComparison comparison = RegionTreeUtils.compare(temp, actual);

        assertEquals(1, comparison.getMoved().size());
        assertEquals("50", comparison.getMoved().get(0).getAreaId());
        assertTrue(comparison.getCreated().isEmpty());
        assertTrue(comparison.getUpdated().isEmpty());
    }

    @Test
    void movedWinsWhenLocationAndTitleBothChange() {
        Map<String, RegionTreeNode> temp = new LinkedHashMap<>();
        Map<String, RegionTreeNode> actual = new LinkedHashMap<>();
        temp.put("180", node("180", "New Title", "1", "50", "1/50/180"));
        actual.put("180", node("180", "Old Title", "1", "49", "1/49/180"));

        RegionTreeComparison comparison = RegionTreeUtils.compare(temp, actual);

        assertEquals(1, comparison.getMoved().size());
        assertTrue(comparison.getUpdated().isEmpty());
    }

    @Test
    void jobPayloadRoundTrip() {
        RegionTreeNode original = node("180", "Salzburg City", "1", "49", "1/49/180");
        RegionTreeNode parsed = RegionTreeNode.fromJobPayload(original.toJobPayload());
        assertEquals(original.getId(), parsed.getId());
        assertEquals(original.getTitle(), parsed.getTitle());
        assertEquals(original.getRegionId(), parsed.getRegionId());
        assertEquals(original.getAreaId(), parsed.getAreaId());
        assertEquals(original.getRelativePath(), parsed.getRelativePath());
    }

    private static RegionTreeNode node(String id, String title, String regionId, String areaId, String relativePath) {
        return new RegionTreeNode(id, title, title, regionId, areaId, "", "", "2026-01-01T00:00:00Z",
                RegionTreeNode.inferLevel(relativePath), relativePath);
    }
}
