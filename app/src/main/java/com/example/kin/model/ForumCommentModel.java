package com.example.kin.model;

import java.util.ArrayList;
import java.util.List;

public class ForumCommentModel {
    public long id;
    public long postId;
    public long parentCommentId;
    public int floorNumber;
    public int replyCount;
    public String content;
    public String username;
    public String replyToUsername;
    public String createdAt;
    public String reviewStatus;
    public String reviewRemark;
    public final List<String> imageUrls = new ArrayList<>();
    public final List<String> mentionUsernames = new ArrayList<>();
}
