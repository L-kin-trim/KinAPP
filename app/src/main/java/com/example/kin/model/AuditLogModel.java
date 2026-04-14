package com.example.kin.model;

public class AuditLogModel {
    public long id;
    public String adminUsername;
    public String actionType;
    public String targetType;
    public String targetId;
    public String detail;
    public String createdAt;
}
