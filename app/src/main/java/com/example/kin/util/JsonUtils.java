package com.example.kin.util;

import android.text.TextUtils;

import com.example.kin.model.FavoriteStatus;
import com.example.kin.model.DraftModel;
import com.example.kin.model.ForumCommentModel;
import com.example.kin.model.ForumPostModel;
import com.example.kin.model.HotKeywordModel;
import com.example.kin.model.ImageUploadItem;
import com.example.kin.model.LikeStatusModel;
import com.example.kin.model.LibraryItem;
import com.example.kin.model.MessageBoardEntryModel;
import com.example.kin.model.MessageUnreadSummaryModel;
import com.example.kin.model.PageResult;
import com.example.kin.model.ReportModel;
import com.example.kin.model.ReviewTemplateModel;
import com.example.kin.model.AuditLogModel;
import com.example.kin.model.SearchSuggestionModel;
import com.example.kin.model.SessionUser;
import com.example.kin.model.StationMessageModel;
import com.example.kin.model.UserProfileModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class JsonUtils {
    private JsonUtils() {
    }

    public interface ItemParser<T> {
        T parse(JSONObject json);
    }

    public static String optString(JSONObject json, String key) {
        return json == null ? "" : json.optString(key, "");
    }

    public static long optLong(JSONObject json, String key) {
        return json == null ? 0L : json.optLong(key, 0L);
    }

    public static int optInt(JSONObject json, String key) {
        return json == null ? 0 : json.optInt(key, 0);
    }

    public static List<String> optStringList(JSONObject json, String key) {
        List<String> items = new ArrayList<>();
        if (json == null) {
            return items;
        }
        JSONArray array = json.optJSONArray(key);
        if (array == null) {
            return items;
        }
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i);
            if (!TextUtils.isEmpty(value)) {
                items.add(value);
            }
        }
        return items;
    }

    public static JSONArray optArray(JSONObject json, String key) {
        return json == null ? null : json.optJSONArray(key);
    }

    public static <T> PageResult<T> parsePage(JSONObject json, ItemParser<T> parser) {
        PageResult<T> result = new PageResult<>();
        result.page = optInt(json, "page");
        result.size = optInt(json, "size");
        result.total = optLong(json, "total");
        result.totalPages = optInt(json, "totalPages");
        JSONArray items = json.optJSONArray("items");
        if (items == null) {
            return result;
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item != null) {
                result.items.add(parser.parse(item));
            }
        }
        return result;
    }

    public static SessionUser parseSessionUser(JSONObject json) {
        SessionUser user = new SessionUser();
        user.id = optLong(json, "id");
        user.username = optString(json, "username");
        user.role = optString(json, "role");
        user.loggedInAt = optLong(json, "loggedInAt");
        user.updatedAt = optLong(json, "updatedAt");
        return user;
    }

    public static ForumPostModel parsePost(JSONObject json) {
        ForumPostModel post = new ForumPostModel();
        post.id = optLong(json, "id");
        post.postType = optString(json, "postType");
        post.title = firstNonEmpty(optString(json, "title"), optString(json, "propName"), optString(json, "tacticName"), shorten(optString(json, "content")));
        post.mapName = optString(json, "mapName");
        post.createdByUsername = optString(json, "createdByUsername");
        post.reviewStatus = optString(json, "reviewStatus");
        post.reviewRemark = optString(json, "reviewRemark");
        post.createdAt = optString(json, "createdAt");
        post.updatedAt = optString(json, "updatedAt");
        post.approvedAt = optString(json, "approvedAt");
        post.withdrawnAt = optString(json, "withdrawnAt");
        post.editableUntil = optString(json, "editableUntil");
        post.content = optString(json, "content");
        post.propName = optString(json, "propName");
        post.toolType = optString(json, "toolType");
        post.throwMethod = optString(json, "throwMethod");
        post.propPosition = optString(json, "propPosition");
        post.stanceImageUrl = optString(json, "stanceImageUrl");
        post.aimImageUrl = optString(json, "aimImageUrl");
        post.landingImageUrl = optString(json, "landingImageUrl");
        post.tacticName = optString(json, "tacticName");
        post.tacticType = optString(json, "tacticType");
        post.tacticDescription = optString(json, "tacticDescription");
        post.member1 = optString(json, "member1");
        post.member1Role = optString(json, "member1Role");
        post.member2 = optString(json, "member2");
        post.member2Role = optString(json, "member2Role");
        post.member3 = optString(json, "member3");
        post.member3Role = optString(json, "member3Role");
        post.member4 = optString(json, "member4");
        post.member4Role = optString(json, "member4Role");
        post.member5 = optString(json, "member5");
        post.member5Role = optString(json, "member5Role");
        post.version = optInt(json, "version");
        post.likeCount = optInt(json, "likeCount");
        post.favoriteCount = optInt(json, "favoriteCount");
        post.liked = json.optBoolean("liked", false);
        post.canEdit = json.optBoolean("canEdit", false);
        post.canWithdraw = json.optBoolean("canWithdraw", false);
        post.imageUrls.addAll(optStringList(json, "imageUrls"));
        post.tags.addAll(optStringList(json, "tags"));
        return post;
    }

    public static LibraryItem parseLibraryItem(JSONObject json) {
        LibraryItem item = new LibraryItem();
        ForumPostModel base = parsePost(json);
        item.id = base.id;
        item.postType = base.postType;
        item.title = base.title;
        item.mapName = base.mapName;
        item.createdByUsername = base.createdByUsername;
        item.reviewStatus = base.reviewStatus;
        item.reviewRemark = base.reviewRemark;
        item.createdAt = base.createdAt;
        item.updatedAt = base.updatedAt;
        item.content = base.content;
        item.propName = base.propName;
        item.toolType = base.toolType;
        item.throwMethod = base.throwMethod;
        item.propPosition = base.propPosition;
        item.stanceImageUrl = base.stanceImageUrl;
        item.aimImageUrl = base.aimImageUrl;
        item.landingImageUrl = base.landingImageUrl;
        item.tacticName = base.tacticName;
        item.tacticType = base.tacticType;
        item.tacticDescription = base.tacticDescription;
        item.member1 = base.member1;
        item.member1Role = base.member1Role;
        item.member2 = base.member2;
        item.member2Role = base.member2Role;
        item.member3 = base.member3;
        item.member3Role = base.member3Role;
        item.member4 = base.member4;
        item.member4Role = base.member4Role;
        item.member5 = base.member5;
        item.member5Role = base.member5Role;
        item.imageUrls.addAll(base.imageUrls);
        item.sourceType = optString(json, "sourceType");
        item.forumPostId = optLong(json, "forumPostId");
        return item;
    }

    public static ForumCommentModel parseComment(JSONObject json) {
        ForumCommentModel comment = new ForumCommentModel();
        comment.id = optLong(json, "id");
        comment.postId = optLong(json, "postId");
        comment.parentCommentId = optLong(json, "parentCommentId");
        comment.floorNumber = optInt(json, "floorNumber");
        comment.replyCount = optInt(json, "replyCount");
        comment.content = optString(json, "content");
        comment.username = optString(json, "username");
        comment.replyToUsername = optString(json, "replyToUsername");
        comment.createdAt = optString(json, "createdAt");
        comment.reviewStatus = optString(json, "reviewStatus");
        comment.reviewRemark = optString(json, "reviewRemark");
        comment.imageUrls.addAll(optStringList(json, "imageUrls"));
        comment.mentionUsernames.addAll(optStringList(json, "mentionUsernames"));
        return comment;
    }

    public static FavoriteStatus parseFavoriteStatus(JSONObject json) {
        FavoriteStatus favoriteStatus = new FavoriteStatus();
        favoriteStatus.id = optLong(json, "id");
        favoriteStatus.forumPostId = optLong(json, "forumPostId");
        favoriteStatus.favorited = json.optBoolean("favorited", false);
        favoriteStatus.updatedAt = optString(json, "updatedAt");
        favoriteStatus.deletedAt = optString(json, "deletedAt");
        return favoriteStatus;
    }

    public static ImageUploadItem parseUploadItem(JSONObject json) {
        ImageUploadItem item = new ImageUploadItem();
        item.fileName = optString(json, "fileName");
        item.objectKey = optString(json, "objectKey");
        item.url = optString(json, "url");
        item.contentType = optString(json, "contentType");
        item.size = optLong(json, "size");
        return item;
    }

    public static StationMessageModel parseStationMessage(JSONObject json) {
        StationMessageModel message = new StationMessageModel();
        message.id = optLong(json, "id");
        message.senderUsername = optString(json, "senderUsername");
        message.recipientUsername = optString(json, "recipientUsername");
        message.content = optString(json, "content");
        message.messageType = optString(json, "messageType");
        message.sentAt = optString(json, "sentAt");
        message.readAt = optString(json, "readAt");
        message.read = json.optBoolean("read", json.optBoolean("isRead", false));
        return message;
    }

    public static DraftModel parseDraft(JSONObject json) {
        DraftModel draft = new DraftModel();
        draft.id = optLong(json, "id");
        draft.draftType = optString(json, "draftType");
        draft.title = optString(json, "title");
        draft.payloadJson = optString(json, "payloadJson");
        draft.autoSaved = json.optBoolean("autoSaved", false);
        draft.updatedAt = optString(json, "updatedAt");
        return draft;
    }

    public static MessageUnreadSummaryModel parseUnreadSummary(JSONObject json) {
        MessageUnreadSummaryModel model = new MessageUnreadSummaryModel();
        model.unreadCount = optInt(json, "unreadCount");
        model.systemNoticeUnreadCount = optInt(json, "systemNoticeUnreadCount");
        model.reviewResultUnreadCount = optInt(json, "reviewResultUnreadCount");
        model.interactionReminderUnreadCount = optInt(json, "interactionReminderUnreadCount");
        model.directUnreadCount = optInt(json, "directUnreadCount");
        return model;
    }

    public static MessageBoardEntryModel parseMessageBoardEntry(JSONObject json) {
        MessageBoardEntryModel model = new MessageBoardEntryModel();
        model.id = optLong(json, "id");
        model.authorUsername = firstNonEmpty(optString(json, "authorUsername"), optString(json, "createdByUsername"));
        model.content = optString(json, "content");
        model.status = optString(json, "status");
        model.statusNote = optString(json, "statusNote");
        model.createdAt = optString(json, "createdAt");
        model.updatedAt = optString(json, "updatedAt");
        return model;
    }

    public static ReportModel parseReport(JSONObject json) {
        ReportModel model = new ReportModel();
        model.id = optLong(json, "id");
        model.targetType = optString(json, "targetType");
        model.targetId = optLong(json, "targetId");
        model.reasonType = optString(json, "reasonType");
        model.reasonDetail = optString(json, "reasonDetail");
        model.status = optString(json, "status");
        model.reporterUsername = optString(json, "reporterUsername");
        model.processNote = optString(json, "processNote");
        model.createdAt = optString(json, "createdAt");
        model.updatedAt = optString(json, "updatedAt");
        return model;
    }

    public static ReviewTemplateModel parseReviewTemplate(JSONObject json) {
        ReviewTemplateModel model = new ReviewTemplateModel();
        model.id = optLong(json, "id");
        model.templateName = optString(json, "templateName");
        model.templateContent = optString(json, "templateContent");
        model.enabled = json.optBoolean("enabled", true);
        return model;
    }

    public static AuditLogModel parseAuditLog(JSONObject json) {
        AuditLogModel model = new AuditLogModel();
        model.id = optLong(json, "id");
        model.adminUsername = optString(json, "adminUsername");
        model.actionType = optString(json, "actionType");
        model.targetType = optString(json, "targetType");
        model.targetId = firstNonEmpty(optString(json, "targetId"), String.valueOf(optLong(json, "targetId")));
        model.detail = firstNonEmpty(optString(json, "detail"), optString(json, "actionDetail"));
        model.createdAt = optString(json, "createdAt");
        return model;
    }

    public static UserProfileModel parseUserProfile(JSONObject json) {
        UserProfileModel model = new UserProfileModel();
        model.username = optString(json, "username");
        model.postCount = optInt(json, "postCount");
        model.approvedPostCount = optInt(json, "approvedPostCount");
        model.approvalRate = json == null ? 0d : json.optDouble("approvalRate", 0d);
        model.favoriteReceivedCount = optInt(json, "favoriteReceivedCount");
        model.likeReceivedCount = optInt(json, "likeReceivedCount");
        model.commentReceivedCount = optInt(json, "commentReceivedCount");
        model.activeStreakDays = optInt(json, "activeStreakDays");
        model.badges.addAll(optStringList(json, "badges"));
        return model;
    }

    public static SearchSuggestionModel parseSuggestion(JSONObject json) {
        SearchSuggestionModel model = new SearchSuggestionModel();
        model.keyword = optString(json, "keyword");
        model.score = optInt(json, "score");
        model.source = optString(json, "source");
        return model;
    }

    public static HotKeywordModel parseHotKeyword(JSONObject json) {
        HotKeywordModel model = new HotKeywordModel();
        model.keyword = optString(json, "keyword");
        model.searchCount = optInt(json, "searchCount");
        model.lastSearchedAt = optString(json, "lastSearchedAt");
        return model;
    }

    public static LikeStatusModel parseLikeStatus(JSONObject json) {
        LikeStatusModel model = new LikeStatusModel();
        model.postId = optLong(json, "postId");
        model.liked = json.optBoolean("liked", false);
        model.likeCount = optInt(json, "likeCount");
        return model;
    }

    public static String shorten(String content) {
        if (TextUtils.isEmpty(content)) {
            return "";
        }
        return content.length() > 30 ? content.substring(0, 30) + "..." : content;
    }

    public static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }
}
