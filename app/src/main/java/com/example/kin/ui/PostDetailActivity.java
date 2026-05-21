package com.example.kin.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.R;
import com.example.kin.data.KinRepository;
import com.example.kin.model.ForumCommentModel;
import com.example.kin.model.ForumPostModel;
import com.example.kin.model.LikeStatusModel;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.KinUi;
import com.example.kin.ui.common.RemoteImageLoader;
import com.google.android.material.button.MaterialButton;
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
    private LinearLayout bodyLayout;
    private LinearLayout commentsLayout;
    private ScrollView bodyScroll;
    private ScrollView commentsScroll;
    private ProgressBar progressBar;
    private TextView statusView;
    private TextInputEditText commentEdit;
    private TextView replyHintView;
    private TextView bodyTab;
    private TextView commentsTab;
    private ImageView likeActionIcon;
    private TextView likeActionLabel;
    private ImageView favoriteActionIcon;
    private TextView favoriteActionLabel;
    private TextView commentActionLabel;
    private boolean postFavorited;
    private long replyTargetCommentId;
    private int commentCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KinRepository(this);
        imageLoader = new RemoteImageLoader();
        postId = getIntent().getLongExtra(EXTRA_POST_ID, 0L);
        mine = getIntent().getBooleanExtra(EXTRA_MINE, false);

        LinearLayout root = KinUi.vertical(this);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));
        root.addView(buildTopBar());
        root.addView(buildStatusRow());

        FrameLayout contentFrame = new FrameLayout(this);
        bodyScroll = new ScrollView(this);
        bodyLayout = KinUi.vertical(this);
        bodyLayout.setPadding(0, 0, 0, KinUi.dp(this, 18));
        bodyScroll.addView(bodyLayout);
        contentFrame.addView(bodyScroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        commentsScroll = new ScrollView(this);
        commentsLayout = KinUi.vertical(this);
        commentsLayout.setPadding(KinUi.dp(this, 16), KinUi.dp(this, 12), KinUi.dp(this, 16), KinUi.dp(this, 18));
        commentsScroll.addView(commentsLayout);
        commentsScroll.setVisibility(View.GONE);
        contentFrame.addView(commentsScroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(contentFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(buildBottomBar());
        setContentView(root);
        selectTab(false);
        loadAll();
    }

    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(KinUi.dp(this, 10), KinUi.dp(this, 8), KinUi.dp(this, 10), KinUi.dp(this, 4));

        ImageView back = new ImageView(this);
        back.setImageResource(android.R.drawable.ic_media_previous);
        back.setColorFilter(KinUi.color(this, com.google.android.material.R.attr.colorOnSurface));
        back.setPadding(KinUi.dp(this, 8), KinUi.dp(this, 8), KinUi.dp(this, 8), KinUi.dp(this, 8));
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(KinUi.dp(this, 48), KinUi.dp(this, 48)));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        bodyTab = tabText("正文");
        commentsTab = tabText("评论");
        bodyTab.setOnClickListener(v -> selectTab(false));
        commentsTab.setOnClickListener(v -> selectTab(true));
        tabs.addView(bodyTab);
        tabs.addView(commentsTab);
        bar.addView(tabs, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView report = new ImageView(this);
        report.setImageResource(android.R.drawable.ic_menu_share);
        report.setColorFilter(KinUi.color(this, com.google.android.material.R.attr.colorOnSurface));
        report.setPadding(KinUi.dp(this, 8), KinUi.dp(this, 8), KinUi.dp(this, 8), KinUi.dp(this, 8));
        report.setOnClickListener(v -> {
            if (currentPost != null) {
                showReportDialog("POST", currentPost.id);
            }
        });
        bar.addView(report, new LinearLayout.LayoutParams(KinUi.dp(this, 48), KinUi.dp(this, 48)));
        return bar;
    }

    private TextView tabText(String value) {
        TextView textView = KinUi.text(this, value, 17, true);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(KinUi.dp(this, 22), KinUi.dp(this, 8), KinUi.dp(this, 22), KinUi.dp(this, 8));
        return textView;
    }

    private View buildStatusRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(KinUi.dp(this, 16), 0, KinUi.dp(this, 16), 0);
        progressBar = new ProgressBar(this);
        statusView = KinUi.muted(this, "", 13);
        statusView.setVisibility(View.GONE);
        row.addView(progressBar, new LinearLayout.LayoutParams(KinUi.dp(this, 32), KinUi.dp(this, 32)));
        row.addView(statusView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private View buildBottomBar() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(KinUi.dp(this, 14), KinUi.dp(this, 8), KinUi.dp(this, 14), KinUi.dp(this, 8));
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_panel : R.color.kin_light_panel));

        replyHintView = KinUi.muted(this, "", 12);
        replyHintView.setVisibility(View.GONE);
        root.addView(replyHintView);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        commentEdit = new TextInputEditText(this);
        commentEdit.setSingleLine(true);
        commentEdit.setHint("来说点什么吧!");
        commentEdit.setTextSize(15);
        commentEdit.setImeOptions(EditorInfo.IME_ACTION_SEND);
        commentEdit.setPadding(KinUi.dp(this, 12), 0, KinUi.dp(this, 12), 0);
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_panel_alt : R.color.kin_light_panel_alt));
        inputBg.setCornerRadius(KinUi.dp(this, 10));
        commentEdit.setBackground(inputBg);
        commentEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitComment();
                return true;
            }
            return false;
        });
        row.addView(commentEdit, new LinearLayout.LayoutParams(0, KinUi.dp(this, 44), 1f));

        likeActionIcon = new ImageView(this);
        likeActionLabel = KinUi.muted(this, "0", 12);
        row.addView(bottomIcon(likeActionIcon, likeActionLabel, R.drawable.ic_action_like, v -> toggleLike()));

        favoriteActionIcon = new ImageView(this);
        favoriteActionLabel = KinUi.muted(this, "收藏", 12);
        row.addView(bottomIcon(favoriteActionIcon, favoriteActionLabel, R.drawable.ic_action_favorite, v -> toggleFavorite()));

        ImageView commentIcon = new ImageView(this);
        commentActionLabel = KinUi.muted(this, "0", 12);
        row.addView(bottomIcon(commentIcon, commentActionLabel, R.drawable.ic_action_comment, v -> {
            selectTab(true);
            commentEdit.requestFocus();
        }));

        root.addView(row);
        return root;
    }

    private View bottomIcon(ImageView icon, TextView label, int iconRes, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(KinUi.dp(this, 10), 0, 0, 0);
        item.setOnClickListener(listener);
        icon.setImageResource(iconRes);
        icon.setColorFilter(getColor(R.color.kin_text_muted));
        item.addView(icon, new LinearLayout.LayoutParams(KinUi.dp(this, 28), KinUi.dp(this, 28)));
        label.setGravity(Gravity.CENTER);
        item.addView(label);
        return item;
    }

    private void loadAll() {
        setLoading(true, "正在加载帖子...");
        repository.getPostDetail(postId, mine, new ApiCallback<>() {
            @Override
            public void onSuccess(ForumPostModel data) {
                currentPost = data;
                renderPost();
                loadInteractionStatus();
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
                commentCount = data.size();
                renderComments(data);
                applyCommentCount();
                setLoading(false, "");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "评论加载失败：" + exception.getMessage());
            }
        });
    }

    private void renderPost() {
        bodyLayout.removeAllViews();
        List<String> images = previewImages();
        if (!images.isEmpty()) {
            bodyLayout.addView(buildHeroImages(images));
        }

        LinearLayout body = KinUi.sectionContainer(this, 18);
        body.addView(buildAuthorRow());
        TextView title = KinUi.text(this, safeText(currentPost.title, "未命名帖子"), 24, true);
        KinUi.margins(title, this, 0, 14, 0, 0);
        body.addView(title);

        TextView summary = KinUi.text(this, buildSummary(), 17, false);
        summary.setLineSpacing(KinUi.dp(this, 4), 1f);
        KinUi.margins(summary, this, 0, 12, 0, 0);
        body.addView(summary);

        TextView meta = KinUi.muted(this, buildMeta(), 13);
        KinUi.margins(meta, this, 0, 18, 0, 0);
        body.addView(meta);

        if (currentPost.canEdit || mine) {
            MaterialButton editButton = KinUi.outlinedButton(this, "更新帖子");
            editButton.setOnClickListener(v -> showEditDialog());
            KinUi.margins(editButton, this, 0, 16, 0, 0);
            body.addView(editButton);
        }
        if (currentPost.canWithdraw || mine) {
            MaterialButton withdrawButton = KinUi.outlinedButton(this, "撤回帖子");
            withdrawButton.setOnClickListener(v -> withdrawPost());
            KinUi.margins(withdrawButton, this, 0, 10, 0, 0);
            body.addView(withdrawButton);
        }

        bodyLayout.addView(body);
        applyLikeActionState();
        applyFavoriteActionState();
    }

    private View buildAuthorRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView avatar = KinUi.text(this, avatarText(currentPost.createdByUsername), 16, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTextColor(getColor(R.color.kin_text_inverse));
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(getColor(R.color.kin_accent));
        avatar.setBackground(avatarBg);
        row.addView(avatar, new LinearLayout.LayoutParams(KinUi.dp(this, 46), KinUi.dp(this, 46)));

        LinearLayout info = KinUi.vertical(this);
        TextView name = KinUi.text(this, safeText(currentPost.createdByUsername, "用户"), 16, true);
        TextView level = KinUi.muted(this, "Lv." + Math.max(currentPost.authorLevel, 0), 12);
        info.addView(name);
        info.addView(level);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoParams.leftMargin = KinUi.dp(this, 12);
        row.addView(info, infoParams);
        return row;
    }

    private View buildHeroImages(List<String> urls) {
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(row);
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = Math.min(KinUi.dp(this, 320), Math.max(KinUi.dp(this, 220), Math.round(width * 0.62f)));
        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            if (TextUtils.isEmpty(url)) {
                continue;
            }
            ImageView imageView = new ImageView(this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_panel_alt : R.color.kin_light_panel_alt));
            int index = i;
            imageView.setOnClickListener(v -> openImagePreview(urls, index));
            row.addView(imageView, new LinearLayout.LayoutParams(width, height));
            imageLoader.load(imageView, url);
        }
        return scrollView;
    }

    private void renderComments(List<ForumCommentModel> comments) {
        commentsLayout.removeAllViews();
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(KinUi.text(this, "评论 " + comments.size(), 18, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(KinUi.muted(this, "热门", 14));
        commentsLayout.addView(header);
        View divider = KinUi.divider(this);
        KinUi.margins(divider, this, 0, 12, 0, 12);
        commentsLayout.addView(divider);

        if (comments.isEmpty()) {
            commentsLayout.addView(KinUi.muted(this, "还没有评论。", 14));
            return;
        }
        for (ForumCommentModel comment : comments) {
            commentsLayout.addView(buildCommentItem(comment));
            View itemDivider = KinUi.divider(this);
            KinUi.margins(itemDivider, this, 0, 14, 0, 14);
            commentsLayout.addView(itemDivider);
        }
    }

    private View buildCommentItem(ForumCommentModel comment) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView avatar = KinUi.text(this, avatarText(comment.username), 13, true);
        avatar.setTextColor(getColor(R.color.kin_text_inverse));
        avatar.setGravity(Gravity.CENTER);
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(getColor(R.color.kin_chip_blue_text));
        avatar.setBackground(avatarBg);
        row.addView(avatar, new LinearLayout.LayoutParams(KinUi.dp(this, 42), KinUi.dp(this, 42)));

        LinearLayout body = KinUi.vertical(this);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bodyParams.leftMargin = KinUi.dp(this, 12);
        row.addView(body, bodyParams);

        body.addView(KinUi.text(this, safeText(comment.username, "用户"), 16, true));
        TextView meta = KinUi.muted(this,
                safeText(comment.createdAt, "") + (TextUtils.isEmpty(comment.replyToUsername) ? "" : " · 回复 " + comment.replyToUsername),
                12);
        KinUi.margins(meta, this, 0, 4, 0, 0);
        body.addView(meta);

        TextView content = KinUi.text(this, safeText(comment.content, ""), 16, false);
        content.setLineSpacing(KinUi.dp(this, 3), 1f);
        KinUi.margins(content, this, 0, 10, 0, 0);
        body.addView(content);

        if (!comment.imageUrls.isEmpty()) {
            View strip = KinUi.imageStrip(this, comment.imageUrls, imageLoader);
            KinUi.margins(strip, this, 0, 10, 0, 0);
            body.addView(strip);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView reply = actionText("回复");
        reply.setOnClickListener(v -> {
            replyTargetCommentId = comment.id;
            replyHintView.setText("正在回复 @" + comment.username);
            replyHintView.setVisibility(View.VISIBLE);
            selectTab(true);
            commentEdit.requestFocus();
        });
        TextView report = actionText("举报");
        report.setOnClickListener(v -> showReportDialog("COMMENT", comment.id));
        actions.addView(reply);
        actions.addView(report);
        KinUi.margins(report, this, 18, 0, 0, 0);
        KinUi.margins(actions, this, 0, 10, 0, 0);
        body.addView(actions);
        return row;
    }

    private TextView actionText(String value) {
        TextView textView = KinUi.muted(this, value, 13);
        textView.setPadding(0, KinUi.dp(this, 4), KinUi.dp(this, 8), KinUi.dp(this, 4));
        return textView;
    }

    private void selectTab(boolean comments) {
        if (bodyScroll != null) {
            bodyScroll.setVisibility(comments ? View.GONE : View.VISIBLE);
        }
        if (commentsScroll != null) {
            commentsScroll.setVisibility(comments ? View.VISIBLE : View.GONE);
        }
        styleTab(bodyTab, !comments);
        styleTab(commentsTab, comments);
    }

    private void styleTab(TextView tab, boolean selected) {
        if (tab == null) {
            return;
        }
        tab.setTextColor(KinUi.color(this, selected ? com.google.android.material.R.attr.colorOnSurface : com.google.android.material.R.attr.colorOnSurfaceVariant));
        tab.setTypeface(Typeface.create("sans-serif-medium", selected ? Typeface.BOLD : Typeface.NORMAL));
    }

    private void submitComment() {
        if (!repository.getSessionManager().isLoggedIn()) {
            setLoading(false, "请先登录再评论。");
            return;
        }
        String content = text(commentEdit);
        if (TextUtils.isEmpty(content)) {
            setLoading(false, "评论内容不能为空。");
            return;
        }
        setLoading(true, "正在提交评论...");
        repository.createComment(postId, content, replyTargetCommentId, parseMentions(content), new ArrayList<>(), new ApiCallback<>() {
            @Override
            public void onSuccess(ForumCommentModel data) {
                commentEdit.setText("");
                replyTargetCommentId = 0L;
                replyHintView.setVisibility(View.GONE);
                selectTab(true);
                loadComments();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "评论失败：" + exception.getMessage());
            }
        });
    }

    private void toggleLike() {
        if (currentPost == null) {
            return;
        }
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

    private void loadInteractionStatus() {
        repository.getLikeStatus(postId, new ApiCallback<>() {
            @Override
            public void onSuccess(LikeStatusModel data) {
                if (currentPost == null) {
                    return;
                }
                currentPost.liked = data.liked;
                currentPost.likeCount = data.likeCount;
                applyLikeActionState();
            }

            @Override
            public void onError(ApiException exception) {
            }
        });
        repository.getFavoriteStatus(postId, new ApiCallback<>() {
            @Override
            public void onSuccess(com.example.kin.model.FavoriteStatus data) {
                postFavorited = data.favorited;
                applyFavoriteActionState();
            }

            @Override
            public void onError(ApiException exception) {
            }
        });
    }

    private void toggleFavorite() {
        if (currentPost == null) {
            return;
        }
        ApiCallback<com.example.kin.model.FavoriteStatus> callback = new ApiCallback<>() {
            @Override
            public void onSuccess(com.example.kin.model.FavoriteStatus data) {
                postFavorited = data.favorited;
                applyFavoriteActionState();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "收藏失败：" + exception.getMessage());
            }
        };
        if (postFavorited) {
            repository.unfavoritePost(postId, callback);
        } else {
            repository.favoritePost(postId, callback);
        }
    }

    private void applyLikeActionState() {
        if (likeActionIcon == null || likeActionLabel == null || currentPost == null) {
            return;
        }
        likeActionLabel.setText(String.valueOf(currentPost.likeCount));
        likeActionIcon.setColorFilter(getColor(currentPost.liked ? R.color.kin_danger : R.color.kin_text_muted));
    }

    private void applyFavoriteActionState() {
        if (favoriteActionIcon == null || favoriteActionLabel == null) {
            return;
        }
        favoriteActionLabel.setText(postFavorited ? "已收藏" : "收藏");
        favoriteActionIcon.setColorFilter(getColor(postFavorited ? R.color.kin_warning : R.color.kin_text_muted));
    }

    private void applyCommentCount() {
        if (commentActionLabel != null) {
            commentActionLabel.setText(String.valueOf(commentCount));
        }
    }

    private void withdrawPost() {
        repository.withdrawPost(postId, new ApiCallback<>() {
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
        });
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

    private void openImagePreview(List<String> urls, int index) {
        Intent intent = new Intent(this, ImagePreviewActivity.class);
        intent.putStringArrayListExtra(ImagePreviewActivity.EXTRA_IMAGE_URLS, new ArrayList<>(urls));
        intent.putExtra(ImagePreviewActivity.EXTRA_IMAGE_INDEX, index);
        startActivity(intent);
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
            return joinNonEmpty(" · ", currentPost.mapName, translateToolType(currentPost.toolType), currentPost.propPosition)
                    + "\n" + safeText(currentPost.throwMethod, "");
        }
        if ("TACTIC_SHARE".equals(currentPost.postType)) {
            return joinNonEmpty(" · ", currentPost.mapName, currentPost.tacticType)
                    + "\n" + safeText(currentPost.tacticDescription, "");
        }
        return safeText(currentPost.content, "暂无正文");
    }

    private String buildMeta() {
        return joinNonEmpty(" · ", currentPost.createdAt, currentPost.mapName, translateType(currentPost.postType));
    }

    private String translateType(String postType) {
        if ("TACTIC_SHARE".equals(postType)) {
            return "战术";
        }
        if ("DAILY_CHAT".equals(postType)) {
            return "日常";
        }
        return "道具";
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

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusView.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        statusView.setText(message);
    }

    private String text(TextInputEditText editText) {
        return editText == null ? "" : String.valueOf(editText.getText()).trim();
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

    private String joinNonEmpty(String delimiter, String... values) {
        List<String> items = new ArrayList<>();
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                items.add(value);
            }
        }
        return TextUtils.join(delimiter, items);
    }

    private String safeText(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private String avatarText(String value) {
        if (TextUtils.isEmpty(value)) {
            return "用";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
