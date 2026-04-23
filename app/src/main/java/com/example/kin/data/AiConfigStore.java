package com.example.kin.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.kin.model.AiConfig;

public class AiConfigStore {
    private static final String FILE_NAME = "ai_config_secure";
    private static final String KEY_PROVIDER_ID = "provider_id";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL = "model";
    private static final String KEY_SYSTEM_PROMPT = "system_prompt";

    private final SharedPreferences preferences;

    public AiConfigStore(Context context) {
        this.preferences = createPreferences(context.getApplicationContext());
    }

    public AiConfig load() {
        AiConfig config = new AiConfig();
        config.providerId = preferences.getString(KEY_PROVIDER_ID, "");
        config.baseUrl = preferences.getString(KEY_BASE_URL, "");
        config.apiKey = preferences.getString(KEY_API_KEY, "");
        config.model = preferences.getString(KEY_MODEL, "");
        config.systemPrompt = preferences.getString(KEY_SYSTEM_PROMPT, "");
        return config;
    }

    public void save(AiConfig config) {
        preferences.edit()
                .putString(KEY_PROVIDER_ID, safe(config.providerId))
                .putString(KEY_BASE_URL, safe(config.baseUrl))
                .putString(KEY_API_KEY, safe(config.apiKey))
                .putString(KEY_MODEL, safe(config.model))
                .putString(KEY_SYSTEM_PROMPT, safe(config.systemPrompt))
                .apply();
    }

    private SharedPreferences createPreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception ignored) {
            return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
