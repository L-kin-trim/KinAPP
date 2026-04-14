package com.example.kin.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_draft_cache")
public class LocalDraftEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String cacheKey = "";
    public String title = "";
    public String payloadJson = "";
    public long remoteDraftId;
    public long updatedAt;
}
