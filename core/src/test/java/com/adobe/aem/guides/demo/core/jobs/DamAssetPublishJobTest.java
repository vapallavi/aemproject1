package com.adobe.aem.guides.demo.core.jobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DamAssetPublishJobTest {

    @Test
    void toAssetPathStripsJcrContentDescendants() {
        assertEquals("/content/dam/demo/logo.jpeg",
                DamAssetPublishJob.toAssetPath(
                        "/content/dam/demo/logo.jpeg/jcr:content/renditions/cq5dam.web.1280.1280.jpeg"));
        assertEquals("/content/dam/demo/logo.jpeg",
                DamAssetPublishJob.toAssetPath("/content/dam/demo/logo.jpeg/jcr:content/metadata"));
        assertEquals("/content/dam/demo/logo.jpeg",
                DamAssetPublishJob.toAssetPath("/content/dam/demo/logo.jpeg"));
        assertNull(DamAssetPublishJob.toAssetPath(null));
    }
}
