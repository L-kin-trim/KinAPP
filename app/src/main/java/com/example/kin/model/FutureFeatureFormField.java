package com.example.kin.model;

public class FutureFeatureFormField {
    public final String key;
    public final String label;
    public final String hint;
    public final boolean multiline;
    public final String defaultValue;

    public FutureFeatureFormField(String key, String label, String hint, boolean multiline, String defaultValue) {
        this.key = key;
        this.label = label;
        this.hint = hint;
        this.multiline = multiline;
        this.defaultValue = defaultValue == null ? "" : defaultValue;
    }
}
