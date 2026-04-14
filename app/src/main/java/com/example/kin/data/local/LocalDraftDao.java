package com.example.kin.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocalDraftDao {
    @Query("SELECT * FROM local_draft_cache WHERE cacheKey = :cacheKey LIMIT 1")
    LocalDraftEntity findByKey(String cacheKey);

    @Query("SELECT * FROM local_draft_cache ORDER BY updatedAt DESC")
    List<LocalDraftEntity> listAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long save(LocalDraftEntity entity);

    @Query("DELETE FROM local_draft_cache WHERE cacheKey = :cacheKey")
    void deleteByKey(String cacheKey);

    @Query("DELETE FROM local_draft_cache WHERE id = :id")
    void deleteById(long id);
}
