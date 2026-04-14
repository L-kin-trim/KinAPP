package com.example.kin.ui;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.kin.MainActivity;
import com.example.kin.model.ForumPostModel;
import com.example.kin.model.HotKeywordModel;
import com.example.kin.model.LikeStatusModel;
import com.example.kin.model.PageResult;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.BasePageFragment;
import com.example.kin.ui.common.KinUi;
import com.example.kin.ui.common.RemoteImageLoader;
import com.example.kin.util.JsonUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends BasePageFragment {
    private final List<ForumPostModel> posts = new ArrayList<>();
    private LinearLayout listContainer;
    private LinearLayout hotKeywordLayout;
    private MaterialButton loadMoreButton;
    private String currentType = "";
    private String keyword = "";
    private String mapName = "";
    private String author = "";
    private String sortType = "LATEST";
    private int currentPage = 0;
    private boolean lastPage = false;

    @Override
    protected void onPageReady() {
        MainActivity activity = (MainActivity) requireActivity();
        activity.setTopBar("首页", "");

        contentLayout.addView(buildHeroCard(activity));
        contentLayout.addView(buildFilterRow(activity));
        contentLayout.addView(buildHotKeywordsCard(activity));

        listContainer = KinUi.vertical(activity);
        contentLayout.addView(listContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        loadMoreButton = KinUi.outlinedButton(activity, "加载更多");
        loadMoreButton.setOnClickListener(v -> loadPosts(false));
        contentLayout.addView(loadMoreButton);
        loadHotKeywords();
        loadPosts(true);
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

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(searchButton);
        actions.addView(publishButton);
        KinUi.margins(publishButton, activity, 10, 0, 0, 0);

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

        root.addView(keywordLayout);
        root.addView(mapLayout);
        root.addView(authorLayout);
        KinUi.margins(mapLayout, activity, 0, 10, 0, 0);
        KinUi.margins(authorLayout, activity, 0, 10, 0, 0);

        new AlertDialog.Builder(activity)
                .setTitle("搜索帖子")
                .setView(root)
                .setPositiveButton("搜索", (dialog, which) -> {
                    keyword = stringValue(keywordEdit);
                    mapName = stringValue(mapEdit);
                    author = stringValue(authorEdit);
                    loadPosts(true);
                })
                .setNeutralButton("清空", (dialog, which) -> {
                    keyword = "";
                    mapName = "";
                    author = "";
                    loadPosts(true);
                })
                .setNegativeButton("取消", null)
                .show();
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
                posts.addAll(data.items);
                currentPage = data.page + 1;
                lastPage = data.page + 1 >= data.totalPages;
                renderPosts(activity.getImageLoader());
                setLoading(false, data.items.isEmpty() && posts.isEmpty() ? "还没有可展示的帖子。" : "");
            }

            @Override
            public void onError(ApiException exception) {
                renderPosts(activity.getImageLoader());
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

    private void renderPosts(RemoteImageLoader imageLoader) {
        listContainer.removeAllViews();
        MainActivity activity = (MainActivity) requireActivity();
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
                View images = KinUi.imageStrip(activity, previewImages, imageLoader);
                KinUi.margins(images, activity, 0, 12, 0, 0);
                body.addView(images);
            }

            LinearLayout actions = new LinearLayout(activity);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            MaterialButton detailButton = KinUi.filledButton(activity, "详情");
            detailButton.setOnClickListener(v -> activity.openPostDetail(post.id,
                    TextUtils.equals(post.createdByUsername, activity.getRepository().getSessionManager().getUser().username)));
            MaterialButton likeButton = KinUi.outlinedButton(activity, "点赞 " + post.likeCount);
            likeButton.setOnClickListener(v -> toggleLike(post, likeButton));
            MaterialButton favoriteButton = KinUi.outlinedButton(activity, "收藏");
            favoriteButton.setOnClickListener(v -> activity.getRepository().favoritePost(post.id, new ApiCallback<>() {
                @Override
                public void onSuccess(com.example.kin.model.FavoriteStatus data) {
                    favoriteButton.setText(data.favorited ? "已收藏" : "收藏");
                }

                @Override
                public void onError(ApiException exception) {
                    favoriteButton.setText("收藏失败");
                }
            }));
            MaterialButton reportButton = KinUi.outlinedButton(activity, "举报");
            reportButton.setOnClickListener(v -> showReportDialog(post.id));
            actions.addView(detailButton);
            actions.addView(likeButton);
            actions.addView(favoriteButton);
            actions.addView(reportButton);
            KinUi.margins(likeButton, activity, 10, 0, 0, 0);
            KinUi.margins(favoriteButton, activity, 10, 0, 0, 0);
            KinUi.margins(reportButton, activity, 10, 0, 0, 0);
            KinUi.margins(actions, activity, 0, 14, 0, 0);
            body.addView(actions);

            card.addView(body);
            listContainer.addView(card);
        }
        loadMoreButton.setVisibility(lastPage ? View.GONE : View.VISIBLE);
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
        TextInputLayout reasonLayout = KinUi.inputLayout(activity, "举报原因类型（如 VIOLATION）", false);
        TextInputLayout detailLayout = KinUi.inputLayout(activity, "补充说明", true);
        TextInputEditText reasonEdit = KinUi.edit(reasonLayout);
        TextInputEditText detailEdit = KinUi.edit(detailLayout);
        root.addView(reasonLayout);
        root.addView(detailLayout);
        KinUi.margins(detailLayout, activity, 0, 12, 0, 0);
        reasonEdit.setText("VIOLATION");
        new AlertDialog.Builder(activity)
                .setTitle("举报帖子")
                .setView(root)
                .setPositiveButton("提交", (dialog, which) -> activity.getRepository().createReport(
                        "POST", postId, stringValue(reasonEdit), stringValue(detailEdit), new ApiCallback<>() {
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
            items.addAll(post.imageUrls);
            return items;
        }
        if (!TextUtils.isEmpty(post.stanceImageUrl)) {
            items.add(post.stanceImageUrl);
        }
        if (!TextUtils.isEmpty(post.aimImageUrl)) {
            items.add(post.aimImageUrl);
        }
        if (!TextUtils.isEmpty(post.landingImageUrl)) {
            items.add(post.landingImageUrl);
        }
        return items;
    }

    private String buildSummary(ForumPostModel post) {
        if ("PROP_SHARE".equals(post.postType)) {
            return post.mapName + " · " + post.toolType + " · " + post.throwMethod;
        }
        if ("TACTIC_SHARE".equals(post.postType)) {
            return post.mapName + " · " + post.tacticType + " · " + JsonUtils.shorten(post.tacticDescription);
        }
        return JsonUtils.shorten(post.content);
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

    private String stringValue(TextInputEditText editText) {
        return String.valueOf(editText.getText()).trim();
    }
}
