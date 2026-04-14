package com.example.kin.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.R;
import com.example.kin.data.KinRepository;
import com.example.kin.model.ForumCommentModel;
import com.example.kin.model.ForumPostModel;
import com.example.kin.model.ImageUploadItem;
import com.example.kin.model.LikeStatusModel;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.KinUi;
import com.example.kin.ui.common.RemoteImageLoader;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {
    public static final String EXTRA_POST_ID = "extra_post_id";
    public static final String EXTRA_MINE = "extra_mine";

    private KinRepository repository;
    private RemoteImageLoader imageLoader;
    private long postId;
    private boolean mine;
    private ForumPostModel currentPost;
    private LinearLayout contentLayout;
    private LinearLayout commentsLayout;
    private ProgressBar progressBar;
    private TextView statusView;
    private TextView commentImageState;
    private TextView replyHintView;
    private TextInputEditText commentEdit;
    private MaterialButton likeButton;
    private long replyTargetCommentId;
    private final List<Uri> commentImageUris = new ArrayList<>();

    private final ActivityResultLauncher<String> commentImagePicker = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                commentImageUris.clear();
                commentImageUris.addAll(uris);
                if (commentImageState != null) {
                    commentImageState.setText("评论图片：" + commentImageUris.size() + " 张");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KinRepository(this);
        imageLoader = new RemoteImageLoader();
        postId = getIntent().getLongExtra(EXTRA_POST_ID, 0L);
        mine = getIntent().getBooleanExtra(EXTRA_MINE, false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("帖子详情");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());
        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(this);
        contentLayout = KinUi.vertical(this);
        contentLayout.setPadding(KinUi.dp(this, 18), KinUi.dp(this, 12), KinUi.dp(this, 18), KinUi.dp(this, 24));
        progressBar = new ProgressBar(this);
        statusView = KinUi.muted(this, "", 13);
        statusView.setVisibility(View.GONE);
        contentLayout.addView(progressBar);
        contentLayout.addView(statusView);
        scrollView.addView(contentLayout);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        loadAll();
    }

    private void loadAll() {
        setLoading(true, "正在加载帖子…");
        repository.getPostDetail(postId, mine, new ApiCallback<>() {
            @Override
            public void onSuccess(ForumPostModel data) {
                currentPost = data;
                renderPost();
                loadComments();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "帖子加载失败：" + exception.getMessage());
            }
        });
    }

    private void loadComments() {
        repository.getComments(postId, new ApiCallback<>() {
            @Override
            public void onSuccess(List<ForumCommentModel> data) {
                renderComments(data);
                setLoading(false, data.isEmpty() ? "还没有评论。" : "");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "评论加载失败：" + exception.getMessage());
            }
        });
    }

    private void renderPost() {
        contentLayout.removeAllViews();
        contentLayout.addView(progressBar);
        contentLayout.addView(statusView);

        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        body.addView(KinUi.text(this, currentPost.title, 22, true));
        TextView meta = KinUi.muted(this,
                currentPost.createdByUsername + " · " + currentPost.createdAt + " · " + currentPost.reviewStatus,
                13);
        KinUi.margins(meta, this, 0, 8, 0, 0);
        body.addView(meta);

        TextView statusText = KinUi.muted(this,
                "版本 " + currentPost.version
                        + (TextUtils.isEmpty(currentPost.reviewRemark) ? "" : " · 审核备注：" + currentPost.reviewRemark)
                        + (TextUtils.isEmpty(currentPost.editableUntil) ? "" : " · 可编辑至 " + currentPost.editableUntil),
                13);
        KinUi.margins(statusText, this, 0, 8, 0, 0);
        body.addView(statusText);

        TextView summary = KinUi.muted(this, buildSummary(), 15);
        KinUi.margins(summary, this, 0, 12, 0, 0);
        body.addView(summary);

        List<String> images = previewImages();
        if (!images.isEmpty()) {
            View strip = KinUi.imageStrip(this, images, imageLoader);
            KinUi.margins(strip, this, 0, 14, 0, 0);
            body.addView(strip);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        likeButton = KinUi.filledButton(this, (currentPost.liked ? "已赞 " : "点赞 ") + currentPost.likeCount);
        likeButton.setOnClickListener(v -> toggleLike());
        MaterialButton favoriteButton = KinUi.outlinedButton(this, "收藏");
        favoriteButton.setOnClickListener(v -> repository.favoritePost(postId, new ApiCallback<>() {
            @Override
            public void onSuccess(com.example.kin.model.FavoriteStatus data) {
                favoriteButton.setText(data.favorited ? "已收藏" : "收藏");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "收藏失败：" + exception.getMessage());
            }
        }));
        MaterialButton messageButton = KinUi.outlinedButton(this, "发消息");
        messageButton.setOnClickListener(v -> showMessageDialog());
        MaterialButton reportButton = KinUi.outlinedButton(this, "举报");
        reportButton.setOnClickListener(v -> showReportDialog("POST", currentPost.id));
        actions.addView(likeButton);
        actions.addView(favoriteButton);
        actions.addView(messageButton);
        actions.addView(reportButton);
        KinUi.margins(favoriteButton, this, 10, 0, 0, 0);
        KinUi.margins(messageButton, this, 10, 0, 0, 0);
        KinUi.margins(reportButton, this, 10, 0, 0, 0);
        KinUi.margins(actions, this, 0, 16, 0, 0);
        body.addView(actions);

        if (currentPost.canEdit || mine) {
            MaterialButton editButton = KinUi.outlinedButton(this, "更新帖子");
            editButton.setOnClickListener(v -> showEditDialog());
            KinUi.margins(editButton, this, 0, 12, 0, 0);
            body.addView(editButton);
        }
        if (currentPost.canWithdraw || mine) {
            MaterialButton withdrawButton = KinUi.outlinedButton(this, "撤回帖子");
            withdrawButton.setOnClickListener(v -> repository.withdrawPost(postId, new ApiCallback<>() {
                @Override
                public void onSuccess(ForumPostModel data) {
                    currentPost = data;
                    renderPost();
                    loadComments();
                }

                @Override
                public void onError(ApiException exception) {
                    setLoading(false, "撤回失败：" + exception.getMessage());
                }
            }));
            KinUi.margins(withdrawButton, this, 0, 12, 0, 0);
            body.addView(withdrawButton);
        }

        card.addView(body);
        contentLayout.addView(card);
        contentLayout.addView(buildCommentComposer());

        commentsLayout = KinUi.vertical(this);
        contentLayout.addView(commentsLayout);
    }

    private View buildCommentComposer() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        body.addView(KinUi.text(this, "发表评论", 18, true));
        replyHintView = KinUi.muted(this, "", 13);
        replyHintView.setVisibility(View.GONE);
        KinUi.margins(replyHintView, this, 0, 8, 0, 0);
        body.addView(replyHintView);
        TextInputLayout commentLayout = KinUi.inputLayout(this, "评论内容，支持 @用户名", true);
        commentEdit = KinUi.edit(commentLayout);
        KinUi.margins(commentLayout, this, 0, 12, 0, 0);
        body.addView(commentLayout);

        LinearLayout imageRow = new LinearLayout(this);
        imageRow.setOrientation(LinearLayout.HORIZONTAL);
        commentImageState = KinUi.muted(this, "评论图片：0 张", 13);
        MaterialButton pickButton = KinUi.outlinedButton(this, "选择图片");
        pickButton.setOnClickListener(v -> commentImagePicker.launch("image/*"));
        imageRow.addView(commentImageState);
        imageRow.addView(pickButton);
        KinUi.margins(pickButton, this, 10, 0, 0, 0);
        KinUi.margins(imageRow, this, 0, 12, 0, 0);
        body.addView(imageRow);

        MaterialButton sendButton = KinUi.filledButton(this, "提交评论");
        sendButton.setOnClickListener(v -> submitComment());
        KinUi.margins(sendButton, this, 0, 14, 0, 0);
        body.addView(sendButton);

        card.addView(body);
        return card;
    }

    private void renderComments(List<ForumCommentModel> comments) {
        commentsLayout.removeAllViews();
        for (ForumCommentModel comment : comments) {
            MaterialCardView card = KinUi.card(this);
            LinearLayout body = KinUi.sectionContainer(this, 16);
            body.addView(KinUi.text(this, "#" + comment.floorNumber + " " + comment.username, 16, true));
            TextView meta = KinUi.muted(this,
                    comment.createdAt + (TextUtils.isEmpty(comment.replyToUsername) ? "" : " · 回复 " + comment.replyToUsername),
                    12);
            KinUi.margins(meta, this, 0, 6, 0, 0);
            body.addView(meta);
            TextView content = KinUi.muted(this, comment.content, 15);
            KinUi.margins(content, this, 0, 10, 0, 0);
            body.addView(content);
            if (!comment.imageUrls.isEmpty()) {
                View strip = KinUi.imageStrip(this, comment.imageUrls, imageLoader);
                KinUi.margins(strip, this, 0, 12, 0, 0);
                body.addView(strip);
            }

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            MaterialButton replyButton = KinUi.outlinedButton(this, "回复");
            replyButton.setOnClickListener(v -> {
                replyTargetCommentId = comment.id;
                replyHintView.setVisibility(View.VISIBLE);
                replyHintView.setText("正在回复 @" + comment.username);
                commentEdit.requestFocus();
            });
            MaterialButton reportButton = KinUi.outlinedButton(this, "举报");
            reportButton.setOnClickListener(v -> showReportDialog("COMMENT", comment.id));
            actions.addView(replyButton);
            actions.addView(reportButton);
            KinUi.margins(reportButton, this, 10, 0, 0, 0);
            KinUi.margins(actions, this, 0, 12, 0, 0);
            body.addView(actions);
            card.addView(body);
            commentsLayout.addView(card);
        }
    }

    private void submitComment() {
        if (!repository.getSessionManager().isLoggedIn()) {
            setLoading(false, "请先登录再评论。");
            return;
        }
        setLoading(true, "正在提交评论…");
        if (commentImageUris.isEmpty()) {
            repository.createComment(postId, text(commentEdit), replyTargetCommentId, parseMentions(text(commentEdit)), new ArrayList<>(), commentCallback());
            return;
        }
        repository.uploadBatchImages(commentImageUris, "comments", new ApiCallback<>() {
            @Override
            public void onSuccess(List<ImageUploadItem> data) {
                List<String> urls = new ArrayList<>();
                for (ImageUploadItem item : data) {
                    urls.add(item.url);
                }
                repository.createComment(postId, text(commentEdit), replyTargetCommentId, parseMentions(text(commentEdit)), urls, commentCallback());
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "评论图片上传失败：" + exception.getMessage());
            }
        });
    }

    private ApiCallback<ForumCommentModel> commentCallback() {
        return new ApiCallback<>() {
            @Override
            public void onSuccess(ForumCommentModel data) {
                commentEdit.setText("");
                replyTargetCommentId = 0L;
                replyHintView.setVisibility(View.GONE);
                commentImageUris.clear();
                if (commentImageState != null) {
                    commentImageState.setText("评论图片：0 张");
                }
                loadComments();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "评论失败：" + exception.getMessage());
            }
        };
    }

    private void toggleLike() {
        ApiCallback<LikeStatusModel> callback = new ApiCallback<>() {
            @Override
            public void onSuccess(LikeStatusModel data) {
                currentPost.liked = data.liked;
                currentPost.likeCount = data.likeCount;
                likeButton.setText((data.liked ? "已赞 " : "点赞 ") + data.likeCount);
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "点赞失败：" + exception.getMessage());
            }
        };
        if (currentPost.liked) {
            repository.unlikePost(postId, callback);
        } else {
            repository.likePost(postId, callback);
        }
    }

    private void showMessageDialog() {
        if (!repository.getSessionManager().isLoggedIn()) {
            setLoading(false, "请先登录再发送站内信。");
            return;
        }
        TextInputLayout inputLayout = KinUi.inputLayout(this, "站内信内容", true);
        TextInputEditText editText = KinUi.edit(inputLayout);
        new AlertDialog.Builder(this)
                .setTitle("发给 " + currentPost.createdByUsername)
                .setView(inputLayout)
                .setPositiveButton("发送", (dialog, which) -> repository.sendMessage(currentPost.createdByUsername, text(editText), new ApiCallback<>() {
                    @Override
                    public void onSuccess(com.example.kin.model.StationMessageModel data) {
                        setLoading(false, "消息已发送。");
                    }

                    @Override
                    public void onError(ApiException exception) {
                        setLoading(false, "发送失败：" + exception.getMessage());
                    }
                }))
                .setNegativeButton("取消", null)
                .show();
    }

    private void showReportDialog(String targetType, long targetId) {
        LinearLayout root = KinUi.vertical(this);
        TextInputLayout reasonLayout = KinUi.inputLayout(this, "举报原因类型", false);
        TextInputLayout detailLayout = KinUi.inputLayout(this, "补充说明", true);
        TextInputEditText reasonEdit = KinUi.edit(reasonLayout);
        TextInputEditText detailEdit = KinUi.edit(detailLayout);
        reasonEdit.setText("VIOLATION");
        root.addView(reasonLayout);
        root.addView(detailLayout);
        KinUi.margins(detailLayout, this, 0, 12, 0, 0);
        new AlertDialog.Builder(this)
                .setTitle("提交举报")
                .setView(root)
                .setPositiveButton("提交", (dialog, which) -> repository.createReport(targetType, targetId, text(reasonEdit), text(detailEdit), new ApiCallback<>() {
                    @Override
                    public void onSuccess(com.example.kin.model.ReportModel data) {
                        setLoading(false, "举报已提交。");
                    }

                    @Override
                    public void onError(ApiException exception) {
                        setLoading(false, "举报失败：" + exception.getMessage());
                    }
                }))
                .setNegativeButton("取消", null)
                .show();
    }

    private void showEditDialog() {
        if (currentPost == null) {
            return;
        }
        LinearLayout root = KinUi.vertical(this);
        TextInputLayout field1 = KinUi.inputLayout(this, "主字段", false);
        TextInputLayout field2 = KinUi.inputLayout(this, "辅助字段", false);
        TextInputLayout field3 = KinUi.inputLayout(this, "说明", true);
        TextInputEditText edit1 = KinUi.edit(field1);
        TextInputEditText edit2 = KinUi.edit(field2);
        TextInputEditText edit3 = KinUi.edit(field3);
        root.addView(field1);
        root.addView(field2);
        root.addView(field3);
        KinUi.margins(field2, this, 0, 10, 0, 0);
        KinUi.margins(field3, this, 0, 10, 0, 0);

        if ("PROP_SHARE".equals(currentPost.postType)) {
            field1.setHint("道具名称");
            field2.setHint("道具类型");
            field3.setHint("投掷方式/点位说明");
            edit1.setText(currentPost.propName);
            edit2.setText(currentPost.toolType);
            edit3.setText(currentPost.throwMethod);
        } else if ("TACTIC_SHARE".equals(currentPost.postType)) {
            field1.setHint("战术名称");
            field2.setHint("战术类型");
            field3.setHint("战术描述");
            edit1.setText(currentPost.tacticName);
            edit2.setText(currentPost.tacticType);
            edit3.setText(currentPost.tacticDescription);
        } else {
            field1.setHint("正文内容");
            edit1.setText(currentPost.content);
            field2.setVisibility(View.GONE);
            field3.setVisibility(View.GONE);
        }

        new AlertDialog.Builder(this)
                .setTitle("更新帖子")
                .setView(root)
                .setPositiveButton("提交", (dialog, which) -> submitEdit(edit1, edit2, edit3))
                .setNegativeButton("取消", null)
                .show();
    }

    private void submitEdit(TextInputEditText edit1, TextInputEditText edit2, TextInputEditText edit3) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("postType", currentPost.postType);
            if ("PROP_SHARE".equals(currentPost.postType)) {
                payload.put("mapName", currentPost.mapName);
                payload.put("propName", text(edit1));
                payload.put("toolType", text(edit2));
                payload.put("throwMethod", text(edit3));
                payload.put("propPosition", currentPost.propPosition);
                payload.put("stanceImageUrl", currentPost.stanceImageUrl);
                payload.put("aimImageUrl", currentPost.aimImageUrl);
                payload.put("landingImageUrl", currentPost.landingImageUrl);
            } else if ("TACTIC_SHARE".equals(currentPost.postType)) {
                payload.put("mapName", currentPost.mapName);
                payload.put("tacticName", text(edit1));
                payload.put("tacticType", text(edit2));
                payload.put("tacticDescription", text(edit3));
                payload.put("member1", currentPost.member1);
                payload.put("member1Role", currentPost.member1Role);
                payload.put("member2", currentPost.member2);
                payload.put("member2Role", currentPost.member2Role);
                payload.put("member3", currentPost.member3);
                payload.put("member3Role", currentPost.member3Role);
                payload.put("member4", currentPost.member4);
                payload.put("member4Role", currentPost.member4Role);
                payload.put("member5", currentPost.member5);
                payload.put("member5Role", currentPost.member5Role);
            } else {
                payload.put("content", text(edit1));
                payload.put("imageUrls", new JSONArray(currentPost.imageUrls));
            }
            repository.updatePost(postId, payload, new ApiCallback<>() {
                @Override
                public void onSuccess(ForumPostModel data) {
                    currentPost = data;
                    renderPost();
                    loadComments();
                    setLoading(false, "已提交更新，等待审核。");
                }

                @Override
                public void onError(ApiException exception) {
                    setLoading(false, "更新失败：" + exception.getMessage());
                }
            });
        } catch (Exception exception) {
            setLoading(false, "更新失败：" + exception.getMessage());
        }
    }

    private List<String> parseMentions(String content) {
        List<String> mentions = new ArrayList<>();
        if (TextUtils.isEmpty(content)) {
            return mentions;
        }
        for (String part : content.split("\\s+")) {
            if (part.startsWith("@") && part.length() > 1) {
                mentions.add(part.substring(1).replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", ""));
            }
        }
        return mentions;
    }

    private List<String> previewImages() {
        List<String> items = new ArrayList<>();
        if (!currentPost.imageUrls.isEmpty()) {
            items.addAll(currentPost.imageUrls);
        }
        if (!TextUtils.isEmpty(currentPost.stanceImageUrl)) {
            items.add(currentPost.stanceImageUrl);
        }
        if (!TextUtils.isEmpty(currentPost.aimImageUrl)) {
            items.add(currentPost.aimImageUrl);
        }
        if (!TextUtils.isEmpty(currentPost.landingImageUrl)) {
            items.add(currentPost.landingImageUrl);
        }
        return items;
    }

    private String buildSummary() {
        if ("PROP_SHARE".equals(currentPost.postType)) {
            return currentPost.mapName + " · " + currentPost.toolType + " · " + currentPost.propPosition + "\n" + currentPost.throwMethod;
        }
        if ("TACTIC_SHARE".equals(currentPost.postType)) {
            return currentPost.mapName + " · " + currentPost.tacticType + "\n" + currentPost.tacticDescription;
        }
        return currentPost.content;
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusView.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        statusView.setText(message);
    }

    private String text(TextInputEditText editText) {
        return String.valueOf(editText.getText()).trim();
    }
}
