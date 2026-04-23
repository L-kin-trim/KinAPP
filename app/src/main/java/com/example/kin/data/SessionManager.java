package com.example.kin.data;

import android.content.Context;
import android.text.TextUtils;

import com.example.kin.data.local.KinDatabase;
import com.example.kin.data.local.SessionDao;
import com.example.kin.data.local.SessionEntity;
import com.example.kin.model.SessionUser;

public class SessionManager {
    private static final String DEFAULT_BASE_URL = "http://47.105.102.113:9126";
    public static final long SESSION_TTL_MS = 30L * 24L * 60L * 60L * 1000L;

    private final SessionDao sessionDao;

    public SessionManager(Context context) {
        sessionDao = KinDatabase.getInstance(context).sessionDao();
    }

    public String getBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    public String getToken() {
        SessionEntity entity = getValidSession();
        return entity == null ? "" : entity.token;
    }

    public boolean isLoggedIn() {
        return getValidSession() != null;
    }

    public SessionUser getUser() {
        SessionEntity entity = getValidSession();
        SessionUser user = new SessionUser();
        if (entity == null) {
            return user;
        }
        user.id = entity.userId;
        user.username = entity.username;
        user.role = entity.role;
        user.loggedInAt = entity.loggedInAt;
        user.updatedAt = entity.updatedAt;
        return user;
    }

    public void saveSession(String token, SessionUser user) {
        long now = System.currentTimeMillis();
        SessionEntity entity = new SessionEntity();
        entity.token = safe(token);
        entity.userId = user.id;
        entity.username = safe(user.username);
        entity.role = TextUtils.isEmpty(user.role) ? "USER" : user.role;
        entity.loggedInAt = user.loggedInAt > 0 ? user.loggedInAt : now;
        entity.updatedAt = now;
        entity.expiresAt = now + SESSION_TTL_MS;
        sessionDao.save(entity);
    }

    public long buildSessionExpiryFromNow() {
        return System.currentTimeMillis() + SESSION_TTL_MS;
    }

    public void clearSession() {
        sessionDao.clear();
    }

    private SessionEntity getValidSession() {
        SessionEntity entity = sessionDao.getActive();
        if (entity == null || TextUtils.isEmpty(entity.token)) {
            return null;
        }
        if (entity.expiresAt <= System.currentTimeMillis()) {
            sessionDao.clear();
            return null;
        }
        return entity;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
