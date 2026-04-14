package com.example.kin.model;

import java.util.ArrayList;
import java.util.List;

public class UserProfileModel {
    public String username;
    public int postCount;
    public int approvedPostCount;
    public double approvalRate;
    public int favoriteReceivedCount;
    public int likeReceivedCount;
    public int commentReceivedCount;
    public int activeStreakDays;
    public final List<String> badges = new ArrayList<>();
}
