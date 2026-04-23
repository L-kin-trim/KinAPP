package com.example.kin.model;

import java.util.ArrayList;
import java.util.List;

public class AiProviderPreset {
    public String id;
    public String label;
    public String baseUrl;
    public String model;
    public String note;

    public static List<AiProviderPreset> defaults() {
        List<AiProviderPreset> presets = new ArrayList<>();
        presets.add(create(
                "tongyi",
                "\u901a\u4e49\u5343\u95ee",
                "https://dashscope.aliyuncs.com/compatible-mode",
                "qwen-plus",
                "\u963f\u91cc\u767e\u70bc OpenAI \u517c\u5bb9\u8def\u7531"));
        presets.add(create(
                "openai",
                "OpenAI",
                "https://api.openai.com",
                "gpt-4o-mini",
                "OpenAI \u5b98\u65b9\u63a5\u53e3"));
        presets.add(create(
                "claude",
                "Claude (\u517c\u5bb9\u8def\u7531)",
                "https://openrouter.ai/api",
                "anthropic/claude-3.7-sonnet",
                "\u901a\u8fc7 OpenAI \u517c\u5bb9\u7f51\u5173\u8c03\u7528 Claude"));
        presets.add(create(
                "doubao",
                "\u8c46\u5305",
                "https://ark.cn-beijing.volces.com/api",
                "doubao-seed-1-6-250615",
                "\u706b\u5c71\u5f15\u64ce Ark OpenAI \u517c\u5bb9\u8def\u7531"));
        presets.add(create(
                "deepseek",
                "DeepSeek",
                "https://api.deepseek.com",
                "deepseek-chat",
                "DeepSeek \u5b98\u65b9 OpenAI \u517c\u5bb9\u63a5\u53e3"));
        return presets;
    }

    public static AiProviderPreset findById(List<AiProviderPreset> presets, String id) {
        if (presets == null || id == null) {
            return null;
        }
        for (AiProviderPreset preset : presets) {
            if (id.equals(preset.id)) {
                return preset;
            }
        }
        return null;
    }

    private static AiProviderPreset create(String id, String label, String baseUrl, String model, String note) {
        AiProviderPreset preset = new AiProviderPreset();
        preset.id = id;
        preset.label = label;
        preset.baseUrl = baseUrl;
        preset.model = model;
        preset.note = note;
        return preset;
    }
}
