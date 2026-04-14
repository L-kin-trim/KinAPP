package com.example.kin.data;

import android.content.Context;
import android.text.TextUtils;

import com.example.kin.data.local.KinDatabase;
import com.example.kin.data.local.LocalDraftDao;
import com.example.kin.data.local.LocalDraftEntity;
import com.example.kin.model.DraftModel;

import java.util.ArrayList;
import java.util.List;

public class LocalDraftStore {
    private final LocalDraftDao localDraftDao;

    public LocalDraftStore(Context context) {
        localDraftDao = KinDatabase.getInstance(context).localDraftDao();
    }

    public DraftModel get(String cacheKey) {
        return toModel(localDraftDao.findByKey(cacheKey));
    }

    public List<DraftModel> listAll() {
        List<DraftModel> result = new ArrayList<>();
        for (LocalDraftEntity entity : localDraftDao.listAll()) {
            result.add(toModel(entity));
        }
        return result;
    }

    public void save(String cacheKey, String title, String payloadJson, long remoteDraftId) {
        if (TextUtils.isEmpty(cacheKey)) {
            return;
        }
        LocalDraftEntity entity = localDraftDao.findByKey(cacheKey);
        if (entity == null) {
            entity = new LocalDraftEntity();
            entity.cacheKey = cacheKey;
        }
        entity.title = title == null ? "" : title;
        entity.payloadJson = payloadJson == null ? "" : payloadJson;
        entity.remoteDraftId = remoteDraftId;
        entity.updatedAt = System.currentTimeMillis();
        localDraftDao.save(entity);
    }

    public void delete(String cacheKey) {
        localDraftDao.deleteByKey(cacheKey);
    }

    public void delete(long id) {
        localDraftDao.deleteById(id);
    }

    private DraftModel toModel(LocalDraftEntity entity) {
        if (entity == null) {
            return null;
        }
        DraftModel model = new DraftModel();
        model.id = entity.id;
        model.remoteDraftId = entity.remoteDraftId;
        model.draftType = entity.cacheKey;
        model.title = entity.title;
        model.payloadJson = entity.payloadJson;
        model.autoSaved = true;
        model.updatedAt = String.valueOf(entity.updatedAt);
        return model;
    }
}
