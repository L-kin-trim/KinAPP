package com.example.kin.model;

public class SessionUser {
    public long id;
    public String username;
    public String role;
    public long loggedInAt;
    public long updatedAt;

    public boolean isAdmin() {
        return role != null && role.toUpperCase().contains("ADMIN");
    }
}
