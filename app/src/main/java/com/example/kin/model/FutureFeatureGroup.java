package com.example.kin.model;

import java.util.ArrayList;
import java.util.List;

public class FutureFeatureGroup {
    public final String key;
    public final String title;
    public final String summary;
    public final String apiPrefix;
    public final List<FutureFeatureDefinition> features = new ArrayList<>();

    public FutureFeatureGroup(String key, String title, String summary, String apiPrefix) {
        this.key = key;
        this.title = title;
        this.summary = summary;
        this.apiPrefix = apiPrefix;
    }
}
