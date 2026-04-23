package com.example.kin.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SessionEntity.class, LocalDraftEntity.class}, version = 2, exportSchema = false)
public abstract class KinDatabase extends RoomDatabase {
    private static volatile KinDatabase instance;

    public abstract SessionDao sessionDao();

    public abstract LocalDraftDao localDraftDao();

    public static KinDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (KinDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    KinDatabase.class,
                                    "kin_local.db")
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
