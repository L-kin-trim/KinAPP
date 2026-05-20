package com.example.kin.model;

import java.util.ArrayList;
import java.util.List;

public class FutureFeatureDefinition {
    public final String key;
    public final String groupKey;
    public final String section;
    public final String title;
    public final String summary;
    public final String apiFeatureKey;
    public final String apiPrefix;
    public final boolean aiEnabled;
    public final boolean taskEnabled;
    public final List<FutureFeatureFormField> fields = new ArrayList<>();

    public FutureFeatureDefinition(String key,
                                   String groupKey,
                                   String section,
                                   String title,
                                   String summary,
                                   boolean aiEnabled,
                                   boolean taskEnabled) {
        this(key, groupKey, section, title, summary, key, "", aiEnabled, taskEnabled);
    }

    public FutureFeatureDefinition(String key,
                                   String groupKey,
                                   String section,
                                   String title,
                                   String summary,
                                   String apiFeatureKey,
                                   String apiPrefix,
                                   boolean aiEnabled,
                                   boolean taskEnabled) {
        this.key = key;
        this.groupKey = groupKey;
        this.section = section;
        this.title = title;
        this.summary = summary;
        this.apiFeatureKey = apiFeatureKey;
        this.apiPrefix = apiPrefix;
        this.aiEnabled = aiEnabled;
        this.taskEnabled = taskEnabled;
    }

    public FutureFeatureDefinition withField(String key, String label, String hint, boolean multiline, String defaultValue) {
        fields.add(new FutureFeatureFormField(key, label, hint, multiline, defaultValue));
        return this;
    }
}
