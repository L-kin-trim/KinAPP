package com.example.kin.data;

import android.content.Context;
import android.text.TextUtils;

import com.example.kin.data.local.KinDatabase;
import com.example.kin.data.local.SessionDao;
import com.example.kin.data.local.SessionEntity;
import com.example.kin.model.SessionUser;

public class SessionManager {
    private static final String DEFAULT_BASE_URL = "http://47.105.102.113:9126";

    private final SessionDao sessionDao;

    public SessionManager(Context context) {
        sessionDao = KinDatabase.getInstance(context).sessionDao();
    }

    public String getBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    public String getToken() {
        SessionEntity entity = sessionDao.getActive();
        return entity == null ? "" : entity.token;
    }

    public boolean isLoggedIn() {
        return !TextUtils.isEmpty(getToken());
    }

    public SessionUser getUser() {
        SessionEntity entity = sessionDao.getActive();
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
        entity.token = token;
        entity.userId = user.id;
        entity.username = user.username;
        entity.role = TextUtils.isEmpty(user.role) ? "USER" : user.role;
        entity.loggedInAt = user.loggedInAt > 0 ? user.loggedInAt : now;
        entity.updatedAt = now;
        sessionDao.save(entity);
    }

    public void clearSession() {
        sessionDao.clear();
    }
}
