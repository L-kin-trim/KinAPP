package com.example.kin.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.R;
import com.example.kin.data.KinRepository;
import com.example.kin.model.ForumPostModel;
import com.example.kin.model.LibraryItem;
import com.example.kin.model.PageResult;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.KinUi;
import com.example.kin.ui.common.RemoteImageLoader;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {
    public static final String EXTRA_MODE = "extra_mode";
    public static final String MODE_HOME = "home";
    public static final String MODE_LIBRARY = "library";

    private static final int MAX_SEARCH_HISTORY = 8;

    private KinRepository repository;
    private RemoteImageLoader imageLoader;
    private String mode;
    private LinearLayout resultLayout;
    private ProgressBar progressBar;
    private TextView statusView;
    private TextInputEditText keywordEdit;
    private MaterialButton loadMoreButton;
    private final List<ForumPostModel> postResults = new ArrayList<>();
    private final List<LibraryItem> libraryResults = new ArrayList<>();
    private int page;
    private boolean lastPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KinRepository(this);
        imageLoader = new RemoteImageLoader();
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (TextUtils.isEmpty(mode)) {
            mode = MODE_HOME;
        }

        LinearLayout root = KinUi.vertical(this);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));
        root.addView(buildTopSearchBar());

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = KinUi.vertical(this);
        content.setPadding(KinUi.dp(this, 16), KinUi.dp(this, 18), KinUi.dp(this, 16), KinUi.dp(this, 24));
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        content.addView(buildHistorySection());
        progressBar = new ProgressBar(this);
        statusView = KinUi.muted(this, "", 13);
        statusView.setVisibility(View.GONE);
        content.addView(progressBar);
        content.addView(statusView);
        resultLayout = KinUi.vertical(this);
        content.addView(resultLayout);

        loadMoreButton = KinUi.outlinedButton(this, "加载更多");
        loadMoreButton.setOnClickListener(v -> runSearch(false));
        content.addView(loadMoreButton);
        loadMoreButton.setVisibility(View.GONE);

        setContentView(root);
        setLoading(false, "");
        keywordEdit.requestFocus();
        keywordEdit.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(keywordEdit, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200L);
    }

    private View buildTopSearchBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(KinUi.dp(this, 8), KinUi.dp(this, 10), KinUi.dp(this, 12), KinUi.dp(this, 8));

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_nav_back);
        back.setPadding(KinUi.dp(this, 8), KinUi.dp(this, 8), KinUi.dp(this, 8), KinUi.dp(this, 8));
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(KinUi.dp(this, 48), KinUi.dp(this, 48)));

        TextInputLayout keywordLayout = new TextInputLayout(this);
        keywordLayout.setStartIconDrawable(android.R.drawable.ic_menu_search);
        keywordLayout.setHint(MODE_LIBRARY.equals(mode) ? "搜索收藏库" : "搜索帖子");
        GradientDrawable background = new GradientDrawable();
        background.setColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_panel_alt : R.color.kin_light_panel_alt));
        background.setCornerRadius(KinUi.dp(this, 5));
        keywordLayout.setBackground(background);

        keywordEdit = new TextInputEditText(this);
        keywordEdit.setSingleLine(true);
        keywordEdit.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        keywordEdit.setTextSize(16);
        keywordEdit.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch(true);
                return true;
            }
            return false;
        });
        keywordLayout.addView(keywordEdit, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        bar.addView(keywordLayout, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return bar;
    }

    private View buildHistorySection() {
        LinearLayout body = KinUi.vertical(this);
        TextView title = KinUi.text(this, "搜索历史", 20, true);
        body.addView(title);
        View history = buildSearchHistoryView();
        if (history != null) {
            KinUi.margins(history, this, 0, 14, 0, 0);
            body.addView(history);
        } else {
            TextView empty = KinUi.muted(this, "暂无搜索历史", 15);
            KinUi.margins(empty, this, 0, 14, 0, 0);
            body.addView(empty);
        }
        return body;
    }

    private View buildSearchHistoryView() {
        List<String> history = searchHistory();
        if (history.isEmpty()) {
            return null;
        }
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(row);
        for (String term : history) {
            Chip chip = KinUi.chip(this, term);
            chip.setTextSize(16);
            chip.setOnClickListener(v -> {
                keywordEdit.setText(term);
                keywordEdit.setSelection(keywordEdit.length());
                runSearch(true);
            });
            row.addView(chip);
            KinUi.margins(chip, this, 0, 0, 8, 0);
        }
        return scrollView;
    }

    private void runSearch(boolean reset) {
        String keyword = text(keywordEdit);
        if (reset) {
            page = 0;
            lastPage = false;
            postResults.clear();
            libraryResults.clear();
        }
        rememberSearchTerm(keyword);
        if (MODE_LIBRARY.equals(mode)) {
            searchLibrary(keyword);
        } else {
            searchPosts(keyword);
        }
    }

    private void searchPosts(String keyword) {
        setLoading(true, "正在搜索帖子...");
        repository.searchPosts(keyword, "", "", "", page, 10, "LATEST", "", "", new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<ForumPostModel> data) {
                postResults.addAll(data.items);
                page = data.page + 1;
                lastPage = data.page + 1 >= data.totalPages;
                renderPostResults();
                setLoading(false, postResults.isEmpty() ? "没有搜索到帖子。" : "");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "搜索失败：" + exception.getMessage());
            }
        });
    }

    private void searchLibrary(String keyword) {
        setLoading(true, "正在搜索收藏库...");
        repository.getFavorites("", page, 30, new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<LibraryItem> data) {
                for (LibraryItem item : data.items) {
                    if (matchesLibrary(item, keyword)) {
                        libraryResults.add(item);
                    }
                }
                page = data.page + 1;
                lastPage = data.page + 1 >= data.totalPages;
                renderLibraryResults();
                setLoading(false, libraryResults.isEmpty() ? "没有搜索到收藏内容。" : "");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "搜索失败：" + exception.getMessage());
            }
        });
    }

    private void renderPostResults() {
        resultLayout.removeAllViews();
        for (ForumPostModel post : postResults) {
            MaterialCardView card = resultCard();
            card.setOnClickListener(v -> openPost(post.id, post.createdByUsername));
            LinearLayout body = KinUi.sectionContainer(this, 16);
            addPostText(body, post.title, buildPostMeta(post), buildPostSummary(post));
            card.addView(body);
            resultLayout.addView(card);
        }
        loadMoreButton.setVisibility(lastPage ? View.GONE : View.VISIBLE);
    }

    private void renderLibraryResults() {
        resultLayout.removeAllViews();
        for (LibraryItem item : libraryResults) {
            MaterialCardView card = resultCard();
            if (item.forumPostId > 0) {
                card.setOnClickListener(v -> openPost(item.forumPostId, item.createdByUsername));
            }
            LinearLayout body = KinUi.sectionContainer(this, 16);
            addPostText(body, safeText(item.title, "未命名收藏"), buildPostMeta(item), buildPostSummary(item));
            card.addView(body);
            resultLayout.addView(card);
        }
        loadMoreButton.setVisibility(lastPage ? View.GONE : View.VISIBLE);
    }

    private MaterialCardView resultCard() {
        MaterialCardView card = KinUi.card(this);
        card.setClickable(true);
        card.setFocusable(true);
        return card;
    }

    private void addPostText(LinearLayout body, String titleValue, String metaValue, String summaryValue) {
        TextView title = KinUi.text(this, safeText(titleValue, "未命名帖子"), 18, true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        body.addView(title);
        TextView meta = KinUi.muted(this, metaValue, 13);
        KinUi.margins(meta, this, 0, 8, 0, 0);
        body.addView(meta);
        TextView summary = KinUi.muted(this, summaryValue, 14);
        summary.setMaxLines(3);
        summary.setEllipsize(TextUtils.TruncateAt.END);
        KinUi.margins(summary, this, 0, 10, 0, 0);
        body.addView(summary);
    }

    private void openPost(long postId, String author) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, postId);
        intent.putExtra(PostDetailActivity.EXTRA_MINE,
                repository.getSessionManager().getUser() != null
                        && TextUtils.equals(author, repository.getSessionManager().getUser().username));
        startActivity(intent);
    }

    private boolean matchesLibrary(LibraryItem item, String query) {
        if (TextUtils.isEmpty(query)) {
            return true;
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        return contains(item.title, normalized)
                || contains(item.mapName, normalized)
                || contains(item.propName, normalized)
                || contains(item.toolType, normalized)
                || contains(item.throwMethod, normalized)
                || contains(item.propPosition, normalized)
                || contains(item.tacticName, normalized)
                || contains(item.tacticType, normalized)
                || contains(item.tacticDescription, normalized)
                || contains(item.content, normalized);
    }

    private boolean contains(String value, String query) {
        return !TextUtils.isEmpty(value) && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String buildPostMeta(ForumPostModel post) {
        List<String> parts = new ArrayList<>();
        if (!TextUtils.isEmpty(post.createdAt)) {
            parts.add(post.createdAt);
        }
        if (!TextUtils.isEmpty(post.mapName)) {
            parts.add(post.mapName);
        }
        parts.add(translateType(post.postType));
        return TextUtils.join(" · ", parts);
    }

    private String buildPostSummary(ForumPostModel post) {
        if ("PROP_SHARE".equals(post.postType)) {
            return safeText(post.throwMethod, post.propPosition);
        }
        if ("TACTIC_SHARE".equals(post.postType)) {
            return safeText(post.tacticDescription, post.tacticType);
        }
        return safeText(post.content, "暂无正文");
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

    private void rememberSearchTerm(String term) {
        if (TextUtils.isEmpty(term)) {
            return;
        }
        List<String> history = searchHistory();
        String normalized = term.trim();
        history.removeIf(item -> TextUtils.equals(item, normalized));
        history.add(0, normalized);
        while (history.size() > MAX_SEARCH_HISTORY) {
            history.remove(history.size() - 1);
        }
        prefs().edit().putString("terms", TextUtils.join("\n", history)).apply();
    }

    private List<String> searchHistory() {
        String raw = prefs().getString("terms", "");
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

    private SharedPreferences prefs() {
        return getSharedPreferences(MODE_LIBRARY.equals(mode) ? "library_search_history" : "home_search_history", Context.MODE_PRIVATE);
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusView.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        statusView.setText(message);
    }

    private String text(TextInputEditText editText) {
        return editText == null ? "" : String.valueOf(editText.getText()).trim();
    }

    private String safeText(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}
