package com.example.kin.ui;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends BasePageFragment {
    private boolean favoriteMode = false;
    private int page = 0;
    private boolean lastPage = false;
    private final List<LibraryItem> items = new ArrayList<>();
    private LinearLayout listContainer;
    private MaterialButton loadMoreButton;
    private MaterialButton createButton;

    @Override
    protected void onPageReady() {
        MainActivity activity = (MainActivity) requireActivity();
        activity.setTopBar("收藏库", "");

        contentLayout.addView(headerCard(activity));
        listContainer = KinUi.vertical(activity);
        contentLayout.addView(listContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        loadMoreButton = KinUi.outlinedButton(activity, "加载更多");
        loadMoreButton.setOnClickListener(v -> loadData(false));
        contentLayout.addView(loadMoreButton);
        loadData(true);
    }

    private View headerCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 18);
        TextView title = KinUi.text(activity, "个人库 + 收藏夹", 22, true);
        TextView subtitle = KinUi.muted(activity, "既能保存自己的投掷/战术记录，也能把论坛里实用内容直接收进库里。", 14);
        LinearLayout chips = new LinearLayout(activity);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        Chip selfChip = KinUi.chip(activity, "我的条目");
        Chip favoriteChip = KinUi.chip(activity, "我的收藏");
        selfChip.setCheckable(true);
        favoriteChip.setCheckable(true);
        selfChip.setChecked(!favoriteMode);
        favoriteChip.setChecked(favoriteMode);
        selfChip.setOnClickListener(v -> {
            favoriteMode = false;
            loadData(true);
        });
        favoriteChip.setOnClickListener(v -> {
            favoriteMode = true;
            loadData(true);
        });
        chips.addView(selfChip);
        chips.addView(favoriteChip);
        KinUi.margins(favoriteChip, activity, 10, 0, 0, 0);

        createButton = KinUi.filledButton(activity, "新建条目");
        createButton.setOnClickListener(v -> showCreateDialog());

        body.addView(title);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);
        KinUi.margins(chips, activity, 0, 14, 0, 0);
        body.addView(chips);
        KinUi.margins(createButton, activity, 0, 14, 0, 0);
        createButton.setVisibility(favoriteMode ? View.GONE : View.VISIBLE);
        body.addView(createButton);
        card.addView(body);
        return card;
    }

    private void loadData(boolean reset) {
        if (createButton != null) {
            createButton.setVisibility(favoriteMode ? View.GONE : View.VISIBLE);
        }
        if (reset) {
            items.clear();
            page = 0;
            lastPage = false;
        }
        setLoading(true, "正在同步收藏库...");
        MainActivity activity = (MainActivity) requireActivity();
        ApiCallback<PageResult<LibraryItem>> callback = new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<LibraryItem> data) {
                items.addAll(data.items);
                page = data.page + 1;
                lastPage = data.page + 1 >= data.totalPages;
                renderItems(activity.getImageLoader());
                setLoading(false, items.isEmpty() ? "当前还没有内容" : "");
            }

            @Override
            public void onError(ApiException exception) {
                renderItems(activity.getImageLoader());
                setLoading(false, "加载失败：" + exception.getMessage());
            }
        };
        if (favoriteMode) {
            activity.getRepository().getFavorites("", page, 10, callback);
        } else {
            activity.getRepository().getLibraryItems("", page, 10, callback);
        }
    }

    private void renderItems(RemoteImageLoader imageLoader) {
        listContainer.removeAllViews();
        MainActivity activity = (MainActivity) requireActivity();
        for (LibraryItem item : items) {
            MaterialCardView card = KinUi.card(activity);
            LinearLayout body = KinUi.sectionContainer(activity, 16);
            TextView title = KinUi.text(activity, item.title, 18, true);
            TextView subtitle = KinUi.muted(activity, item.mapName + " · " + translate(item.postType), 13);
            body.addView(title);
            KinUi.margins(subtitle, activity, 0, 8, 0, 0);
            body.addView(subtitle);
            if (!TextUtils.isEmpty(item.throwMethod)) {
                TextView desc = KinUi.muted(activity, item.throwMethod, 14);
                KinUi.margins(desc, activity, 0, 10, 0, 0);
                body.addView(desc);
            } else if (!TextUtils.isEmpty(item.tacticDescription)) {
                TextView desc = KinUi.muted(activity, item.tacticDescription, 14);
                KinUi.margins(desc, activity, 0, 10, 0, 0);
                body.addView(desc);
            }
            List<String> images = preview(item);
            if (!images.isEmpty()) {
                View strip = KinUi.imageStrip(activity, images, imageLoader);
                KinUi.margins(strip, activity, 0, 12, 0, 0);
                body.addView(strip);
            }
            if (item.forumPostId > 0) {
                LinearLayout actions = new LinearLayout(activity);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                MaterialButton openButton = KinUi.outlinedButton(activity, "打开原帖");
                openButton.setOnClickListener(v -> activity.openPostDetail(item.forumPostId, false));
                actions.addView(openButton);
                if (favoriteMode) {
                    MaterialButton unfavoriteButton = KinUi.outlinedButton(activity, "取消收藏");
                    unfavoriteButton.setOnClickListener(v -> activity.getRepository().unfavoritePost(item.forumPostId, new ApiCallback<>() {
                        @Override
                        public void onSuccess(com.example.kin.model.FavoriteStatus data) {
                            loadData(true);
                        }

                        @Override
                        public void onError(ApiException exception) {
                            setLoading(false, "取消收藏失败：" + exception.getMessage());
                        }
                    }));
                    actions.addView(unfavoriteButton);
                    KinUi.margins(unfavoriteButton, activity, 10, 0, 0, 0);
                }
                KinUi.margins(actions, activity, 0, 12, 0, 0);
                body.addView(actions);
            }
            card.addView(body);
            listContainer.addView(card);
        }
        loadMoreButton.setVisibility(lastPage ? View.GONE : View.VISIBLE);
    }

    private void showCreateDialog() {
        MainActivity activity = (MainActivity) requireActivity();
        LinearLayout root = KinUi.vertical(activity);
        TextInputLayout typeLayout = KinUi.inputLayout(activity, "类型：PROP_SHARE 或 TACTIC_SHARE", false);
        TextInputLayout mapLayout = KinUi.inputLayout(activity, "地图名", false);
        TextInputLayout titleLayout = KinUi.inputLayout(activity, "标题/名称", false);
        TextInputLayout descLayout = KinUi.inputLayout(activity, "描述", true);
        root.addView(typeLayout);
        root.addView(mapLayout);
        root.addView(titleLayout);
        root.addView(descLayout);
        KinUi.margins(mapLayout, activity, 0, 10, 0, 0);
        KinUi.margins(titleLayout, activity, 0, 10, 0, 0);
        KinUi.margins(descLayout, activity, 0, 10, 0, 0);

        TextInputEditText typeEdit = KinUi.edit(typeLayout);
        TextInputEditText mapEdit = KinUi.edit(mapLayout);
        TextInputEditText titleEdit = KinUi.edit(titleLayout);
        TextInputEditText descEdit = KinUi.edit(descLayout);
        typeEdit.setText("PROP_SHARE");

        new AlertDialog.Builder(activity)
                .setTitle("新建个人条目")
                .setView(root)
                .setPositiveButton("保存", (dialog, which) -> {
                    try {
                        JSONObject payload = new JSONObject();
                        String type = stringValue(typeEdit);
                        payload.put("postType", type);
                        payload.put("mapName", stringValue(mapEdit));
                        if ("TACTIC_SHARE".equals(type)) {
                            payload.put("tacticName", stringValue(titleEdit));
                            payload.put("tacticType", "默认战术");
                            payload.put("tacticDescription", stringValue(descEdit));
                            payload.put("member1", "成员1");
                            payload.put("member1Role", "默认");
                            payload.put("member2", "成员2");
                            payload.put("member2Role", "默认");
                            payload.put("member3", "成员3");
                            payload.put("member3Role", "默认");
                            payload.put("member4", "成员4");
                            payload.put("member4Role", "默认");
                            payload.put("member5", "成员5");
                            payload.put("member5Role", "默认");
                        } else {
                            payload.put("postType", "PROP_SHARE");
                            payload.put("propName", stringValue(titleEdit));
                            payload.put("toolType", "SMOKE_GRENADE");
                            payload.put("throwMethod", stringValue(descEdit));
                            payload.put("propPosition", "自定义点位");
                            payload.put("stanceImageUrl", "");
                            payload.put("aimImageUrl", "");
                            payload.put("landingImageUrl", "");
                        }
                        activity.getRepository().createLibraryItem(payload, new ApiCallback<>() {
                            @Override
                            public void onSuccess(LibraryItem data) {
                                loadData(true);
                            }

                            @Override
                            public void onError(ApiException exception) {
                                setLoading(false, "创建失败：" + exception.getMessage());
                            }
                        });
                    } catch (Exception ignored) {
                    }
                })
                .setNegativeButton("取消", null)
                .show();
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
            return "战术";
        }
        return "道具";
    }

    private String stringValue(TextInputEditText editText) {
        return String.valueOf(editText.getText()).trim();
    }
}
