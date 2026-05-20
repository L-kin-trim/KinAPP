package com.example.kin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.kin.net.ApiException;

import org.junit.Test;

public class FutureFeatureErrorContractTest {
    @Test
    public void isFeatureUnavailable_shouldTreatSkeletonMissingEndpointsAsUnavailable() {
        assertTrue(new ApiException(404, "missing").isFeatureUnavailable());
        assertTrue(new ApiException(405, "method").isFeatureUnavailable());
        assertTrue(new ApiException(501, "todo").isFeatureUnavailable());
        assertFalse(new ApiException(429, "rate limited").isFeatureUnavailable());
    }
}
