package com.example.kin.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostDetailActivity extends AppCompatActivity {
    public static final String EXTRA_POST_ID = "extra_post_id";
    public static final String EXTRA_MINE = "extra_mine";

    private KinRepository repository;
    private RemoteImageLoader imageLoader;
    private long postId;
    private boolean mine;
    private ForumPostModel currentPost;
    private LinearLayout bodyLayout;
    private LinearLayout bodyCommentsLayout;
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
    private TextView imageCounterView;
    private TextView imageCaptionView;
    private boolean postFavorited;
    private boolean showingCommentsTab;
    private boolean tabAnimationReady;
    private boolean tabAnimationRunning;
    private boolean commentSortByTime;
    private long replyTargetCommentId;
    private int commentCount;
    private float swipeStartX;
    private float swipeStartY;
    private final List<DetailImage> detailImages = new ArrayList<>();
    private final List<ForumCommentModel> currentComments = new ArrayList<>();

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
        attachSwipeSwitch(bodyScroll);
        contentFrame.addView(bodyScroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        commentsScroll = new ScrollView(this);
        commentsLayout = KinUi.vertical(this);
        commentsLayout.setPadding(KinUi.dp(this, 16), KinUi.dp(this, 12), KinUi.dp(this, 16), KinUi.dp(this, 18));
        commentsScroll.addView(commentsLayout);
        commentsScroll.setVisibility(View.GONE);
        attachSwipeSwitch(commentsScroll);
        contentFrame.addView(commentsScroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(contentFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(buildBottomBar());
        setContentView(root);
        selectTab(false, false);
        loadAll();
    }

    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(KinUi.dp(this, 10), KinUi.dp(this, 8), KinUi.dp(this, 10), KinUi.dp(this, 4));

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_nav_back);
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

        ImageView share = new ImageView(this);
        share.setImageResource(android.R.drawable.ic_menu_share);
        share.setColorFilter(getColor(R.color.kin_text_muted));
        share.setPadding(KinUi.dp(this, 8), KinUi.dp(this, 8), KinUi.dp(this, 8), KinUi.dp(this, 8));
        share.setOnClickListener(v -> {
            if (currentPost != null) {
                showReportDialog("POST", currentPost.id);
            }
        });
        bar.addView(share, new LinearLayout.LayoutParams(KinUi.dp(this, 48), KinUi.dp(this, 48)));
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

    private void attachSwipeSwitch(View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                swipeStartX = event.getX();
                swipeStartY = event.getY();
                return false;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float dx = event.getX() - swipeStartX;
                float dy = event.getY() - swipeStartY;
                if (Math.abs(dx) > KinUi.dp(this, 72) && Math.abs(dx) > Math.abs(dy) * 1.4f) {
                    selectTab(dx < 0);
                    return true;
                }
            }
            return false;
        });
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
        repository.getComments(postId, commentSortByTime ? "TIME" : "HOT", new ApiCallback<>() {
            @Override
            public void onSuccess(List<ForumCommentModel> data) {
                currentComments.clear();
                currentComments.addAll(data);
                commentCount = data.size();
                renderComments(commentsLayout, data, true);
                renderComments(bodyCommentsLayout, data, false);
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
        buildDetailImages();
        if (!detailImages.isEmpty()) {
            bodyLayout.addView(buildHeroImages());
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

        View separator = new View(this);
        separator.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_stroke_dark : R.color.kin_stroke));
        LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                KinUi.dp(this, 6));
        separatorParams.setMargins(0, KinUi.dp(this, 12), 0, KinUi.dp(this, 14));
        bodyLayout.addView(separator, separatorParams);

        bodyCommentsLayout = KinUi.vertical(this);
        bodyCommentsLayout.setPadding(KinUi.dp(this, 16), 0, KinUi.dp(this, 16), KinUi.dp(this, 18));
        bodyLayout.addView(bodyCommentsLayout);
        renderComments(bodyCommentsLayout, currentComments, false);
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
        row.addView(avatar, new LinearLayout.LayoutParams(KinUi.dp(this, 56), KinUi.dp(this, 56)));

        LinearLayout info = KinUi.vertical(this);
        TextView name = KinUi.text(this, safeText(currentPost.createdByUsername, "用户"), 16, true);
        TextView level = KinUi.muted(this, "Lv." + Math.max(currentPost.authorLevel, 0), 13);
        info.addView(name);
        info.addView(level);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoParams.leftMargin = KinUi.dp(this, 14);
        row.addView(info, infoParams);
        return row;
    }

    private View buildHeroImages() {
        LinearLayout root = KinUi.vertical(this);
        FrameLayout imageFrame = new FrameLayout(this);
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = Math.min(KinUi.dp(this, 340), Math.max(KinUi.dp(this, 230), Math.round(width * 0.62f)));

        ViewPager2 pager = new ViewPager2(this);
        pager.setAdapter(new ImagePagerAdapter());
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateImageIndicator(position);
            }
        });
        imageFrame.addView(pager, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

        imageCounterView = KinUi.text(this, "1/" + detailImages.size(), 13, true);
        imageCounterView.setTextColor(getColor(R.color.kin_text_inverse));
        GradientDrawable counterBg = new GradientDrawable();
        counterBg.setColor(getColor(R.color.kin_overlay));
        counterBg.setCornerRadius(KinUi.dp(this, 12));
        imageCounterView.setBackground(counterBg);
        imageCounterView.setPadding(KinUi.dp(this, 10), KinUi.dp(this, 4), KinUi.dp(this, 10), KinUi.dp(this, 4));
        FrameLayout.LayoutParams counterParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        counterParams.setMargins(0, KinUi.dp(this, 12), KinUi.dp(this, 12), 0);
        imageFrame.addView(imageCounterView, counterParams);
        root.addView(imageFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

        if ("PROP_SHARE".equals(currentPost.postType)) {
            imageCaptionView = KinUi.text(this, "", 17, false);
            imageCaptionView.setGravity(Gravity.CENTER);
            imageCaptionView.setPadding(0, KinUi.dp(this, 10), 0, 0);
            root.addView(imageCaptionView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        updateImageIndicator(0);
        return root;
    }

    private void updateImageIndicator(int position) {
        if (imageCounterView != null) {
            imageCounterView.setText((position + 1) + "/" + detailImages.size());
        }
        if (imageCaptionView != null && position >= 0 && position < detailImages.size()) {
            imageCaptionView.setText(detailImages.get(position).caption);
            imageCaptionView.setVisibility(TextUtils.isEmpty(detailImages.get(position).caption) ? View.GONE : View.VISIBLE);
        }
    }

    private void buildDetailImages() {
        detailImages.clear();
        if ("PROP_SHARE".equals(currentPost.postType)) {
            addDetailImage(currentPost.stanceImageUrl, "站位图");
            addDetailImage(currentPost.aimImageUrl, "瞄点图");
            addDetailImage(currentPost.landingImageUrl, "落点图");
            for (String imageUrl : currentPost.imageUrls) {
                addDetailImage(imageUrl, "图片");
            }
            return;
        }
        for (String imageUrl : currentPost.imageUrls) {
            addDetailImage(imageUrl, "");
        }
        addDetailImage(currentPost.stanceImageUrl, "");
        addDetailImage(currentPost.aimImageUrl, "");
        addDetailImage(currentPost.landingImageUrl, "");
    }

    private void addDetailImage(String url, String caption) {
        if (!TextUtils.isEmpty(url)) {
            detailImages.add(new DetailImage(url, caption));
        }
    }

    private void renderComments(LinearLayout target, List<ForumCommentModel> comments, boolean standalone) {
        if (target == null) {
            return;
        }
        target.removeAllViews();
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(KinUi.text(this, "评论 " + comments.size(), 18, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView hotSort = sortText("热门", !commentSortByTime);
        TextView timeSort = sortText("时间", commentSortByTime);
        hotSort.setOnClickListener(v -> {
            if (commentSortByTime) {
                commentSortByTime = false;
                loadComments();
            }
        });
        timeSort.setOnClickListener(v -> {
            if (!commentSortByTime) {
                commentSortByTime = true;
                loadComments();
            }
        });
        header.addView(hotSort);
        header.addView(timeSort);
        target.addView(header);

        if (!standalone) {
            View thin = KinUi.divider(this);
            KinUi.margins(thin, this, 0, 12, 0, 12);
            target.addView(thin);
        }

        if (comments.isEmpty()) {
            TextView empty = KinUi.muted(this, "还没有评论。", 14);
            KinUi.margins(empty, this, 0, 12, 0, 0);
            target.addView(empty);
            return;
        }
        CommentBuckets buckets = groupComments(comments);
        for (ForumCommentModel comment : buckets.parents) {
            target.addView(buildCommentItem(comment, false));
            List<ForumCommentModel> replies = buckets.repliesByParent.get(comment.id);
            if (replies != null && !replies.isEmpty()) {
                target.addView(buildReplyGroup(replies));
            }
            View itemDivider = KinUi.divider(this);
            KinUi.margins(itemDivider, this, 0, 14, 0, 14);
            target.addView(itemDivider);
        }
    }

    private TextView sortText(String value, boolean selected) {
        TextView textView = selected ? KinUi.text(this, value, 14, true) : KinUi.muted(this, value, 14);
        textView.setPadding(KinUi.dp(this, 10), KinUi.dp(this, 4), 0, KinUi.dp(this, 4));
        return textView;
    }

    private CommentBuckets groupComments(List<ForumCommentModel> comments) {
        CommentBuckets buckets = new CommentBuckets();
        Map<Long, ForumCommentModel> byId = new LinkedHashMap<>();
        for (ForumCommentModel comment : comments) {
            byId.put(comment.id, comment);
        }
        for (ForumCommentModel comment : comments) {
            if (comment.parentCommentId > 0 && byId.containsKey(comment.parentCommentId)) {
                long parentId = rootParentId(comment, byId);
                buckets.repliesByParent
                        .computeIfAbsent(parentId, key -> new ArrayList<>())
                        .add(comment);
            } else {
                buckets.parents.add(comment);
            }
        }
        if (commentSortByTime) {
            buckets.parents.sort(Comparator.comparingInt((ForumCommentModel item) -> item.floorNumber).reversed());
        } else {
            buckets.parents.sort(Comparator
                    .comparingInt((ForumCommentModel item) -> item.likeCount)
                    .reversed()
                    .thenComparingInt(item -> item.floorNumber));
        }
        for (List<ForumCommentModel> replies : buckets.repliesByParent.values()) {
            replies.sort(Comparator.comparingInt(item -> item.floorNumber));
        }
        return buckets;
    }

    private long rootParentId(ForumCommentModel comment, Map<Long, ForumCommentModel> byId) {
        long parentId = comment.parentCommentId;
        ForumCommentModel parent = byId.get(parentId);
        while (parent != null && parent.parentCommentId > 0 && byId.containsKey(parent.parentCommentId)) {
            parentId = parent.parentCommentId;
            parent = byId.get(parentId);
        }
        return parentId;
    }

    private View buildReplyGroup(List<ForumCommentModel> replies) {
        LinearLayout group = KinUi.vertical(this);
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_panel_alt : R.color.kin_light_panel_alt));
        background.setCornerRadius(KinUi.dp(this, 8));
        group.setBackground(background);
        group.setPadding(KinUi.dp(this, 12), KinUi.dp(this, 8), KinUi.dp(this, 12), KinUi.dp(this, 8));
        KinUi.margins(group, this, 34, 10, 0, 0);
        for (ForumCommentModel reply : replies) {
            group.addView(buildCommentItem(reply, true));
        }
        return group;
    }

    private View buildCommentItem(ForumCommentModel comment, boolean childReply) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView avatar = KinUi.text(this, avatarText(comment.username), 13, true);
        avatar.setTextColor(getColor(R.color.kin_text_inverse));
        avatar.setGravity(Gravity.CENTER);
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(getColor(R.color.kin_chip_blue_text));
        avatar.setBackground(avatarBg);
        int avatarSize = childReply ? KinUi.dp(this, 18) : KinUi.dp(this, 21);
        row.addView(avatar, new LinearLayout.LayoutParams(avatarSize, avatarSize));

        LinearLayout body = KinUi.vertical(this);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bodyParams.leftMargin = KinUi.dp(this, 10);
        row.addView(body, bodyParams);

        TextView line = KinUi.text(this, buildCommentLine(comment), childReply ? 14 : 16, false);
        line.setLineSpacing(KinUi.dp(this, 3), 1f);
        body.addView(line);

        if (!TextUtils.isEmpty(comment.createdAt)) {
            TextView time = KinUi.muted(this, comment.createdAt, 12);
            KinUi.margins(time, this, 0, 4, 0, 0);
            body.addView(time);
        }

        if (!comment.imageUrls.isEmpty()) {
            View strip = KinUi.imageStrip(this, comment.imageUrls, imageLoader);
            KinUi.margins(strip, this, 0, 10, 0, 0);
            body.addView(strip);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        if (!childReply) {
            TextView like = actionText((comment.liked ? "已赞 " : "点赞 ") + comment.likeCount);
            like.setOnClickListener(v -> toggleCommentLike(comment));
            actions.addView(like);
        }
        TextView reply = actionText("回复");
        reply.setOnClickListener(v -> {
            replyTargetCommentId = comment.id;
            replyHintView.setText("正在回复 @" + comment.username);
            replyHintView.setVisibility(View.VISIBLE);
            commentEdit.requestFocus();
        });
        TextView report = actionText("举报");
        report.setOnClickListener(v -> showReportDialog("COMMENT", comment.id));
        actions.addView(reply);
        actions.addView(report);
        KinUi.margins(reply, this, childReply ? 0 : 18, 0, 0, 0);
        KinUi.margins(report, this, 18, 0, 0, 0);
        KinUi.margins(actions, this, 0, 10, 0, 0);
        body.addView(actions);
        return row;
    }

    private String buildCommentLine(ForumCommentModel comment) {
        String author = safeText(comment.username, "用户");
        String replyTo = cleanReplyUsername(comment.replyToUsername);
        String prefix = TextUtils.isEmpty(replyTo) ? author : author + "（回复 " + replyTo + "）";
        return prefix + "：" + safeText(comment.content, "");
    }

    private String cleanReplyUsername(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        String trimmed = value.trim();
        if ("null".equalsIgnoreCase(trimmed)) {
            return "";
        }
        return trimmed;
    }

    private void toggleCommentLike(ForumCommentModel comment) {
        ApiCallback<LikeStatusModel> callback = new ApiCallback<>() {
            @Override
            public void onSuccess(LikeStatusModel data) {
                comment.liked = data.liked;
                comment.likeCount = data.likeCount;
                renderComments(commentsLayout, currentComments, true);
                renderComments(bodyCommentsLayout, currentComments, false);
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "评论点赞失败：" + exception.getMessage());
            }
        };
        if (comment.liked) {
            repository.unlikeComment(comment.id, callback);
        } else {
            repository.likeComment(comment.id, callback);
        }
    }

    private TextView actionText(String value) {
        TextView textView = KinUi.muted(this, value, 13);
        textView.setPadding(0, KinUi.dp(this, 4), KinUi.dp(this, 8), KinUi.dp(this, 4));
        return textView;
    }

    private void selectTab(boolean comments) {
        selectTab(comments, true);
    }

    private void selectTab(boolean comments, boolean animate) {
        if (showingCommentsTab == comments && tabAnimationReady) {
            styleTab(bodyTab, !comments);
            styleTab(commentsTab, comments);
            return;
        }
        if (tabAnimationRunning) {
            return;
        }
        View outgoing = showingCommentsTab ? commentsScroll : bodyScroll;
        View incoming = comments ? commentsScroll : bodyScroll;
        boolean toRight = !comments;
        showingCommentsTab = comments;
        if (!animate || !tabAnimationReady || outgoing == null || incoming == null) {
            if (bodyScroll != null) {
                bodyScroll.setVisibility(comments ? View.GONE : View.VISIBLE);
                bodyScroll.setAlpha(comments ? 0f : 1f);
                bodyScroll.setTranslationX(0f);
            }
            if (commentsScroll != null) {
                commentsScroll.setVisibility(comments ? View.VISIBLE : View.GONE);
                commentsScroll.setAlpha(comments ? 1f : 0f);
                commentsScroll.setTranslationX(0f);
            }
            tabAnimationReady = true;
            styleTab(bodyTab, !comments);
            styleTab(commentsTab, comments);
            return;
        }
        styleTab(bodyTab, !comments);
        styleTab(commentsTab, comments);

        int distance = Math.max(getResources().getDisplayMetrics().widthPixels, 1);
        float incomingStart = toRight ? -distance : distance;
        float outgoingEnd = toRight ? distance : -distance;
        tabAnimationRunning = true;
        incoming.setVisibility(View.VISIBLE);
        incoming.setAlpha(0f);
        incoming.setTranslationX(incomingStart);
        incoming.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    incoming.setTranslationX(0f);
                    incoming.setAlpha(1f);
                })
                .start();
        outgoing.animate()
                .translationX(outgoingEnd)
                .alpha(0f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    outgoing.setVisibility(View.GONE);
                    outgoing.setTranslationX(0f);
                    outgoing.setAlpha(1f);
                    tabAnimationRunning = false;
                })
                .start();
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
                if (!showingCommentsTab) {
                    bodyScroll.post(() -> bodyScroll.smoothScrollTo(0, Math.max(0, bodyCommentsLayout.getTop() - KinUi.dp(PostDetailActivity.this, 12))));
                }
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
        favoriteActionLabel.setText(postFavorited ? "已收" : "收藏");
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

    private void openImagePreview(int index) {
        ArrayList<String> urls = new ArrayList<>();
        for (DetailImage detailImage : detailImages) {
            urls.add(detailImage.url);
        }
        Intent intent = new Intent(this, ImagePreviewActivity.class);
        intent.putStringArrayListExtra(ImagePreviewActivity.EXTRA_IMAGE_URLS, urls);
        intent.putExtra(ImagePreviewActivity.EXTRA_IMAGE_INDEX, index);
        startActivity(intent);
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

    private final class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageHolder> {
        @NonNull
        @Override
        public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(PostDetailActivity.this);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundColor(getColor(KinUi.isNight(PostDetailActivity.this) ? R.color.kin_dark_panel_alt : R.color.kin_light_panel_alt));
            return new ImageHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageHolder holder, int position) {
            DetailImage detailImage = detailImages.get(position);
            holder.imageView.setOnClickListener(v -> openImagePreview(position));
            imageLoader.load(holder.imageView, detailImage.url);
        }

        @Override
        public int getItemCount() {
            return detailImages.size();
        }

        final class ImageHolder extends RecyclerView.ViewHolder {
            final ImageView imageView;

            ImageHolder(@NonNull ImageView imageView) {
                super(imageView);
                this.imageView = imageView;
            }
        }
    }

    private static final class DetailImage {
        final String url;
        final String caption;

        DetailImage(String url, String caption) {
            this.url = url;
            this.caption = caption;
        }
    }

    private static final class CommentBuckets {
        final List<ForumCommentModel> parents = new ArrayList<>();
        final Map<Long, List<ForumCommentModel>> repliesByParent = new LinkedHashMap<>();
    }
}
