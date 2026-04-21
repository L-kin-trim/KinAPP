package com.example.kin;

import static org.junit.Assert.assertEquals;

import com.example.kin.net.OpenAiStreamClient;

import org.junit.Test;

public class OpenAiStreamClientTest {
    @Test
    public void extractDeltaContent_shouldReturnToken() {
        String line = "data: {\"choices\":[{\"delta\":{\"content\":\"hello\"}}]}";
        assertEquals("hello", OpenAiStreamClient.extractDeltaContent(line));
    }

    @Test
    public void extractDeltaContent_shouldIgnoreDoneAndInvalid() {
        assertEquals("", OpenAiStreamClient.extractDeltaContent("data: [DONE]"));
        assertEquals("", OpenAiStreamClient.extractDeltaContent("event: ping"));
        assertEquals("", OpenAiStreamClient.extractDeltaContent("data: {invalid-json"));
    }
}
