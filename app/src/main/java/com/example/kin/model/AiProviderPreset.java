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
                "Tongyi (Qwen)",
                "https://dashscope.aliyuncs.com/compatible-mode",
                "qwen-plus",
                "Alibaba Bailian OpenAI-compatible route"));
        presets.add(create(
                "openai",
                "OpenAI",
                "https://api.openai.com",
                "gpt-4o-mini",
                "Official OpenAI API"));
        presets.add(create(
                "claude",
                "Claude (compatible)",
                "https://openrouter.ai/api",
                "anthropic/claude-3.7-sonnet",
                "Claude via OpenAI-compatible gateway"));
        presets.add(create(
                "doubao",
                "Doubao",
                "https://ark.cn-beijing.volces.com/api",
                "doubao-seed-1-6-250615",
                "Volcengine Ark OpenAI-compatible route"));
        presets.add(create(
                "deepseek",
                "DeepSeek",
                "https://api.deepseek.com",
                "deepseek-chat",
                "Official DeepSeek OpenAI-compatible endpoint"));
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
