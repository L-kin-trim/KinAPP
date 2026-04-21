package com.example.kin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.kin.model.AiConfig;

import org.junit.Test;

public class AiConfigTest {
    @Test
    public void isValid_shouldRequireBaseUrlApiKeyAndModel() {
        AiConfig config = new AiConfig();
        assertFalse(config.isValid());
        config.baseUrl = "https://api.example.com";
        config.apiKey = "sk-test";
        config.model = "gpt-test";
        assertTrue(config.isValid());
    }
}
