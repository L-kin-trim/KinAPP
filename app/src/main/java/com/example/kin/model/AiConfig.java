package com.example.kin.model;

public class AiConfig {
    public String baseUrl = "";
    public String apiKey = "";
    public String model = "";
    public String systemPrompt = "";

    public boolean isValid() {
        return !isEmpty(baseUrl) && !isEmpty(apiKey) && !isEmpty(model);
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
