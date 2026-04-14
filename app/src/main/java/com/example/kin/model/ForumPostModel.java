package com.example.kin.model;

import java.util.ArrayList;
import java.util.List;

public class ForumPostModel {
    public long id;
    public String postType;
    public String title;
    public String mapName;
    public String createdByUsername;
    public String reviewStatus;
    public String reviewRemark;
    public String createdAt;
    public String updatedAt;
    public String approvedAt;
    public String withdrawnAt;
    public String editableUntil;
    public String content;
    public String propName;
    public String toolType;
    public String throwMethod;
    public String propPosition;
    public String stanceImageUrl;
    public String aimImageUrl;
    public String landingImageUrl;
    public String tacticName;
    public String tacticType;
    public String tacticDescription;
    public String member1;
    public String member1Role;
    public String member2;
    public String member2Role;
    public String member3;
    public String member3Role;
    public String member4;
    public String member4Role;
    public String member5;
    public String member5Role;
    public int version;
    public int likeCount;
    public int favoriteCount;
    public boolean liked;
    public boolean canEdit;
    public boolean canWithdraw;
    public final List<String> imageUrls = new ArrayList<>();
    public final List<String> tags = new ArrayList<>();
}
