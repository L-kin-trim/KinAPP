package com.example.kin.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
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
import com.example.kin.ui.future.FutureFeatureDetailActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private ImageView likeActionIcon;
    private TextView likeActionLabel;
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
                setLoading(false, "");
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

        LinearLayout body = KinUi.sectionContainer(this, 18);
        body.addView(KinUi.text(this, currentPost.title, 22, true));
        TextView meta = KinUi.muted(this,
                currentPost.createdByUsername + " · " + currentPost.createdAt,
                13);
        KinUi.margins(meta, this, 0, 8, 0, 0);
        body.addView(meta);

        TextView summary = KinUi.muted(this, buildSummary(), 15);
        KinUi.margins(summary, this, 0, 12, 0, 0);
        body.addView(summary);

        List<String> images = previewImages();
        if (!images.isEmpty()) {
            View strip = buildPreviewImageStrip(images);
            KinUi.margins(strip, this, 0, 14, 0, 0);
            body.addView(strip);
        }

        View actions = buildPostActionIcons();
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

        contentLayout.addView(body);

        View divider = KinUi.divider(this);
        KinUi.margins(divider, this, 0, 18, 0, 18);
        contentLayout.addView(divider);

        contentLayout.addView(buildCommentComposer());

        commentsLayout = KinUi.vertical(this);
        KinUi.margins(commentsLayout, this, 0, 18, 0, 0);
        contentLayout.addView(commentsLayout);
    }

    private View buildPostActionIcons() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER);

        likeActionIcon = new ImageView(this);
        likeActionLabel = KinUi.muted(this, "", 12);
        View likeAction = buildIconAction(likeActionIcon, likeActionLabel, R.drawable.ic_action_like, "点赞");
        likeAction.setOnClickListener(v -> toggleLike());
        applyLikeActionState();

        ImageView favoriteIcon = new ImageView(this);
        TextView favoriteLabel = KinUi.muted(this, "收藏", 12);
        View favoriteAction = buildIconAction(favoriteIcon, favoriteLabel, R.drawable.ic_action_favorite, "收藏");
        favoriteAction.setOnClickListener(v -> repository.favoritePost(postId, new ApiCallback<>() {
            @Override
            public void onSuccess(com.example.kin.model.FavoriteStatus data) {
                favoriteLabel.setText(data.favorited ? "已收藏" : "收藏");
                favoriteIcon.setColorFilter(getColor(data.favorited ? R.color.kin_warning : R.color.kin_text_muted));
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "收藏失败：" + exception.getMessage());
            }
        }));

        ImageView reportIcon = new ImageView(this);
        TextView reportLabel = KinUi.muted(this, "举报", 12);
        View reportAction = buildIconAction(reportIcon, reportLabel, R.drawable.ic_action_report, "举报");
        reportAction.setOnClickListener(v -> showReportDialog("POST", currentPost.id));

        row.addView(likeAction, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(favoriteAction, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(reportAction, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private View buildIconAction(ImageView icon, TextView label, int iconResId, String contentDescription) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(android.view.Gravity.CENTER);
        container.setClickable(true);
        container.setFocusable(true);
        container.setContentDescription(contentDescription);
        container.setPadding(KinUi.dp(this, 8), KinUi.dp(this, 6), KinUi.dp(this, 8), KinUi.dp(this, 6));

        icon.setImageResource(iconResId);
        icon.setColorFilter(getColor(R.color.kin_text_muted));
        container.addView(icon, new LinearLayout.LayoutParams(KinUi.dp(this, 28), KinUi.dp(this, 28)));
        KinUi.margins(label, this, 0, 4, 0, 0);
        label.setGravity(android.view.Gravity.CENTER);
        container.addView(label);
        return container;
    }

    private void applyLikeActionState() {
        if (likeActionIcon == null || likeActionLabel == null || currentPost == null) {
            return;
        }
        likeActionLabel.setText((currentPost.liked ? "已赞 " : "点赞 ") + currentPost.likeCount);
        likeActionIcon.setColorFilter(getColor(currentPost.liked ? R.color.kin_danger : R.color.kin_text_muted));
    }

    private View buildPreviewImageStrip(List<String> urls) {
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, KinUi.dp(this, 8), 0, KinUi.dp(this, 8));
        scrollView.addView(row);
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            if (TextUtils.isEmpty(url)) {
                continue;
            }
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(KinUi.dp(this, 160), KinUi.dp(this, 112));
            params.rightMargin = KinUi.dp(this, 10);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_panel_alt : R.color.kin_light_panel_alt));
            imageView.setClipToOutline(true);
            imageView.setClickable(true);
            imageView.setFocusable(true);
            int index = i;
            imageView.setOnClickListener(v -> openImagePreview(urls, index));
            imageLoader.load(imageView, url);
            row.addView(imageView);
        }
        return scrollView;
    }

    private void openImagePreview(List<String> urls, int index) {
        Intent intent = new Intent(this, ImagePreviewActivity.class);
        intent.putStringArrayListExtra(ImagePreviewActivity.EXTRA_IMAGE_URLS, new ArrayList<>(urls));
        intent.putExtra(ImagePreviewActivity.EXTRA_IMAGE_INDEX, index);
        startActivity(intent);
    }

    private View buildPostUpgradeCard() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        body.addView(KinUi.text(this, "帖子详情工具", 18, true));
        body.addView(KinUi.muted(this, "版本历史、阅读进度、相似推荐、投票、分享海报和评论总结集中在详情页使用。", 14));
        LinearLayout row = KinUi.buttonRow(this,
                featureButton("版本历史", "content.version_history"),
                featureButton("相似推荐", "community.similar_content"),
                featureButton("评论总结", "ai.comment_summary"));
        KinUi.margins(row, this, 0, 12, 0, 0);
        body.addView(row);
        LinearLayout row2 = KinUi.buttonRow(this,
                featureButton("阅读进度", "community.reading_progress"),
                featureButton("投票", "community.poll_post"),
                featureButton("分享海报", "community.share_poster"));
        KinUi.margins(row2, this, 0, 10, 0, 0);
        body.addView(row2);
        card.addView(body);
        return card;
    }

    private MaterialButton featureButton(String label, String featureKey) {
        MaterialButton button = KinUi.outlinedButton(this, label);
        button.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, FutureFeatureDetailActivity.class);
            intent.putExtra(FutureFeatureDetailActivity.EXTRA_FEATURE_KEY, featureKey);
            startActivity(intent);
        });
        return button;
    }

    private View buildCommentComposer() {
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
        imageRow.addView(commentImageState, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        imageRow.addView(pickButton, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        KinUi.margins(imageRow, this, 0, 12, 0, 0);
        body.addView(imageRow);

        MaterialButton sendButton = KinUi.filledButton(this, "提交评论");
        sendButton.setOnClickListener(v -> submitComment());
        KinUi.margins(sendButton, this, 0, 14, 0, 0);
        body.addView(sendButton);

        return body;
    }

    private void renderComments(List<ForumCommentModel> comments) {
        commentsLayout.removeAllViews();
        if (comments.isEmpty()) {
            commentsLayout.addView(KinUi.muted(this, "还没有评论。", 14));
            return;
        }
        for (ForumCommentModel comment : comments) {
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

            MaterialButton replyButton = KinUi.outlinedButton(this, "回复");
            replyButton.setOnClickListener(v -> {
                replyTargetCommentId = comment.id;
                replyHintView.setVisibility(View.VISIBLE);
                replyHintView.setText("正在回复 @" + comment.username);
                commentEdit.requestFocus();
            });
            MaterialButton reportButton = KinUi.outlinedButton(this, "举报");
            reportButton.setOnClickListener(v -> showReportDialog("COMMENT", comment.id));
            LinearLayout actions = KinUi.buttonRow(this, replyButton, reportButton);
            KinUi.margins(actions, this, 0, 12, 0, 0);
            body.addView(actions);
            commentsLayout.addView(body);
            View divider = KinUi.divider(this);
            KinUi.margins(divider, this, 0, 4, 0, 4);
            commentsLayout.addView(divider);
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
                applyLikeActionState();
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
        reasonEdit.setText("违规");
        root.addView(reasonLayout);
        root.addView(detailLayout);
        KinUi.margins(detailLayout, this, 0, 12, 0, 0);
        new AlertDialog.Builder(this)
                .setTitle("提交举报")
                .setView(root)
                .setPositiveButton("提交", (dialog, which) -> repository.createReport(targetType, targetId, normalizeReportReason(text(reasonEdit)), text(detailEdit), new ApiCallback<>() {
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
            return currentPost.mapName + " · " + translateToolType(currentPost.toolType) + " · " + currentPost.propPosition + "\n" + currentPost.throwMethod;
        }
        if ("TACTIC_SHARE".equals(currentPost.postType)) {
            return currentPost.mapName + " · " + currentPost.tacticType + "\n" + currentPost.tacticDescription;
        }
        return currentPost.content;
    }

    private String translateToolType(String toolType) {
        if (TextUtils.isEmpty(toolType)) {
            return "";
        }
        switch (toolType) {
            case "SMOKE":
            case "SMOKE_GRENADE":
                return "烟雾弹";
            case "FLASH":
            case "FLASHBANG":
                return "闪光弹";
            case "HE":
            case "HE_GRENADE":
                return "高爆手雷";
            case "MOLOTOV":
                return "燃烧瓶";
            case "INCENDIARY":
            case "INCENDIARY_GRENADE":
                return "燃烧弹";
            case "DECOY":
            case "DECOY_GRENADE":
                return "诱饵弹";
            default:
                return toolType;
        }
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusView.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        statusView.setText(message);
    }

    private String text(TextInputEditText editText) {
        return String.valueOf(editText.getText()).trim();
    }

    private String normalizeReportReason(String rawReason) {
        if (TextUtils.isEmpty(rawReason)) {
            return "VIOLATION";
        }
        String reason = rawReason.trim();
        String upper = reason.toUpperCase(Locale.ROOT);
        if (upper.matches("[A-Z_]+")) {
            return upper;
        }
        if (reason.contains("广告") || reason.contains("引流")) {
            return "SPAM";
        }
        if (reason.contains("辱骂") || reason.contains("骚扰")) {
            return "ABUSE";
        }
        return "VIOLATION";
    }
}
