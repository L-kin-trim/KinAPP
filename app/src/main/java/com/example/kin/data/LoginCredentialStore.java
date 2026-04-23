package com.example.kin.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class LoginCredentialStore {
    private static final String FILE_NAME = "session_credential_secure";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_EXPIRES_AT = "expires_at";

    private final SharedPreferences preferences;

    public LoginCredentialStore(Context context) {
        this.preferences = createPreferences(context.getApplicationContext());
    }

    public void save(String username, String password, long expiresAt) {
        preferences.edit()
                .putString(KEY_USERNAME, safe(username))
                .putString(KEY_PASSWORD, safe(password))
                .putLong(KEY_EXPIRES_AT, expiresAt)
                .apply();
    }

    public Credentials load() {
        Credentials credentials = new Credentials();
        credentials.username = preferences.getString(KEY_USERNAME, "");
        credentials.password = preferences.getString(KEY_PASSWORD, "");
        credentials.expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L);
        if (!credentials.isValid()) {
            return null;
        }
        return credentials;
    }

    public void clear() {
        preferences.edit().clear().apply();
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

    public static class Credentials {
        public String username = "";
        public String password = "";
        public long expiresAt;

        public boolean isValid() {
            return !isEmpty(username)
                    && !isEmpty(password)
                    && expiresAt > System.currentTimeMillis();
        }

        private boolean isEmpty(String value) {
            return value == null || value.trim().isEmpty();
        }
    }
}
