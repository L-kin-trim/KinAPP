package com.example.kin.ui;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.R;
import com.example.kin.data.KinRepository;
import com.example.kin.model.DraftModel;
import com.example.kin.model.ForumPostModel;
import com.example.kin.model.ImageUploadItem;
import com.example.kin.model.PageResult;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PublishEditorActivity extends AppCompatActivity {
    private static final String CACHE_KEY = "publish_forum_post";

    private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoSaveRunnable = () -> saveDraft(true);

    private KinRepository repository;
    private LinearLayout contentLayout;
    private ProgressBar progressBar;
    private TextView statusView;
    private TextView draftSummary;
    private String currentType = "PROP_SHARE";
    private long remoteDraftId;

    private LinearLayout propSection;
    private LinearLayout tacticSection;
    private LinearLayout dailySection;
    private final List<Chip> typeChips = new ArrayList<>();

    private TextInputEditText propNameEdit;
    private TextInputEditText mapNameEdit;
    private TextInputEditText toolTypeEdit;
    private TextInputEditText throwMethodEdit;
    private TextInputEditText propPositionEdit;
    private TextInputEditText tacticNameEdit;
    private TextInputEditText tacticTypeEdit;
    private TextInputEditText tacticDescriptionEdit;
    private TextInputEditText contentEdit;
    private final List<TextInputEditText> memberEdits = new ArrayList<>();
    private final List<TextInputEditText> roleEdits = new ArrayList<>();

    private Uri stanceUri;
    private Uri aimUri;
    private Uri landingUri;
    private final List<Uri> galleryUris = new ArrayList<>();
    private TextView stanceState;
    private TextView aimState;
    private TextView landingState;
    private TextView galleryState;

    private final ActivityResultLauncher<String> stancePicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                stanceUri = uri;
                updateImageState();
                scheduleAutoSave();
            });
    private final ActivityResultLauncher<String> aimPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                aimUri = uri;
                updateImageState();
                scheduleAutoSave();
            });
    private final ActivityResultLauncher<String> landingPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                landingUri = uri;
                updateImageState();
                scheduleAutoSave();
            });
    private final ActivityResultLauncher<String> galleryPicker = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                galleryUris.clear();
                if (uris.size() > 10) {
                    galleryUris.addAll(uris.subList(0, 10));
                } else {
                    galleryUris.addAll(uris);
                }
                updateImageState();
                scheduleAutoSave();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KinRepository(this);
        buildUi();
        hydrateDrafts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("发布帖子");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());
        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(this);
        contentLayout = KinUi.vertical(this);
        contentLayout.setPadding(KinUi.dp(this, 18), KinUi.dp(this, 12), KinUi.dp(this, 18), KinUi.dp(this, 24));
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        statusView = KinUi.muted(this, "", 13);
        statusView.setVisibility(View.GONE);
        contentLayout.addView(progressBar);
        contentLayout.addView(statusView);
        contentLayout.addView(buildDraftCard());
        contentLayout.addView(buildTypeHeader());
        contentLayout.addView(buildForm());
        scrollView.addView(contentLayout);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        updateTypeUi();
        updateImageState();
    }

    private View buildDraftCard() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        body.addView(KinUi.text(this, "草稿箱", 20, true));
        draftSummary = KinUi.muted(this, "正在读取草稿…", 14);
        KinUi.margins(draftSummary, this, 0, 8, 0, 0);
        body.addView(draftSummary);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton saveButton = KinUi.filledButton(this, "立即保存草稿");
        MaterialButton refreshButton = KinUi.outlinedButton(this, "刷新草稿");
        MaterialButton clearButton = KinUi.outlinedButton(this, "清空本地草稿");
        saveButton.setOnClickListener(v -> saveDraft(false));
        refreshButton.setOnClickListener(v -> hydrateDrafts());
        clearButton.setOnClickListener(v -> clearLocalDraft());
        row.addView(saveButton);
        row.addView(refreshButton);
        row.addView(clearButton);
        KinUi.margins(refreshButton, this, 10, 0, 0, 0);
        KinUi.margins(clearButton, this, 10, 0, 0, 0);
        KinUi.margins(row, this, 0, 16, 0, 0);
        body.addView(row);
        card.addView(body);
        return card;
    }

    private View buildTypeHeader() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        body.addView(KinUi.text(this, "帖子类型", 18, true));
        LinearLayout chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.addView(typeChip("道具分享帖", "PROP_SHARE"));
        chipsRow.addView(typeChip("战术分享帖", "TACTIC_SHARE"));
        chipsRow.addView(typeChip("日常闲聊帖", "DAILY_CHAT"));
        KinUi.margins(chipsRow, this, 0, 14, 0, 0);
        body.addView(chipsRow);
        card.addView(body);
        return card;
    }

    private Chip typeChip(String label, String type) {
        Chip chip = KinUi.chip(this, label);
        chip.setCheckable(true);
        chip.setOnClickListener(v -> {
            currentType = type;
            updateTypeUi();
            scheduleAutoSave();
        });
        typeChips.add(chip);
        return chip;
    }

    private View buildForm() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);

        mapNameEdit = addInput(body, "地图", false);

        propSection = KinUi.vertical(this);
        propNameEdit = addInput(propSection, "道具名称", false);
        toolTypeEdit = addInput(propSection, "道具类型", false);
        throwMethodEdit = addInput(propSection, "投掷方式", false);
        propPositionEdit = addInput(propSection, "道具位置", true);
        stanceState = imageRow(propSection, "站位图", () -> stancePicker.launch("image/*"));
        aimState = imageRow(propSection, "瞄点图", () -> aimPicker.launch("image/*"));
        landingState = imageRow(propSection, "落点图", () -> landingPicker.launch("image/*"));

        tacticSection = KinUi.vertical(this);
        tacticNameEdit = addInput(tacticSection, "战术名称", false);
        tacticTypeEdit = addInput(tacticSection, "战术类型", false);
        tacticDescriptionEdit = addInput(tacticSection, "战术描述", true);
        for (int i = 1; i <= 5; i++) {
            memberEdits.add(addInput(tacticSection, "成员" + i, false));
            roleEdits.add(addInput(tacticSection, "成员" + i + "角色/任务", false));
        }

        dailySection = KinUi.vertical(this);
        contentEdit = addInput(dailySection, "内容", true);
        galleryState = imageRow(dailySection, "图片上传（最多 10 张）", () -> galleryPicker.launch("image/*"));

        body.addView(propSection);
        KinUi.margins(tacticSection, this, 0, 14, 0, 0);
        body.addView(tacticSection);
        KinUi.margins(dailySection, this, 0, 14, 0, 0);
        body.addView(dailySection);

        MaterialButton submitButton = KinUi.filledButton(this, "提交帖子");
        submitButton.setOnClickListener(v -> submit());
        KinUi.margins(submitButton, this, 0, 16, 0, 0);
        body.addView(submitButton);
        card.addView(body);
        return card;
    }

    private TextInputEditText addInput(LinearLayout parent, String hint, boolean multiline) {
        TextInputLayout layout = KinUi.inputLayout(this, hint, multiline);
        if (parent.getChildCount() > 0) {
            KinUi.margins(layout, this, 0, 10, 0, 0);
        }
        parent.addView(layout);
        TextInputEditText editText = KinUi.edit(layout);
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { scheduleAutoSave(); }
        });
        return editText;
    }

    private TextView imageRow(LinearLayout parent, String label, Runnable picker) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView state = KinUi.muted(this, label + "：未选择", 13);
        MaterialButton button = KinUi.outlinedButton(this, "选择");
        button.setOnClickListener(v -> picker.run());
        row.addView(state);
        row.addView(button);
        KinUi.margins(button, this, 10, 0, 0, 0);
        KinUi.margins(row, this, 0, 10, 0, 0);
        parent.addView(row);
        return state;
    }

    private void hydrateDrafts() {
        DraftModel localDraft = repository.getLocalDraftStore().get(CACHE_KEY);
        if (localDraft != null && !TextUtils.isEmpty(localDraft.payloadJson)) {
            draftSummary.setText("已恢复本地草稿。");
            remoteDraftId = localDraft.remoteDraftId;
            applyPayload(localDraft.payloadJson);
            return;
        }
        repository.getDrafts("FORUM_POST", 0, 1, new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<DraftModel> data) {
                if (data.items.isEmpty()) {
                    draftSummary.setText("暂无服务端草稿。");
                    return;
                }
                DraftModel draft = data.items.get(0);
                remoteDraftId = draft.id;
                draftSummary.setText("已载入最近服务端草稿：" + draft.title);
                applyPayload(draft.payloadJson);
            }

            @Override
            public void onError(ApiException exception) {
                draftSummary.setText(exception.isFeatureUnavailable() ? "后端草稿接口未开放。" : "草稿读取失败：" + exception.getMessage());
            }
        });
    }

    private void applyPayload(String payloadJson) {
        try {
            JSONObject payload = new JSONObject(payloadJson);
            currentType = payload.optString("postType", currentType);
            mapNameEdit.setText(payload.optString("mapName"));
            propNameEdit.setText(payload.optString("propName"));
            toolTypeEdit.setText(payload.optString("toolType"));
            throwMethodEdit.setText(payload.optString("throwMethod"));
            propPositionEdit.setText(payload.optString("propPosition"));
            tacticNameEdit.setText(payload.optString("tacticName"));
            tacticTypeEdit.setText(payload.optString("tacticType"));
            tacticDescriptionEdit.setText(payload.optString("tacticDescription"));
            contentEdit.setText(payload.optString("content"));
            for (int i = 0; i < 5; i++) {
                memberEdits.get(i).setText(payload.optString("member" + (i + 1)));
                roleEdits.get(i).setText(payload.optString("member" + (i + 1) + "Role"));
            }
            updateTypeUi();
        } catch (Exception ignored) {
        }
    }

    private void updateTypeUi() {
        for (Chip chip : typeChips) {
            String text = String.valueOf(chip.getText());
            boolean checked = ("PROP_SHARE".equals(currentType) && text.contains("道具"))
                    || ("TACTIC_SHARE".equals(currentType) && text.contains("战术"))
                    || ("DAILY_CHAT".equals(currentType) && text.contains("日常"));
            chip.setChecked(checked);
        }
        propSection.setVisibility("PROP_SHARE".equals(currentType) ? View.VISIBLE : View.GONE);
        tacticSection.setVisibility("TACTIC_SHARE".equals(currentType) ? View.VISIBLE : View.GONE);
        dailySection.setVisibility("DAILY_CHAT".equals(currentType) || "OTHER".equals(currentType) ? View.VISIBLE : View.GONE);
    }

    private void updateImageState() {
        if (stanceState != null) {
            stanceState.setText("站位图：" + (stanceUri == null ? "未选择" : "已选择"));
        }
        if (aimState != null) {
            aimState.setText("瞄点图：" + (aimUri == null ? "未选择" : "已选择"));
        }
        if (landingState != null) {
            landingState.setText("落点图：" + (landingUri == null ? "未选择" : "已选择"));
        }
        if (galleryState != null) {
            galleryState.setText("图片上传（最多 10 张）：" + galleryUris.size() + " 张");
        }
    }

    private void scheduleAutoSave() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        autoSaveHandler.postDelayed(autoSaveRunnable, 1800L);
    }

    private void clearLocalDraft() {
        repository.getLocalDraftStore().delete(CACHE_KEY);
        remoteDraftId = 0L;
        draftSummary.setText("本地草稿已清空。");
    }

    private void saveDraft(boolean autoSaved) {
        JSONObject payload = buildPayload();
        String title = buildDraftTitle();
        repository.getLocalDraftStore().save(CACHE_KEY, title, payload.toString(), remoteDraftId);
        repository.saveDraft(remoteDraftId, "FORUM_POST", title, payload.toString(), autoSaved, new ApiCallback<>() {
            @Override
            public void onSuccess(DraftModel data) {
                remoteDraftId = data.id;
                repository.getLocalDraftStore().save(CACHE_KEY, title, payload.toString(), remoteDraftId);
                draftSummary.setText((autoSaved ? "自动保存" : "手动保存") + "成功：" + title);
            }

            @Override
            public void onError(ApiException exception) {
                if (exception.isFeatureUnavailable()) {
                    draftSummary.setText("已保存到本地，服务端草稿接口未开放。");
                    return;
                }
                draftSummary.setText("草稿保存失败：" + exception.getMessage());
            }
        });
    }

    private void submit() {
        if (!repository.getSessionManager().isLoggedIn()) {
            setLoading(false, "请先登录再发帖。");
            return;
        }
        setLoading(true, "正在处理图片并提交…");
        if ("PROP_SHARE".equals(currentType)) {
            submitPropPost();
        } else if ("TACTIC_SHARE".equals(currentType)) {
            submitTacticPost();
        } else {
            submitDailyPost();
        }
    }

    private void submitPropPost() {
        if (stanceUri == null || aimUri == null || landingUri == null) {
            setLoading(false, "道具分享帖需要完整的站位图、瞄点图、落点图。");
            return;
        }
        repository.uploadSingleImage(stanceUri, "prop-share", new ApiCallback<>() {
            @Override
            public void onSuccess(ImageUploadItem stance) {
                repository.uploadSingleImage(aimUri, "prop-share", new ApiCallback<>() {
                    @Override
                    public void onSuccess(ImageUploadItem aim) {
                        repository.uploadSingleImage(landingUri, "prop-share", new ApiCallback<>() {
                            @Override
                            public void onSuccess(ImageUploadItem landing) {
                                JSONObject payload = buildPayload();
                                try {
                                    payload.put("stanceImageUrl", stance.url);
                                    payload.put("aimImageUrl", aim.url);
                                    payload.put("landingImageUrl", landing.url);
                                } catch (Exception ignored) {
                                }
                                createPost(payload);
                            }

                            @Override
                            public void onError(ApiException exception) {
                                setLoading(false, "落点图上传失败：" + exception.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onError(ApiException exception) {
                        setLoading(false, "瞄点图上传失败：" + exception.getMessage());
                    }
                });
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "站位图上传失败：" + exception.getMessage());
            }
        });
    }

    private void submitTacticPost() {
        createPost(buildPayload());
    }

    private void submitDailyPost() {
        if (galleryUris.isEmpty()) {
            createPost(buildPayload());
            return;
        }
        repository.uploadBatchImages(galleryUris, "posts", new ApiCallback<>() {
            @Override
            public void onSuccess(List<ImageUploadItem> data) {
                JSONArray urls = new JSONArray();
                for (ImageUploadItem item : data) {
                    urls.put(item.url);
                }
                JSONObject payload = buildPayload();
                try {
                    payload.put("imageUrls", urls);
                } catch (Exception ignored) {
                }
                createPost(payload);
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "帖子图片上传失败：" + exception.getMessage());
            }
        });
    }

    private void createPost(JSONObject payload) {
        repository.createPost(payload, new ApiCallback<>() {
            @Override
            public void onSuccess(ForumPostModel data) {
                repository.getLocalDraftStore().delete(CACHE_KEY);
                setLoading(false, "发布成功，已进入待审核状态。");
                Intent intent = new Intent(PublishEditorActivity.this, PostDetailActivity.class);
                intent.putExtra(PostDetailActivity.EXTRA_POST_ID, data.id);
                intent.putExtra(PostDetailActivity.EXTRA_MINE, true);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "发布失败：" + exception.getMessage());
            }
        });
    }

    private JSONObject buildPayload() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("postType", currentType);
            if (remoteDraftId > 0) {
                payload.put("draftId", remoteDraftId);
            }
            if ("PROP_SHARE".equals(currentType)) {
                payload.put("mapName", text(mapNameEdit));
                payload.put("propName", text(propNameEdit));
                payload.put("toolType", text(toolTypeEdit));
                payload.put("throwMethod", text(throwMethodEdit));
                payload.put("propPosition", text(propPositionEdit));
                payload.put("stanceImageUrl", "");
                payload.put("aimImageUrl", "");
                payload.put("landingImageUrl", "");
            } else if ("TACTIC_SHARE".equals(currentType)) {
                payload.put("mapName", text(mapNameEdit));
                payload.put("tacticName", text(tacticNameEdit));
                payload.put("tacticType", text(tacticTypeEdit));
                payload.put("tacticDescription", text(tacticDescriptionEdit));
                for (int i = 0; i < 5; i++) {
                    payload.put("member" + (i + 1), text(memberEdits.get(i)));
                    payload.put("member" + (i + 1) + "Role", text(roleEdits.get(i)));
                }
            } else {
                payload.put("content", text(contentEdit));
                payload.put("imageUrls", new JSONArray());
            }
        } catch (Exception ignored) {
        }
        return payload;
    }

    private String buildDraftTitle() {
        if ("PROP_SHARE".equals(currentType)) {
            return emptyFallback(text(propNameEdit), "道具分享草稿");
        }
        if ("TACTIC_SHARE".equals(currentType)) {
            return emptyFallback(text(tacticNameEdit), "战术分享草稿");
        }
        return emptyFallback(text(contentEdit), "日常闲聊草稿");
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusView.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
        statusView.setText(message);
    }

    private String text(TextInputEditText editText) {
        return String.valueOf(editText.getText()).trim();
    }

    private String emptyFallback(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}
