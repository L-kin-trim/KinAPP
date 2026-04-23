package com.example.kin.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "session")
public class SessionEntity {
    @PrimaryKey
    @NonNull
    public String key = "active";
    public String token = "";
    public long userId;
    public String username = "";
    public String role = "USER";
    public long loggedInAt;
    public long updatedAt;
    public long expiresAt;
}
