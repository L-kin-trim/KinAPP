package com.example.kin.data;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.example.kin.model.AuditLogModel;
import com.example.kin.model.DraftModel;
import com.example.kin.model.FavoriteStatus;
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
import com.example.kin.model.SearchSuggestionModel;
import com.example.kin.model.SessionUser;
import com.example.kin.model.StationMessageModel;
import com.example.kin.model.UserProfileModel;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiClient;
import com.example.kin.net.ApiException;
import com.example.kin.util.JsonUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KinRepository {
    private final Context appContext;
    private final ApiClient apiClient;
    private final SessionManager sessionManager;
    private final LocalDraftStore localDraftStore;

    public KinRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.apiClient = new ApiClient(appContext);
        this.sessionManager = new SessionManager(appContext);
        this.localDraftStore = new LocalDraftStore(appContext);
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public LocalDraftStore getLocalDraftStore() {
        return localDraftStore;
    }

    public void login(String username, String password, ApiCallback<SessionUser> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
        } catch (Exception ignored) {
        }
        apiClient.postJson("/api/auth/login", body, false, new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                SessionUser user = JsonUtils.parseSessionUser(data.optJSONObject("user"));
                sessionManager.saveSession(data.optString("token"), user);
                callback.onSuccess(user);
            }

            @Override
            public void onError(ApiException exception) {
                callback.onError(exception);
            }
        });
    }

    public void register(String username, String password, String email, ApiCallback<SessionUser> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            body.put("password", password);
            body.put("email", email);
        } catch (Exception ignored) {
        }
        apiClient.postJson("/api/auth/register", body, false, modelCallback(callback, JsonUtils::parseSessionUser));
    }

    public void getPosts(String postType, int page, int size, ApiCallback<PageResult<ForumPostModel>> callback) {
        getPosts(postType, page, size, "", "", "", callback);
    }

    public void getPosts(String postType, int page, int size, String sortType, String mapTag, String toolTag,
                         ApiCallback<PageResult<ForumPostModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("postType", postType);
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        query.put("sortType", sortType);
        query.put("mapTag", mapTag);
        query.put("toolTag", toolTag);
        apiClient.get("/api/forum/posts", query, true, pageCallback(callback, JsonUtils::parsePost));
    }

    public void searchPosts(String keyword, String postType, String mapName, String author, int page, int size,
                            ApiCallback<PageResult<ForumPostModel>> callback) {
        searchPosts(keyword, postType, mapName, author, page, size, "", "", "", callback);
    }

    public void searchPosts(String keyword, String postType, String mapName, String author, int page, int size,
                            String sortType, String mapTag, String toolTag,
                            ApiCallback<PageResult<ForumPostModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("keyword", keyword);
        query.put("postType", postType);
        query.put("mapName", mapName);
        query.put("createdByUsername", author);
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        query.put("sortType", sortType);
        query.put("mapTag", mapTag);
        query.put("toolTag", toolTag);
        apiClient.get("/api/forum/posts/search", query, true, pageCallback(callback, JsonUtils::parsePost));
    }

    public void getHotKeywords(int limit, ApiCallback<List<HotKeywordModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("limit", String.valueOf(limit));
        apiClient.get("/api/forum/posts/search/hot-keywords", query, true, listCallback(callback, JsonUtils::parseHotKeyword));
    }

    public void getSearchSuggestions(String keyword, int limit, ApiCallback<List<SearchSuggestionModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("keyword", keyword);
        query.put("limit", String.valueOf(limit));
        apiClient.get("/api/forum/posts/search/suggestions", query, true, listCallback(callback, JsonUtils::parseSuggestion));
    }

    public void getPostDetail(long postId, boolean mine, ApiCallback<ForumPostModel> callback) {
        String path = mine ? "/api/forum/posts/" + postId + "/mine" : "/api/forum/posts/" + postId;
        apiClient.get(path, null, true, modelCallback(callback, JsonUtils::parsePost));
    }

    public void createPost(JSONObject payload, ApiCallback<ForumPostModel> callback) {
        apiClient.postJson("/api/forum/posts", payload, true, modelCallback(callback, JsonUtils::parsePost));
    }

    public void updatePost(long postId, JSONObject payload, ApiCallback<ForumPostModel> callback) {
        apiClient.putJson("/api/forum/posts/" + postId, payload, true, modelCallback(callback, JsonUtils::parsePost));
    }

    public void withdrawPost(long postId, ApiCallback<ForumPostModel> callback) {
        apiClient.patchJson("/api/forum/posts/" + postId + "/withdraw", new JSONObject(), true, modelCallback(callback, JsonUtils::parsePost));
    }

    public void likePost(long postId, ApiCallback<LikeStatusModel> callback) {
        apiClient.postJson("/api/forum/posts/" + postId + "/like", new JSONObject(), true, modelCallback(callback, JsonUtils::parseLikeStatus));
    }

    public void unlikePost(long postId, ApiCallback<LikeStatusModel> callback) {
        apiClient.deleteJson("/api/forum/posts/" + postId + "/like", null, true, modelCallback(callback, JsonUtils::parseLikeStatus));
    }

    public void getLikeStatus(long postId, ApiCallback<LikeStatusModel> callback) {
        apiClient.get("/api/forum/posts/" + postId + "/like", null, true, modelCallback(callback, JsonUtils::parseLikeStatus));
    }

    public void getComments(long postId, ApiCallback<List<ForumCommentModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("page", "0");
        query.put("size", "100");
        apiClient.get("/api/forum/comments/posts/" + postId, query, true, new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                JSONArray items = data.optJSONArray("items");
                if (items == null) {
                    items = data.optJSONArray("content");
                }
                callback.onSuccess(parseList(items, JsonUtils::parseComment));
            }

            @Override
            public void onError(ApiException exception) {
                callback.onError(exception);
            }
        });
    }

    public void createComment(long postId, String content, List<String> imageUrls, ApiCallback<ForumCommentModel> callback) {
        createComment(postId, content, 0L, Collections.emptyList(), imageUrls, callback);
    }

    public void createComment(long postId, String content, long parentCommentId, List<String> mentionUsernames,
                              List<String> imageUrls, ApiCallback<ForumCommentModel> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("content", content);
            if (parentCommentId > 0) {
                body.put("parentCommentId", parentCommentId);
            } else {
                body.put("parentCommentId", JSONObject.NULL);
            }
            body.put("mentionUsernames", new JSONArray(mentionUsernames == null ? Collections.emptyList() : mentionUsernames));
            body.put("imageUrls", new JSONArray(imageUrls == null ? Collections.emptyList() : imageUrls));
        } catch (Exception ignored) {
        }
        apiClient.postJson("/api/forum/comments/posts/" + postId, body, true, modelCallback(callback, JsonUtils::parseComment));
    }

    public void getAdminPosts(String status, int page, int size, ApiCallback<PageResult<ForumPostModel>> callback) {
        getAdminPosts(status, "", "", "", "", "", page, size, callback);
    }

    public void getAdminPosts(String status, String postType, String mapName, String author, String createdFrom, String createdTo,
                              int page, int size, ApiCallback<PageResult<ForumPostModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("status", status);
        query.put("postType", postType);
        query.put("mapName", mapName);
        query.put("createdByUsername", author);
        query.put("createdFrom", createdFrom);
        query.put("createdTo", createdTo);
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        apiClient.get("/api/forum/posts/admin/review", query, true, pageCallback(callback, JsonUtils::parsePost));
    }

    public void reviewPost(long postId, String reviewStatus, String reviewRemark, ApiCallback<ForumPostModel> callback) {
        reviewPost(postId, reviewStatus, reviewRemark, 0L, callback);
    }

    public void reviewPost(long postId, String reviewStatus, String reviewRemark, long rejectTemplateId,
                           ApiCallback<ForumPostModel> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("reviewStatus", reviewStatus);
            body.put("reviewRemark", reviewRemark);
            if (rejectTemplateId > 0) {
                body.put("rejectTemplateId", rejectTemplateId);
            } else {
                body.put("rejectTemplateId", JSONObject.NULL);
            }
        } catch (Exception ignored) {
        }
        apiClient.patchJson("/api/forum/posts/" + postId + "/admin/review", body, true, modelCallback(callback, JsonUtils::parsePost));
    }

    public void batchReviewPosts(List<Long> postIds, String reviewStatus, String reviewRemark, long rejectTemplateId,
                                 ApiCallback<JSONObject> callback) {
        JSONObject body = new JSONObject();
        try {
            JSONArray ids = new JSONArray();
            for (Long postId : postIds) {
                ids.put(postId);
            }
            body.put("postIds", ids);
            body.put("reviewStatus", reviewStatus);
            body.put("reviewRemark", reviewRemark);
            if (rejectTemplateId > 0) {
                body.put("rejectTemplateId", rejectTemplateId);
            } else {
                body.put("rejectTemplateId", JSONObject.NULL);
            }
        } catch (Exception ignored) {
        }
        apiClient.postJson("/api/forum/posts/admin/review/batch", body, true, callback);
    }

    public void deletePostAdmin(long postId, String reason, ApiCallback<ForumPostModel> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("reason", reason);
        } catch (Exception ignored) {
        }
        apiClient.deleteJson("/api/forum/posts/" + postId + "/admin", body, true, modelCallback(callback, JsonUtils::parsePost));
    }

    public void getAdminComments(String status, int page, int size, ApiCallback<PageResult<ForumCommentModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("status", status);
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        apiClient.get("/api/forum/comments/admin/review", query, true, pageCallback(callback, JsonUtils::parseComment));
    }

    public void reviewComment(long commentId, String reviewStatus, String reviewRemark, ApiCallback<JSONObject> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("reviewStatus", reviewStatus);
            body.put("reviewRemark", reviewRemark);
        } catch (Exception ignored) {
        }
        apiClient.patchJson("/api/forum/comments/" + commentId + "/admin/review", body, true, callback);
    }

    public void deleteCommentAdmin(long commentId, ApiCallback<JSONObject> callback) {
        apiClient.deleteJson("/api/forum/comments/" + commentId + "/admin", null, true, callback);
    }

    public void uploadSingleImage(Uri uri, String module, ApiCallback<ImageUploadItem> callback) {
        try {
            List<ApiClient.MultipartPart> parts = apiClient.createParts(appContext, "file", Collections.singletonList(uri));
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("module", module);
            apiClient.postMultipart("/api/forum/images/upload", fields, parts, true, modelCallback(callback, JsonUtils::parseUploadItem));
        } catch (Exception exception) {
            callback.onError(new ApiException(-1, exception.getMessage()));
        }
    }

    public void uploadBatchImages(List<Uri> uris, String module, ApiCallback<List<ImageUploadItem>> callback) {
        try {
            List<ApiClient.MultipartPart> parts = apiClient.createParts(appContext, "files", uris);
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("module", module);
            apiClient.postMultipart("/api/forum/images/upload/batch", fields, parts, true, new ApiCallback<>() {
                @Override
                public void onSuccess(JSONObject data) {
                    callback.onSuccess(parseList(data.optJSONArray("items"), JsonUtils::parseUploadItem));
                }

                @Override
                public void onError(ApiException exception) {
                    callback.onError(exception);
                }
            });
        } catch (Exception exception) {
            callback.onError(new ApiException(-1, exception.getMessage()));
        }
    }

    public void saveDraft(long id, String draftType, String title, String payloadJson, boolean autoSaved,
                          ApiCallback<DraftModel> callback) {
        JSONObject body = new JSONObject();
        try {
            if (id > 0) {
                body.put("id", id);
            } else {
                body.put("id", JSONObject.NULL);
            }
            body.put("draftType", draftType);
            body.put("title", title);
            body.put("payloadJson", payloadJson);
            body.put("autoSaved", autoSaved);
        } catch (Exception ignored) {
        }
        apiClient.postJson("/api/drafts", body, true, modelCallback(callback, JsonUtils::parseDraft));
    }

    public void getDrafts(String draftType, int page, int size, ApiCallback<PageResult<DraftModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("draftType", draftType);
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        apiClient.get("/api/drafts", query, true, pageCallback(callback, JsonUtils::parseDraft));
    }

    public void getDraftDetail(long draftId, ApiCallback<DraftModel> callback) {
        apiClient.get("/api/drafts/" + draftId, null, true, modelCallback(callback, JsonUtils::parseDraft));
    }

    public void deleteDraft(long draftId, ApiCallback<JSONObject> callback) {
        apiClient.deleteJson("/api/drafts/" + draftId, null, true, callback);
    }

    public void createLibraryItem(JSONObject payload, ApiCallback<LibraryItem> callback) {
        apiClient.postJson("/api/my/library/items", payload, true, modelCallback(callback, JsonUtils::parseLibraryItem));
    }

    public void getLibraryItems(String postType, int page, int size, ApiCallback<PageResult<LibraryItem>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("postType", postType);
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        apiClient.get("/api/my/library/items", query, true, pageCallback(callback, JsonUtils::parseLibraryItem));
    }

    public void favoritePost(long postId, ApiCallback<FavoriteStatus> callback) {
        apiClient.postJson("/api/my/library/favorites/" + postId, new JSONObject(), true, modelCallback(callback, JsonUtils::parseFavoriteStatus));
    }

    public void unfavoritePost(long postId, ApiCallback<FavoriteStatus> callback) {
        apiClient.deleteJson("/api/my/library/favorites/" + postId, null, true, modelCallback(callback, JsonUtils::parseFavoriteStatus));
    }

    public void getFavorites(String postType, int page, int size, ApiCallback<PageResult<LibraryItem>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("postType", postType);
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        apiClient.get("/api/my/library/favorites", query, true, pageCallback(callback, JsonUtils::parseLibraryItem));
    }

    public void sendMessage(String recipientUsername, String content, ApiCallback<StationMessageModel> callback) {
        sendMessage(recipientUsername, content, "DIRECT", callback);
    }

    public void sendMessage(String recipientUsername, String content, String messageType, ApiCallback<StationMessageModel> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("recipientUsername", recipientUsername);
            body.put("content", content);
            body.put("messageType", TextUtils.isEmpty(messageType) ? "DIRECT" : messageType);
        } catch (Exception ignored) {
        }
        apiClient.postJson("/api/messages", body, true, modelCallback(callback, JsonUtils::parseStationMessage));
    }

    public void getInbox(int page, int size, ApiCallback<PageResult<StationMessageModel>> callback) {
        getInbox(page, size, "ALL", "", callback);
    }

    public void getInbox(int page, int size, String readFilter, String messageType,
                         ApiCallback<PageResult<StationMessageModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        query.put("readFilter", readFilter);
        query.put("messageType", messageType);
        apiClient.get("/api/messages/inbox", query, true, pageCallback(callback, JsonUtils::parseStationMessage));
    }

    public void getSent(int page, int size, ApiCallback<PageResult<StationMessageModel>> callback) {
        getSent(page, size, "", callback);
    }

    public void getSent(int page, int size, String messageType, ApiCallback<PageResult<StationMessageModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        query.put("messageType", messageType);
        apiClient.get("/api/messages/sent", query, true, pageCallback(callback, JsonUtils::parseStationMessage));
    }

    public void getUnreadSummary(ApiCallback<MessageUnreadSummaryModel> callback) {
        apiClient.get("/api/messages/unread-summary", null, true, modelCallback(callback, JsonUtils::parseUnreadSummary));
    }

    public void markMessageRead(long messageId, ApiCallback<JSONObject> callback) {
        apiClient.patchJson("/api/messages/" + messageId + "/read", new JSONObject(), true, callback);
    }

    public void markAllMessagesRead(String messageType, ApiCallback<JSONObject> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("messageType", messageType);
        apiClient.postJson("/api/messages/read-all", query, new JSONObject(), true, callback);
    }

    public void createMessageBoardEntry(String content, ApiCallback<MessageBoardEntryModel> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("content", content);
        } catch (Exception ignored) {
        }
        apiClient.postJson("/api/message-board/entries", body, true, modelCallback(callback, JsonUtils::parseMessageBoardEntry));
    }

    public void getMessageBoardEntries(String status, int page, int size, ApiCallback<PageResult<MessageBoardEntryModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("status", status);
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        apiClient.get("/api/message-board/entries", query, true, pageCallback(callback, JsonUtils::parseMessageBoardEntry));
    }

    public void getMessageBoardEntry(long entryId, ApiCallback<MessageBoardEntryModel> callback) {
        apiClient.get("/api/message-board/entries/" + entryId, null, true, modelCallback(callback, JsonUtils::parseMessageBoardEntry));
    }

    public void getMyMessageBoardEntries(int page, int size, ApiCallback<PageResult<MessageBoardEntryModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        apiClient.get("/api/message-board/entries/mine", query, true, pageCallback(callback, JsonUtils::parseMessageBoardEntry));
    }

    public void revokeMessageBoardEntry(long entryId, String statusNote, ApiCallback<MessageBoardEntryModel> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("statusNote", statusNote);
        } catch (Exception ignored) {
        }
        apiClient.patchJson("/api/message-board/entries/" + entryId + "/revoke", body, true, modelCallback(callback, JsonUtils::parseMessageBoardEntry));
    }

    public void createReport(String targetType, long targetId, String reasonType, String reasonDetail, ApiCallback<ReportModel> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("targetType", targetType);
            body.put("targetId", targetId);
            body.put("reasonType", reasonType);
            body.put("reasonDetail", reasonDetail);
        } catch (Exception ignored) {
        }
        apiClient.postJson("/api/reports", body, true, modelCallback(callback, JsonUtils::parseReport));
    }

    public void getMyReports(int page, int size, ApiCallback<PageResult<ReportModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        apiClient.get("/api/reports/mine", query, true, pageCallback(callback, JsonUtils::parseReport));
    }

    public void getAdminOverview(int windowDays, ApiCallback<JSONObject> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("windowDays", String.valueOf(windowDays));
        apiClient.get("/api/admin/operations/overview", query, true, callback);
    }

    public void getReviewTemplates(boolean enabledOnly, ApiCallback<List<ReviewTemplateModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("enabledOnly", String.valueOf(enabledOnly));
        apiClient.get("/api/admin/review/templates", query, true, listCallback(callback, JsonUtils::parseReviewTemplate));
    }

    public void createReviewTemplate(String templateName, String templateContent, boolean enabled, ApiCallback<ReviewTemplateModel> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("templateName", templateName);
            body.put("templateContent", templateContent);
            body.put("enabled", enabled);
        } catch (Exception ignored) {
        }
        apiClient.postJson("/api/admin/review/templates", body, true, modelCallback(callback, JsonUtils::parseReviewTemplate));
    }

    public void updateReviewTemplate(long templateId, String templateName, String templateContent, boolean enabled,
                                     ApiCallback<ReviewTemplateModel> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("templateName", templateName);
            body.put("templateContent", templateContent);
            body.put("enabled", enabled);
        } catch (Exception ignored) {
        }
        apiClient.putJson("/api/admin/review/templates/" + templateId, body, true, modelCallback(callback, JsonUtils::parseReviewTemplate));
    }

    public void deleteReviewTemplate(long templateId, ApiCallback<JSONObject> callback) {
        apiClient.deleteJson("/api/admin/review/templates/" + templateId, null, true, callback);
    }

    public void getAdminReports(String status, String targetType, String reasonType, String reporterUsername,
                                int page, int size, ApiCallback<PageResult<ReportModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("status", status);
        query.put("targetType", targetType);
        query.put("reasonType", reasonType);
        query.put("reporterUsername", reporterUsername);
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        apiClient.get("/api/admin/reports", query, true, pageCallback(callback, JsonUtils::parseReport));
    }

    public void handleReport(long reportId, String processNote, ApiCallback<ReportModel> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("processNote", processNote);
        } catch (Exception ignored) {
        }
        apiClient.patchJson("/api/admin/reports/" + reportId + "/handle", body, true, modelCallback(callback, JsonUtils::parseReport));
    }

    public void getAdminMessageBoardEntries(String status, String authorUsername, int page, int size,
                                            ApiCallback<PageResult<MessageBoardEntryModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("status", status);
        query.put("authorUsername", authorUsername);
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        apiClient.get("/api/admin/message-board/entries", query, true, pageCallback(callback, JsonUtils::parseMessageBoardEntry));
    }

    public void getAdminMessageBoardEntry(long entryId, ApiCallback<MessageBoardEntryModel> callback) {
        apiClient.get("/api/admin/message-board/entries/" + entryId, null, true, modelCallback(callback, JsonUtils::parseMessageBoardEntry));
    }

    public void updateAdminMessageBoardStatus(long entryId, String status, String statusNote,
                                              ApiCallback<MessageBoardEntryModel> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("status", status);
            body.put("statusNote", statusNote);
        } catch (Exception ignored) {
        }
        apiClient.patchJson("/api/admin/message-board/entries/" + entryId + "/status", body, true, modelCallback(callback, JsonUtils::parseMessageBoardEntry));
    }

    public void revokeAdminMessageBoardEntry(long entryId, String statusNote, ApiCallback<MessageBoardEntryModel> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("statusNote", statusNote);
        } catch (Exception ignored) {
        }
        apiClient.patchJson("/api/admin/message-board/entries/" + entryId + "/revoke", body, true, modelCallback(callback, JsonUtils::parseMessageBoardEntry));
    }

    public void searchUsers(String keyword, int page, int size, ApiCallback<JSONObject> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("keyword", keyword);
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        apiClient.get("/api/admin/users/search", query, true, callback);
    }

    public void getUserAudit(long userId, ApiCallback<JSONObject> callback) {
        apiClient.get("/api/admin/users/" + userId + "/overview", null, true, callback);
    }

    public void getAuditLogs(String adminUsername, String actionType, String from, String to, int page, int size,
                             ApiCallback<PageResult<AuditLogModel>> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("adminUsername", adminUsername);
        query.put("actionType", actionType);
        query.put("from", from);
        query.put("to", to);
        query.put("page", String.valueOf(page));
        query.put("size", String.valueOf(size));
        apiClient.get("/api/admin/audit-logs", query, true, pageCallback(callback, JsonUtils::parseAuditLog));
    }

    public void exportAuditLogs(String adminUsername, String actionType, String from, String to,
                                ApiCallback<String> callback) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("adminUsername", adminUsername);
        query.put("actionType", actionType);
        query.put("from", from);
        query.put("to", to);
        apiClient.getText("/api/admin/audit-logs/export", query, true, callback);
    }

    public void getUserProfile(String username, ApiCallback<UserProfileModel> callback) {
        String path = TextUtils.isEmpty(username) ? "/api/users/me/profile" : "/api/users/" + username + "/profile";
        apiClient.get(path, null, true, modelCallback(callback, JsonUtils::parseUserProfile));
    }

    private <T> ApiCallback<JSONObject> modelCallback(ApiCallback<T> callback, JsonUtils.ItemParser<T> parser) {
        return new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                callback.onSuccess(parser.parse(data));
            }

            @Override
            public void onError(ApiException exception) {
                callback.onError(exception);
            }
        };
    }

    private <T> ApiCallback<JSONObject> pageCallback(ApiCallback<PageResult<T>> callback, JsonUtils.ItemParser<T> parser) {
        return new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                callback.onSuccess(JsonUtils.parsePage(data, parser));
            }

            @Override
            public void onError(ApiException exception) {
                callback.onError(exception);
            }
        };
    }

    private <T> ApiCallback<JSONObject> listCallback(ApiCallback<List<T>> callback, JsonUtils.ItemParser<T> parser) {
        return new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                JSONArray items = data.optJSONArray("items");
                if (items == null) {
                    items = data.optJSONArray("content");
                }
                callback.onSuccess(parseList(items, parser));
            }

            @Override
            public void onError(ApiException exception) {
                callback.onError(exception);
            }
        };
    }

    private <T> List<T> parseList(JSONArray array, JsonUtils.ItemParser<T> parser) {
        List<T> items = new ArrayList<>();
        if (array == null) {
            return items;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item != null) {
                items.add(parser.parse(item));
            }
        }
        return items;
    }
}
