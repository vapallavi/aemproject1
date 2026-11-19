package com.adobe.aem.guides.demo.core.regiontree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of comparing {@code /conf/region-tree/temp} against {@code /conf/region-tree/actual}.
 */
public final class RegionTreeComparison {

    private final List<RegionTreeNode> created;
    private final List<RegionTreeNode> updated;
    private final List<RegionTreeNode> moved;

    public RegionTreeComparison(List<RegionTreeNode> created, List<RegionTreeNode> updated,
            List<RegionTreeNode> moved) {
        this.created = Collections.unmodifiableList(new ArrayList<>(created));
        this.updated = Collections.unmodifiableList(new ArrayList<>(updated));
        this.moved = Collections.unmodifiableList(new ArrayList<>(moved));
    }

    public List<RegionTreeNode> getCreated() {
        return created;
    }

    public List<RegionTreeNode> getUpdated() {
        return updated;
    }

    public List<RegionTreeNode> getMoved() {
        return moved;
    }

    public String[] createdPayloads() {
        return toPayloads(created);
    }

    public String[] updatedPayloads() {
        return toPayloads(updated);
    }

    public String[] movedPayloads() {
        return toPayloads(moved);
    }

    private static String[] toPayloads(List<RegionTreeNode> nodes) {
        String[] payloads = new String[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            payloads[i] = nodes.get(i).toJobPayload();
        }
        return payloads;
    }
}
