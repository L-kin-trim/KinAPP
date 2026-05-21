package com.example.kin.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.kin.MainActivity;
import com.example.kin.model.LibraryItem;
import com.example.kin.model.PageResult;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.BasePageFragment;
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
import java.util.Set;
import java.util.TreeSet;

public class LibraryFragment extends BasePageFragment {
    private static final String SEARCH_PREFS = "library_search_history";
    private static final String SEARCH_HISTORY_KEY = "terms";
    private static final int MAX_SEARCH_HISTORY = 8;

    private final List<LibraryItem> allItems = new ArrayList<>();
    private LinearLayout mapFilterLayout;
    private LinearLayout listContainer;
    private MaterialButton loadMoreButton;
    private String selectedMap = "";
    private String searchQuery = "";
    private int page = 0;
    private boolean lastPage = false;

    @Override
    protected void onPageReady() {
        MainActivity activity = (MainActivity) requireActivity();
        activity.setTopBar("\u6536\u85cf\u5e93", "");

        TextView archiveTitle = KinUi.muted(activity, "\u6309\u5730\u56fe\u5f52\u6863", 13);
        contentLayout.addView(archiveTitle);

        HorizontalScrollView scrollView = new HorizontalScrollView(activity);
        scrollView.setHorizontalScrollBarEnabled(false);
        mapFilterLayout = new LinearLayout(activity);
        mapFilterLayout.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(mapFilterLayout);
        KinUi.margins(scrollView, activity, 0, 8, 0, 12);
        contentLayout.addView(scrollView);

        listContainer = KinUi.vertical(activity);
        contentLayout.addView(listContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        loadMoreButton = KinUi.outlinedButton(activity, "\u52a0\u8f7d\u66f4\u591a");
        loadMoreButton.setOnClickListener(v -> loadData(false));
        contentLayout.addView(loadMoreButton);

        rebuildMapFilters(activity);
        loadData(true);
    }

    @Override
    protected void onRefreshRequested() {
        loadData(true);
    }

    public void openSearch() {
        android.content.Intent intent = new android.content.Intent(requireContext(), SearchActivity.class);
        intent.putExtra(SearchActivity.EXTRA_MODE, SearchActivity.MODE_LIBRARY);
        startActivity(intent);
    }

    private void loadData(boolean reset) {
        if (reset) {
            allItems.clear();
            page = 0;
            lastPage = false;
        }
        setLoading(true, "\u6b63\u5728\u540c\u6b65\u6536\u85cf\u5e93...");
        MainActivity activity = (MainActivity) requireActivity();
        activity.getRepository().getFavorites("", page, 10, new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<LibraryItem> data) {
                allItems.addAll(data.items);
                page = data.page + 1;
                lastPage = data.page + 1 >= data.totalPages;
                rebuildMapFilters(activity);
                renderItems(activity.getImageLoader());
                finishRefreshing();
                setLoading(false, allItems.isEmpty() ? "\u5f53\u524d\u8fd8\u6ca1\u6709\u6536\u85cf\u5e16\u5b50" : "");
            }

            @Override
            public void onError(ApiException exception) {
                renderItems(activity.getImageLoader());
                finishRefreshing();
                setLoading(false, "\u52a0\u8f7d\u5931\u8d25\uff1a" + exception.getMessage());
            }
        });
    }

    private void rebuildMapFilters(MainActivity activity) {
        if (mapFilterLayout == null) {
            return;
        }
        mapFilterLayout.removeAllViews();
        mapFilterLayout.addView(mapChip(activity, "\u5168\u90e8", ""));

        Set<String> maps = new TreeSet<>();
        for (LibraryItem item : allItems) {
            if (!TextUtils.isEmpty(item.mapName)) {
                maps.add(item.mapName.trim());
            }
        }
        if (!TextUtils.isEmpty(selectedMap) && !maps.contains(selectedMap)) {
            selectedMap = "";
        }
        for (String map : maps) {
            Chip chip = mapChip(activity, map, map);
            KinUi.margins(chip, activity, 8, 0, 0, 0);
            mapFilterLayout.addView(chip);
        }
    }

    private Chip mapChip(MainActivity activity, String label, String value) {
        Chip chip = KinUi.chip(activity, label);
        chip.setCheckable(true);
        chip.setChecked(TextUtils.equals(selectedMap, value));
        chip.setOnClickListener(v -> {
            selectedMap = value;
            rebuildMapFilters(activity);
            renderItems(activity.getImageLoader());
            setLoading(false, filteredItems().isEmpty() ? "\u6ca1\u6709\u5339\u914d\u7684\u6536\u85cf\u5e16\u5b50" : "");
        });
        return chip;
    }

    private void showSearchDialog() {
        MainActivity activity = (MainActivity) requireActivity();
        LinearLayout root = KinUi.vertical(activity);
        TextInputLayout keywordLayout = KinUi.inputLayout(activity, "\u641c\u7d22\u6807\u9898 / \u5730\u56fe / \u70b9\u4f4d / \u63cf\u8ff0", false);
        TextInputEditText keywordEdit = KinUi.edit(keywordLayout);
        keywordEdit.setText(searchQuery);
        root.addView(keywordLayout);
        View historyView = buildSearchHistoryView(activity, keywordEdit);
        if (historyView != null) {
            root.addView(historyView);
            KinUi.margins(historyView, activity, 0, 8, 0, 0);
        }

        new AlertDialog.Builder(activity)
                .setTitle("\u641c\u7d22\u6536\u85cf\u5e93")
                .setView(root)
                .setPositiveButton("\u641c\u7d22", (dialog, which) -> {
                    searchQuery = stringValue(keywordEdit);
                    rememberSearchTerm(searchQuery);
                    renderItems(activity.getImageLoader());
                    setLoading(false, filteredItems().isEmpty() ? "\u6ca1\u6709\u5339\u914d\u7684\u6536\u85cf\u5e16\u5b50" : "");
                })
                .setNeutralButton("\u6e05\u7a7a", (dialog, which) -> {
                    searchQuery = "";
                    renderItems(activity.getImageLoader());
                    setLoading(false, allItems.isEmpty() ? "\u5f53\u524d\u8fd8\u6ca1\u6709\u6536\u85cf\u5e16\u5b50" : "");
                })
                .setNegativeButton("\u53d6\u6d88", null)
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

    private void renderItems(RemoteImageLoader imageLoader) {
        listContainer.removeAllViews();
        MainActivity activity = (MainActivity) requireActivity();
        List<LibraryItem> displayItems = filteredItems();
        if (displayItems.isEmpty() && !allItems.isEmpty()) {
            TextView empty = KinUi.muted(activity, "\u6ca1\u6709\u5339\u914d\u7684\u6536\u85cf\u5e16\u5b50", 14);
            listContainer.addView(empty);
        }
        for (LibraryItem item : displayItems) {
            MaterialCardView card = KinUi.card(activity);
            if (item.forumPostId > 0) {
                card.setClickable(true);
                card.setFocusable(true);
                card.setOnClickListener(v -> activity.openPostDetail(item.forumPostId, false));
            }
            LinearLayout body = KinUi.sectionContainer(activity, 16);
            TextView title = KinUi.text(activity, safeText(item.title, "\u672a\u547d\u540d\u6536\u85cf"), 18, true);
            TextView subtitle = KinUi.muted(activity, buildSubtitle(item), 13);
            body.addView(title);
            KinUi.margins(subtitle, activity, 0, 8, 0, 0);
            body.addView(subtitle);

            String descText = description(item);
            if (!TextUtils.isEmpty(descText)) {
                TextView desc = KinUi.muted(activity, descText, 14);
                desc.setMaxLines(3);
                desc.setEllipsize(TextUtils.TruncateAt.END);
                KinUi.margins(desc, activity, 0, 10, 0, 0);
                body.addView(desc);
            }

            List<String> images = preview(item);
            if (!images.isEmpty()) {
                View strip = KinUi.imageStrip(activity, images, imageLoader);
                KinUi.margins(strip, activity, 0, 12, 0, 0);
                body.addView(strip);
            }
            card.addView(body);
            listContainer.addView(card);
        }
        loadMoreButton.setVisibility(lastPage ? View.GONE : View.VISIBLE);
    }

    private List<LibraryItem> filteredItems() {
        List<LibraryItem> result = new ArrayList<>();
        for (LibraryItem item : allItems) {
            if (!TextUtils.isEmpty(selectedMap) && !TextUtils.equals(selectedMap, item.mapName)) {
                continue;
            }
            if (!matchesSearch(item)) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    private boolean matchesSearch(LibraryItem item) {
        if (TextUtils.isEmpty(searchQuery)) {
            return true;
        }
        String query = searchQuery.toLowerCase(Locale.ROOT);
        return contains(item.title, query)
                || contains(item.mapName, query)
                || contains(item.propName, query)
                || contains(item.toolType, query)
                || contains(item.throwMethod, query)
                || contains(item.propPosition, query)
                || contains(item.tacticName, query)
                || contains(item.tacticType, query)
                || contains(item.tacticDescription, query)
                || contains(item.content, query)
                || contains(item.createdByUsername, query);
    }

    private boolean contains(String value, String query) {
        return !TextUtils.isEmpty(value) && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String buildSubtitle(LibraryItem item) {
        List<String> parts = new ArrayList<>();
        if (!TextUtils.isEmpty(item.mapName)) {
            parts.add(item.mapName);
        }
        parts.add(translate(item.postType));
        if (!TextUtils.isEmpty(item.propPosition)) {
            parts.add(item.propPosition);
        }
        return TextUtils.join(" \u00b7 ", parts);
    }

    private String description(LibraryItem item) {
        if (!TextUtils.isEmpty(item.throwMethod)) {
            return item.throwMethod;
        }
        if (!TextUtils.isEmpty(item.tacticDescription)) {
            return item.tacticDescription;
        }
        return item.content;
    }

    private List<String> preview(LibraryItem item) {
        List<String> result = new ArrayList<>();
        if (!TextUtils.isEmpty(item.stanceImageUrl)) {
            result.add(item.stanceImageUrl);
        }
        if (!TextUtils.isEmpty(item.aimImageUrl)) {
            result.add(item.aimImageUrl);
        }
        if (!TextUtils.isEmpty(item.landingImageUrl)) {
            result.add(item.landingImageUrl);
        }
        if (!item.imageUrls.isEmpty()) {
            result.addAll(item.imageUrls);
        }
        return result;
    }

    private String translate(String postType) {
        if ("TACTIC_SHARE".equals(postType)) {
            return "\u6218\u672f";
        }
        if ("DAILY_CHAT".equals(postType)) {
            return "\u65e5\u5e38";
        }
        return "\u9053\u5177";
    }

    private String stringValue(TextInputEditText editText) {
        return String.valueOf(editText.getText()).trim();
    }

    private String safeText(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}
