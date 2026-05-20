package com.example.kin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.kin.model.FutureFeatureDefinition;
import com.example.kin.model.FutureFeatureRegistry;

import org.junit.Test;

public class FutureFeatureRegistryTest {
    @Test
    public void registry_shouldCoverFuturePlanSectionsTwoThroughFifteen() {
        assertEquals(14, FutureFeatureRegistry.groups().size());
        assertTrue(FutureFeatureRegistry.allFeatures().size() >= 131);
        assertNotNull(FutureFeatureRegistry.featureByKey("content.markdown_editor"));
        assertNotNull(FutureFeatureRegistry.featureByKey("cs2.interactive_map_points"));
        assertNotNull(FutureFeatureRegistry.featureByKey("platform.refresh_token"));
    }

    @Test
    public void feature_shouldExposeStableJsonFields() {
        FutureFeatureDefinition feature = FutureFeatureRegistry.featureByKey("ai.utility_recommendation");
        assertNotNull(feature);
        assertEquals("ai", feature.groupKey);
        assertTrue(feature.aiEnabled);
        assertTrue(feature.fields.size() >= 4);
    }
}
