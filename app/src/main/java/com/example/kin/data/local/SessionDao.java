package com.example.kin.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface SessionDao {
    @Query("SELECT * FROM session WHERE `key` = 'active' LIMIT 1")
    SessionEntity getActive();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(SessionEntity entity);

    @Query("DELETE FROM session")
    void clear();
}
