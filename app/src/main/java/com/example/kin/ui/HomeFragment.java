package com.example.kin.ui;

import android.graphics.drawable.GradientDrawable;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.kin.MainActivity;
import com.example.kin.R;
import com.example.kin.model.ForumPostModel;
import com.example.kin.model.HotKeywordModel;
import com.example.kin.model.LikeStatusModel;
import com.example.kin.model.PageResult;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.BasePageFragment;
import com.example.kin.ui.common.KinUi;
import com.example.kin.ui.common.LevelVisuals;
import com.example.kin.ui.common.RemoteImageLoader;
import com.example.kin.util.JsonUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends BasePageFragment {
    private static final String SEARCH_PREFS = "home_search_history";
    private static final String SEARCH_HISTORY_KEY = "terms";
    private static final int MAX_SEARCH_HISTORY = 8;

    private final List<ForumPostModel> posts = new ArrayList<>();
    private LinearLayout listContainer;
    private LinearLayout hotKeywordLayout;
    private MaterialButton loadMoreButton;
    private String currentType = "";
    private String keyword = "";
    private String mapName = "";
    private String author = "";
    private String sortType = "LATEST";
    private boolean exactSearch = false;
    private int currentPage = 0;
    private boolean lastPage = false;

    @Override
    protected void onPageReady() {
        MainActivity activity = (MainActivity) requireActivity();
        activity.setTopBar("首页", "");
        contentLayout.setPadding(KinUi.dp(activity, 12), KinUi.dp(activity, 10), KinUi.dp(activity, 12), KinUi.dp(activity, 24));

        listContainer = KinUi.vertical(activity);
        contentLayout.addView(listContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        loadMoreButton = KinUi.outlinedButton(activity, "加载更多");
        loadMoreButton.setOnClickListener(v -> loadPosts(false));
        contentLayout.addView(loadMoreButton);
        loadPosts(true);
    }

    @Override
    protected void onRefreshRequested() {
        loadPosts(true);
    }

    public void openSearch() {
        android.content.Intent intent = new android.content.Intent(requireContext(), SearchActivity.class);
        intent.putExtra(SearchActivity.EXTRA_MODE, SearchActivity.MODE_HOME);
        startActivity(intent);
    }

    private View buildFutureCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 18);
        body.addView(KinUi.text(activity, "社区浏览能力", 19, true));
        TextView subtitle = KinUi.muted(activity, "推荐流、关注动态、热榜、话题、新手专区、问答与相似内容按首页场景提供。", 14);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);
        LinearLayout row;
        MaterialButton recommendButton = KinUi.filledButton(activity, "个性推荐");
        recommendButton.setOnClickListener(v -> openFutureFeature("community.recommend_feed"));
        MaterialButton hotButton = KinUi.outlinedButton(activity, "热榜趋势");
        hotButton.setOnClickListener(v -> openFutureFeature("community.trending_rank"));
        MaterialButton beginnerButton = KinUi.outlinedButton(activity, "新手专区");
        beginnerButton.setOnClickListener(v -> openFutureFeature("community.beginner_zone"));
        row = KinUi.buttonRow(activity, recommendButton, hotButton, beginnerButton);
        KinUi.margins(row, activity, 0, 14, 0, 0);
        body.addView(row);
        card.addView(body);
        return card;
    }

    private void openFutureFeature(String featureKey) {
        MainActivity activity = (MainActivity) requireActivity();
        android.content.Intent intent = new android.content.Intent(activity, com.example.kin.ui.future.FutureFeatureDetailActivity.class);
        intent.putExtra(com.example.kin.ui.future.FutureFeatureDetailActivity.EXTRA_FEATURE_KEY, featureKey);
        startActivity(intent);
    }

    private View buildHeroCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 18);
        TextView title = KinUi.text(activity, "CS2 道具与战术社区", 22, true);
        TextView subtitle = KinUi.muted(activity, "按地图、作者、关键词检索帖子，支持点赞、收藏、举报与详情复盘。", 14);
        MaterialButton searchButton = KinUi.filledButton(activity, "搜索帖子");
        searchButton.setOnClickListener(v -> showSearchDialog());
        MaterialButton publishButton = KinUi.outlinedButton(activity, "去发布");
        publishButton.setOnClickListener(v -> activity.switchToPublish());

        LinearLayout actions = KinUi.buttonRow(activity, searchButton, publishButton);

        body.addView(title);
        KinUi.margins(subtitle, activity, 0, 8, 0, 14);
        body.addView(subtitle);
        body.addView(actions);
        card.addView(body);
        return card;
    }

    private View buildFilterRow(MainActivity activity) {
        LinearLayout root = KinUi.vertical(activity);

        LinearLayout typeRow = new LinearLayout(activity);
        typeRow.setOrientation(LinearLayout.HORIZONTAL);
        typeRow.addView(filterChip(activity, "全部", ""));
        typeRow.addView(filterChip(activity, "道具", "PROP_SHARE"));
        typeRow.addView(filterChip(activity, "战术", "TACTIC_SHARE"));
        typeRow.addView(filterChip(activity, "日常", "DAILY_CHAT"));

        LinearLayout sortRow = new LinearLayout(activity);
        sortRow.setOrientation(LinearLayout.HORIZONTAL);
        KinUi.margins(sortRow, activity, 0, 10, 0, 14);
        sortRow.addView(sortChip(activity, "最新", "LATEST"));
        sortRow.addView(sortChip(activity, "热门", "HOT"));
        sortRow.addView(sortChip(activity, "最多收藏", "MOST_FAVORITE"));

        root.addView(typeRow);
        root.addView(sortRow);
        return root;
    }

    private View buildHotKeywordsCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 16);
        body.addView(KinUi.text(activity, "热门关键词", 18, true));
        hotKeywordLayout = new LinearLayout(activity);
        hotKeywordLayout.setOrientation(LinearLayout.HORIZONTAL);
        KinUi.margins(hotKeywordLayout, activity, 0, 12, 0, 0);
        body.addView(hotKeywordLayout);
        card.addView(body);
        return card;
    }

    private Chip filterChip(MainActivity activity, String label, String type) {
        Chip chip = KinUi.chip(activity, label);
        chip.setCheckable(true);
        chip.setOnClickListener(v -> {
            currentType = type;
            loadPosts(true);
        });
        return chip;
    }

    private Chip sortChip(MainActivity activity, String label, String value) {
        Chip chip = KinUi.chip(activity, label);
        chip.setCheckable(true);
        chip.setOnClickListener(v -> {
            sortType = value;
            loadPosts(true);
        });
        return chip;
    }

    private void loadHotKeywords() {
        MainActivity activity = (MainActivity) requireActivity();
        hotKeywordLayout.removeAllViews();
        activity.getRepository().getHotKeywords(6, new ApiCallback<>() {
            @Override
            public void onSuccess(List<HotKeywordModel> data) {
                for (HotKeywordModel item : data) {
                    Chip chip = KinUi.chip(activity, item.keyword);
                    chip.setOnClickListener(v -> {
                        keyword = item.keyword;
                        loadPosts(true);
                    });
                    hotKeywordLayout.addView(chip);
                }
            }

            @Override
            public void onError(ApiException exception) {
                Chip chip = KinUi.chip(activity, exception.isFeatureUnavailable() ? "热词待开放" : "热词加载失败");
                hotKeywordLayout.addView(chip);
            }
        });
    }

    private void showSearchDialog() {
        MainActivity activity = (MainActivity) requireActivity();
        LinearLayout root = KinUi.vertical(activity);

        TextInputLayout keywordLayout = KinUi.inputLayout(activity, "关键词", false);
        TextInputLayout mapLayout = KinUi.inputLayout(activity, "地图", false);
        TextInputLayout authorLayout = KinUi.inputLayout(activity, "作者", false);
        TextInputEditText keywordEdit = KinUi.edit(keywordLayout);
        TextInputEditText mapEdit = KinUi.edit(mapLayout);
        TextInputEditText authorEdit = KinUi.edit(authorLayout);
        keywordEdit.setText(keyword);
        mapEdit.setText(mapName);
        authorEdit.setText(author);
        CheckBox exactCheck = new CheckBox(activity);
        exactCheck.setText("\u7cbe\u786e\u641c\u7d22");
        exactCheck.setChecked(exactSearch);

        root.addView(keywordLayout);
        View historyView = buildSearchHistoryView(activity, keywordEdit);
        if (historyView != null) {
            root.addView(historyView);
        }
        root.addView(mapLayout);
        root.addView(authorLayout);
        root.addView(exactCheck);
        if (historyView != null) {
            KinUi.margins(historyView, activity, 0, 8, 0, 0);
        }
        KinUi.margins(mapLayout, activity, 0, 10, 0, 0);
        KinUi.margins(authorLayout, activity, 0, 10, 0, 0);
        KinUi.margins(exactCheck, activity, 0, 8, 0, 0);

        new AlertDialog.Builder(activity)
                .setTitle("搜索帖子")
                .setView(root)
                .setPositiveButton("搜索", (dialog, which) -> {
                    keyword = stringValue(keywordEdit);
                    mapName = stringValue(mapEdit);
                    author = stringValue(authorEdit);
                    exactSearch = exactCheck.isChecked();
                    rememberSearchTerm(TextUtils.isEmpty(keyword) ? mapName : keyword);
                    loadPosts(true);
                })
                .setNeutralButton("清空", (dialog, which) -> {
                    keyword = "";
                    mapName = "";
                    author = "";
                    exactSearch = false;
                    loadPosts(true);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private View buildSearchHistoryView(MainActivity activity, TextInputEditText target) {
        List<String> history = searchHistory(activity);
        if (history.isEmpty()) {
            return null;
        }
        HorizontalScrollView scrollView = new HorizontalScrollView(activity);
        scrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(row);
        for (String term : history) {
            Chip chip = KinUi.chip(activity, term);
            chip.setOnClickListener(v -> target.setText(term));
            row.addView(chip);
            KinUi.margins(chip, activity, 0, 0, 8, 0);
        }
        return scrollView;
    }

    private void rememberSearchTerm(String term) {
        if (TextUtils.isEmpty(term)) {
            return;
        }
        MainActivity activity = (MainActivity) requireActivity();
        List<String> history = searchHistory(activity);
        String normalized = term.trim();
        history.removeIf(item -> TextUtils.equals(item, normalized));
        history.add(0, normalized);
        while (history.size() > MAX_SEARCH_HISTORY) {
            history.remove(history.size() - 1);
        }
        activity.getSharedPreferences(SEARCH_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(SEARCH_HISTORY_KEY, TextUtils.join("\n", history))
                .apply();
    }

    private List<String> searchHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SEARCH_PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(SEARCH_HISTORY_KEY, "");
        List<String> history = new ArrayList<>();
        if (TextUtils.isEmpty(raw)) {
            return history;
        }
        for (String term : raw.split("\n")) {
            if (!TextUtils.isEmpty(term)) {
                history.add(term);
            }
        }
        return history;
    }

    private void loadPosts(boolean reset) {
        if (reset) {
            posts.clear();
            currentPage = 0;
            lastPage = false;
        }
        setLoading(true, "正在同步内容…");
        MainActivity activity = (MainActivity) requireActivity();
        ApiCallback<PageResult<ForumPostModel>> callback = new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<ForumPostModel> data) {
                posts.addAll(exactSearch ? exactFiltered(data.items) : data.items);
                currentPage = data.page + 1;
                lastPage = data.page + 1 >= data.totalPages;
                renderPosts(activity.getImageLoader());
                finishRefreshing();
                setLoading(false, data.items.isEmpty() && posts.isEmpty() ? "还没有可展示的帖子。" : "");
            }

            @Override
            public void onError(ApiException exception) {
                renderPosts(activity.getImageLoader());
                finishRefreshing();
                setLoading(false, "帖子加载失败：" + exception.getMessage());
            }
        };

        boolean hasSearch = !TextUtils.isEmpty(keyword) || !TextUtils.isEmpty(mapName) || !TextUtils.isEmpty(author);
        if (hasSearch) {
            activity.getRepository().searchPosts(keyword, currentType, mapName, author, currentPage, 10, sortType, "", "", callback);
        } else {
            activity.getRepository().getPosts(currentType, currentPage, 10, sortType, "", "", callback);
        }
    }

    private List<ForumPostModel> exactFiltered(List<ForumPostModel> source) {
        if (!exactSearch) {
            return source;
        }
        List<ForumPostModel> result = new ArrayList<>();
        for (ForumPostModel post : source) {
            if (matchesExact(post)) {
                result.add(post);
            }
        }
        return result;
    }

    private boolean matchesExact(ForumPostModel post) {
        return exactMatch(keyword,
                post.title,
                post.mapName,
                post.propName,
                post.toolType,
                translateToolType(post.toolType),
                post.throwMethod,
                post.propPosition,
                post.tacticName,
                post.tacticType,
                post.tacticDescription,
                post.content)
                && exactMatch(mapName, post.mapName)
                && exactMatch(author, post.createdByUsername);
    }

    private boolean exactMatch(String query, String... values) {
        if (TextUtils.isEmpty(query)) {
            return true;
        }
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (!TextUtils.isEmpty(value) && normalizedQuery.equals(value.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void renderPosts(RemoteImageLoader imageLoader) {
        listContainer.removeAllViews();
        MainActivity activity = (MainActivity) requireActivity();
        if (System.currentTimeMillis() >= 0) {
            renderFeedPosts(activity, imageLoader);
            loadMoreButton.setVisibility(lastPage ? View.GONE : View.VISIBLE);
            return;
        }
        for (ForumPostModel post : posts) {
            MaterialCardView card = KinUi.card(activity);
            LinearLayout body = KinUi.sectionContainer(activity, 16);

            body.addView(KinUi.text(activity, post.title, 18, true));
            TextView meta = KinUi.muted(activity,
                    post.createdByUsername + " · " + post.createdAt + " · " + translateType(post.postType),
                    12);
            KinUi.margins(meta, activity, 0, 8, 0, 0);
            body.addView(meta);

            TextView summary = KinUi.muted(activity, buildSummary(post), 14);
            KinUi.margins(summary, activity, 0, 10, 0, 0);
            body.addView(summary);

            if (!post.tags.isEmpty()) {
                TextView tags = KinUi.muted(activity, TextUtils.join("  ", post.tags), 12);
                KinUi.margins(tags, activity, 0, 10, 0, 0);
                body.addView(tags);
            }

            List<String> previewImages = previewImages(post);
            if (!previewImages.isEmpty()) {
                View images = buildImagePreviewRow(activity, previewImages, imageLoader);
                KinUi.margins(images, activity, 0, 12, 0, 0);
                body.addView(images);
            }

            MaterialButton detailButton = KinUi.filledButton(activity, "\u8be6\u60c5");
            detailButton.setOnClickListener(v -> activity.openPostDetail(post.id,
                    activity.getRepository().getSessionManager().getUser() != null
                            && TextUtils.equals(post.createdByUsername, activity.getRepository().getSessionManager().getUser().username)));
            MaterialButton likeButton = KinUi.outlinedButton(activity, "\u70b9\u8d5e " + post.likeCount);
            likeButton.setOnClickListener(v -> toggleLike(post, likeButton));
            MaterialButton favoriteButton = KinUi.outlinedButton(activity, "\u6536\u85cf");
            favoriteButton.setOnClickListener(v -> activity.getRepository().favoritePost(post.id, new ApiCallback<>() {
                @Override
                public void onSuccess(com.example.kin.model.FavoriteStatus data) {
                    favoriteButton.setText(data.favorited ? "\u5df2\u6536\u85cf" : "\u6536\u85cf");
                }

                @Override
                public void onError(ApiException exception) {
                    favoriteButton.setText("\u6536\u85cf\u5931\u8d25");
                }
            }));
            MaterialButton reportButton = KinUi.outlinedButton(activity, "\u4e3e\u62a5");
            reportButton.setOnClickListener(v -> showReportDialog(post.id));

            LinearLayout actions = KinUi.buttonGrid(activity, 2, detailButton, likeButton, favoriteButton, reportButton);
            KinUi.margins(actions, activity, 0, 14, 0, 0);
            body.addView(actions);
            card.addView(body);
            listContainer.addView(card);
        }
        loadMoreButton.setVisibility(lastPage ? View.GONE : View.VISIBLE);
    }

    private void renderFeedPosts(MainActivity activity, RemoteImageLoader imageLoader) {
        for (int i = 0; i < posts.size(); i++) {
            ForumPostModel post = posts.get(i);
            listContainer.addView(buildFeedItem(activity, imageLoader, post));
            if (i < posts.size() - 1) {
                listContainer.addView(buildFeedSeparator(activity));
            }
        }
    }

    private View buildFeedItem(MainActivity activity, RemoteImageLoader imageLoader, ForumPostModel post) {
        LinearLayout body = KinUi.vertical(activity);
        body.setClickable(true);
        body.setFocusable(true);
        body.setPadding(KinUi.dp(activity, 10), KinUi.dp(activity, 16), KinUi.dp(activity, 10), KinUi.dp(activity, 16));
        body.setOnClickListener(v -> openPostDetail(activity, post));

        body.addView(buildFeedAuthorLine(activity, post));

        TextView title = KinUi.text(activity, safeText(post.title, "\u672a\u547d\u540d\u5e16\u5b50"), 20, true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        KinUi.margins(title, activity, 0, 12, 0, 0);
        body.addView(title);

        TextView summary = KinUi.text(activity, buildFeedSummary(post), 16, false);
        summary.setMaxLines(3);
        summary.setEllipsize(TextUtils.TruncateAt.END);
        summary.setLineSpacing(KinUi.dp(activity, 3), 1f);
        KinUi.margins(summary, activity, 0, 8, 0, 0);
        body.addView(summary);

        List<String> previewImages = previewImages(post);
        if (!previewImages.isEmpty()) {
            View images = buildImagePreviewRow(activity, previewImages, imageLoader);
            KinUi.margins(images, activity, 0, 12, 0, 0);
            body.addView(images);
        }

        body.addView(buildPostFooter(activity, post));
        return body;
    }

    private View buildFeedAuthorLine(MainActivity activity, ForumPostModel post) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView avatar = KinUi.text(activity, avatarText(post.createdByUsername), 10, true);
        avatar.setGravity(android.view.Gravity.CENTER);
        avatar.setTextColor(activity.getColor(R.color.kin_text_inverse));
        GradientDrawable avatarBg = LevelVisuals.avatarBackground(post.authorLevel);
        avatar.setBackground(avatarBg);
        row.addView(avatar, new LinearLayout.LayoutParams(KinUi.dp(activity, 24), KinUi.dp(activity, 24)));

        TextView username = KinUi.muted(activity, safeText(post.createdByUsername, "\u7528\u6237"), 14);
        username.setMaxLines(1);
        username.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams usernameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        usernameParams.leftMargin = KinUi.dp(activity, 8);
        row.addView(username, usernameParams);

        TextView level = buildLevelBadge(activity, post.authorLevel);
        row.addView(level);
        return row;
    }

    private View buildFeedSeparator(MainActivity activity) {
        View separator = new View(activity);
        separator.setBackgroundColor(activity.getColor(KinUi.isNight(activity) ? R.color.kin_stroke_dark : R.color.kin_stroke));
        separator.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                KinUi.dp(activity, 1)
        ));
        return separator;
    }

    private void openPostDetail(MainActivity activity, ForumPostModel post) {
        activity.openPostDetail(post.id,
                activity.getRepository().getSessionManager().getUser() != null
                        && TextUtils.equals(post.createdByUsername, activity.getRepository().getSessionManager().getUser().username));
    }

    private View buildAuthorRow(MainActivity activity, ForumPostModel post) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView username = KinUi.muted(activity, safeText(post.createdByUsername, "\u7528\u6237"), 13);
        username.setMaxLines(1);
        username.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(username, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView level = buildLevelBadge(activity, post.authorLevel);
        row.addView(level);
        return row;
    }

    private TextView buildLevelBadge(MainActivity activity, int authorLevel) {
        TextView badge = KinUi.text(activity, "Lv." + LevelVisuals.normalize(authorLevel), 11, true);
        badge.setTextColor(activity.getColor(R.color.kin_text_inverse));
        GradientDrawable background = LevelVisuals.badgeBackground(activity, authorLevel);
        badge.setBackground(background);
        badge.setPadding(KinUi.dp(activity, 5), KinUi.dp(activity, 2), KinUi.dp(activity, 5), KinUi.dp(activity, 2));
        badge.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return badge;
    }

    private View buildPostFooter(MainActivity activity, ForumPostModel post) {
        LinearLayout footer = new LinearLayout(activity);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(android.view.Gravity.CENTER_VERTICAL);
        KinUi.margins(footer, activity, 0, 12, 0, 0);

        TextView category = buildCategoryBadge(activity, feedCategoryLabel(post));
        KinUi.margins(category, activity, 0, 0, 8, 0);
        footer.addView(category);

        TextView author = KinUi.muted(activity, safeText(post.createdByUsername, "\u7528\u6237"), 13);
        author.setMaxLines(1);
        author.setEllipsize(TextUtils.TruncateAt.END);
        footer.addView(author, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView stats = KinUi.muted(activity,
                "\u70b9\u8d5e " + post.likeCount + "    \u8bc4\u8bba " + post.commentCount,
                13);
        stats.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        footer.addView(stats);
        return footer;
    }

    private TextView buildCategoryBadge(MainActivity activity, String label) {
        TextView badge = KinUi.text(activity, label, 13, false);
        badge.setTextColor(activity.getColor(R.color.kin_text_muted));
        GradientDrawable background = new GradientDrawable();
        background.setColor(activity.getColor(R.color.kin_light_panel_alt));
        background.setCornerRadius(KinUi.dp(activity, 5));
        badge.setBackground(background);
        badge.setPadding(KinUi.dp(activity, 8), KinUi.dp(activity, 4), KinUi.dp(activity, 8), KinUi.dp(activity, 4));
        return badge;
    }

    private View buildImagePreviewRow(MainActivity activity, List<String> urls, RemoteImageLoader imageLoader) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int visibleCount = Math.min(urls.size(), 3);
        for (int i = 0; i < visibleCount; i++) {
            ImageView imageView = new ImageView(activity);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    KinUi.dp(activity, 118),
                    1f
            );
            if (i > 0) {
                params.leftMargin = KinUi.dp(activity, 8);
            }
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable background = new GradientDrawable();
            background.setColor(activity.getColor(R.color.kin_light_panel_alt));
            background.setCornerRadius(KinUi.dp(activity, 8));
            imageView.setBackground(background);
            imageView.setClipToOutline(true);
            imageLoader.load(imageView, urls.get(i));
            row.addView(imageView);
        }
        return row;
    }

    private void toggleLike(ForumPostModel post, MaterialButton likeButton) {
        MainActivity activity = (MainActivity) requireActivity();
        ApiCallback<LikeStatusModel> callback = new ApiCallback<>() {
            @Override
            public void onSuccess(LikeStatusModel data) {
                post.liked = data.liked;
                post.likeCount = data.likeCount;
                likeButton.setText((data.liked ? "已赞 " : "点赞 ") + data.likeCount);
            }

            @Override
            public void onError(ApiException exception) {
                likeButton.setText("点赞失败");
            }
        };
        if (post.liked) {
            activity.getRepository().unlikePost(post.id, callback);
        } else {
            activity.getRepository().likePost(post.id, callback);
        }
    }

    private void showReportDialog(long postId) {
        MainActivity activity = (MainActivity) requireActivity();
        LinearLayout root = KinUi.vertical(activity);
        TextInputLayout reasonLayout = KinUi.inputLayout(activity, "举报原因类型（如：违规）", false);
        TextInputLayout detailLayout = KinUi.inputLayout(activity, "补充说明", true);
        TextInputEditText reasonEdit = KinUi.edit(reasonLayout);
        TextInputEditText detailEdit = KinUi.edit(detailLayout);
        root.addView(reasonLayout);
        root.addView(detailLayout);
        KinUi.margins(detailLayout, activity, 0, 12, 0, 0);
        reasonEdit.setText("违规");
        new AlertDialog.Builder(activity)
                .setTitle("举报帖子")
                .setView(root)
                .setPositiveButton("提交", (dialog, which) -> activity.getRepository().createReport(
                        "POST", postId, normalizeReportReason(stringValue(reasonEdit)), stringValue(detailEdit), new ApiCallback<>() {
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

    private List<String> previewImages(ForumPostModel post) {
        List<String> items = new ArrayList<>();
        if (!post.imageUrls.isEmpty()) {
            for (String imageUrl : post.imageUrls) {
                if (!TextUtils.isEmpty(imageUrl) && items.size() < 3) {
                    items.add(imageUrl);
                }
            }
            return items;
        }
        if (!TextUtils.isEmpty(post.stanceImageUrl) && items.size() < 3) {
            items.add(post.stanceImageUrl);
        }
        if (!TextUtils.isEmpty(post.aimImageUrl) && items.size() < 3) {
            items.add(post.aimImageUrl);
        }
        if (!TextUtils.isEmpty(post.landingImageUrl) && items.size() < 3) {
            items.add(post.landingImageUrl);
        }
        return items;
    }

    private String buildSummary(ForumPostModel post) {
        if ("PROP_SHARE".equals(post.postType)) {
            return post.mapName + " · " + translateToolType(post.toolType) + " · " + post.throwMethod;
        }
        if ("TACTIC_SHARE".equals(post.postType)) {
            return post.mapName + " · " + post.tacticType + " · " + JsonUtils.shorten(post.tacticDescription);
        }
        return JsonUtils.shorten(post.content);
    }

    private String buildFeedMeta(ForumPostModel post) {
        String map = TextUtils.isEmpty(post.mapName) ? "" : " \u00b7 " + post.mapName;
        return safeText(post.createdAt, "") + map + " \u00b7 " + translateType(post.postType);
    }

    private String buildFeedSummary(ForumPostModel post) {
        if ("PROP_SHARE".equals(post.postType)) {
            return safeText(post.throwMethod, post.propPosition);
        }
        if ("TACTIC_SHARE".equals(post.postType)) {
            return safeText(post.tacticDescription, post.tacticType);
        }
        return safeText(post.content, "\u6682\u65e0\u6b63\u6587");
    }

    private String categoryText(ForumPostModel post) {
        if (!TextUtils.isEmpty(post.mapName)) {
            return post.mapName;
        }
        return translateType(post.postType);
    }

    private String feedCategoryLabel(ForumPostModel post) {
        if ("PROP_SHARE".equals(post.postType)) {
            return "CS2道具分享";
        }
        if ("TACTIC_SHARE".equals(post.postType)) {
            return "CS2战术分享";
        }
        if (!TextUtils.isEmpty(post.mapName)) {
            return post.mapName;
        }
        return translateType(post.postType);
    }

    private String safeText(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private String avatarText(String value) {
        if (TextUtils.isEmpty(value)) {
            return "U";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String translateType(String postType) {
        switch (postType) {
            case "PROP_SHARE":
                return "道具";
            case "TACTIC_SHARE":
                return "战术";
            case "DAILY_CHAT":
                return "日常";
            default:
                return "其他";
        }
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

    private String stringValue(TextInputEditText editText) {
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

    private void applyPostActionButtonStyle(MainActivity activity, MaterialButton button, int leftMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        params.leftMargin = KinUi.dp(activity, leftMarginDp);
        button.setLayoutParams(params);
        button.setMaxLines(1);
        button.setInsetTop(KinUi.dp(activity, 6));
        button.setInsetBottom(KinUi.dp(activity, 6));
    }
}
