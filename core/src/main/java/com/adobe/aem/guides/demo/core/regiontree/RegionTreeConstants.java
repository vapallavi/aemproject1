package com.adobe.aem.guides.demo.core.regiontree;

/**
 * Shared paths, job topics, and property names for region-tree sync.
 * Job topics must use {@code /} segments so Sling JobManager can register them.
 */
public final class RegionTreeConstants {

    public static final String SUBSERVICE = "region-tree-service";

    public static final String ROOT_PATH = "/conf/region-tree";
    public static final String TEMP_PATH = ROOT_PATH + "/temp";
    public static final String ACTUAL_PATH = ROOT_PATH + "/actual";
    public static final String DATA_PATH = ROOT_PATH + "/data";

    public static final String JOB_TOPIC_TEMP = "com/jet2/region-tree/temp";
    public static final String JOB_TOPIC_UPDATE = "com/jet2/region-tree/update";

    public static final String PN_ID = "id";
    public static final String PN_NAME = "name";
    public static final String PN_TITLE = "title";
    public static final String PN_REGION_ID = "regionId";
    public static final String PN_AREA_ID = "areaId";
    public static final String PN_DAM_PATH = "damPath";
    public static final String PN_TAG_PATH = "tagPath";
    public static final String PN_LAST_MODIFIED = "lastModified";
    public static final String PN_LEVEL = "level";

    public static final String PN_REGION_TREE_JSON = "regionTreeJson";
    public static final String PN_START_DATE_IMPORT = "startDateImport";
    public static final String PN_END_DATE_IMPORT = "endDateImport";

    public static final String JOB_CREATED = "createdNodes";
    public static final String JOB_UPDATED = "updatedNodes";
    public static final String JOB_MOVED = "movedNodes";

    public static final String LEVEL_REGION = "region";
    public static final String LEVEL_AREA = "area";
    public static final String LEVEL_RESORT = "resort";

    public static final String MSG_SUCCESS = "The temp folder is created correctly!!";
    public static final String MSG_FAILURE = "Error during the creation of the temp folder!!!";

    private RegionTreeConstants() {
    }
}
