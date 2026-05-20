package com.example.kin.ui.future;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.R;
import com.example.kin.data.KinRepository;
import com.example.kin.model.FutureAsyncTask;
import com.example.kin.model.FutureFeatureDefinition;
import com.example.kin.model.FutureFeatureFormField;
import com.example.kin.model.FutureFeatureRecord;
import com.example.kin.model.FutureFeatureRegistry;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.FutureUi;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FutureFeatureDetailActivity extends AppCompatActivity {
    public static final String EXTRA_FEATURE_KEY = "extra_feature_key";

    private KinRepository repository;
    private FutureFeatureDefinition feature;
    private LinearLayout contentLayout;
    private LinearLayout recordLayout;
    private ProgressBar progressBar;
    private TextView statusView;
    private FutureTacticBoardView tacticBoardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KinRepository(this);
        feature = FutureFeatureRegistry.featureByKey(getIntent().getStringExtra(EXTRA_FEATURE_KEY));
        if (feature == null) {
            finish();
            return;
        }
        buildShell();
        renderStatic();
        loadRecords();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle(feature.title);
        toolbar.setSubtitle(feature.section + " · " + feature.key);
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());
        root.addView(toolbar);

        ScrollView scrollView = new ScrollView(this);
        contentLayout = KinUi.vertical(this);
        contentLayout.setPadding(KinUi.dp(this, 18), KinUi.dp(this, 12), KinUi.dp(this, 18), KinUi.dp(this, 24));
        progressBar = new ProgressBar(this);
        statusView = KinUi.muted(this, "", 13);
        contentLayout.addView(progressBar);
        contentLayout.addView(statusView);
        scrollView.addView(contentLayout);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void renderStatic() {
        contentLayout.addView(FutureUi.featureCard(this,
                feature.title,
                feature.summary,
                feature.aiEnabled ? "AI 可用" : "记录",
                v -> showRecordDialog(null)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        MaterialButton create = KinUi.filledButton(this, "新建能力记录");
        create.setOnClickListener(v -> showRecordDialog(null));
        MaterialButton quality = KinUi.outlinedButton(this, "质量检测");
        quality.setOnClickListener(v -> runQualityCheck());
        MaterialButton ai = KinUi.outlinedButton(this, "AI 生成方案");
        ai.setEnabled(feature.aiEnabled);
        ai.setOnClickListener(v -> runAiGenerate());
        MaterialButton task = KinUi.outlinedButton(this, "创建异步任务");
        task.setEnabled(feature.taskEnabled);
        task.setOnClickListener(v -> createTask());
        actions.addView(create);
        actions.addView(quality);
        actions.addView(ai);
        actions.addView(task);
        KinUi.margins(quality, this, 0, 10, 0, 0);
        KinUi.margins(ai, this, 0, 10, 0, 0);
        KinUi.margins(task, this, 0, 10, 0, 0);
        contentLayout.addView(actions);

        if ("cs2".equals(feature.groupKey)) {
            contentLayout.addView(buildCs2CanvasCard());
        }

        contentLayout.addView(KinUi.text(this, "能力记录", 20, true));
        recordLayout = KinUi.vertical(this);
        contentLayout.addView(recordLayout);
    }

    private MaterialCardView buildCs2CanvasCard() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 16);
        body.addView(KinUi.text(this, "CS2 地图/战术板画布", 18, true));
        TextView hint = KinUi.muted(this, "点击画布添加队员/道具点，保存时会写入 payloadJson.points。", 13);
        KinUi.margins(hint, this, 0, 8, 0, 0);
        body.addView(hint);
        tacticBoardView = new FutureTacticBoardView(this);
        KinUi.margins(tacticBoardView, this, 0, 12, 0, 0);
        body.addView(tacticBoardView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, KinUi.dp(this, 260)));
        MaterialButton save = KinUi.outlinedButton(this, "保存画布示例记录");
        save.setOnClickListener(v -> saveCanvasRecord());
        KinUi.margins(save, this, 0, 12, 0, 0);
        body.addView(save);
        card.addView(body);
        return card;
    }

    private void loadRecords() {
        setLoading(true, "正在同步 " + feature.title + "...");
        repository.getFutureRecords(feature, "", "", new ApiCallback<>() {
            @Override
            public void onSuccess(List<FutureFeatureRecord> data) {
                renderRecords(data);
                setLoading(false, data.isEmpty() ? "暂无记录，可新建第一条。" : "");
            }

            @Override
            public void onError(ApiException exception) {
                recordLayout.removeAllViews();
                recordLayout.addView(FutureUi.statusCard(FutureFeatureDetailActivity.this,
                        "记录接口不可用",
                        exception.isFeatureUnavailable() ? "后端通用 CRUD 尚未开放。" : exception.getMessage(),
                        v -> loadRecords()));
                setLoading(false, "");
            }
        });
    }

    private void renderRecords(List<FutureFeatureRecord> records) {
        recordLayout.removeAllViews();
        for (FutureFeatureRecord record : records) {
            recordLayout.addView(FutureUi.recordCard(
                    this,
                    feature,
                    record,
                    v -> showRecordDialog(record),
                    v -> showStatusDialog(record),
                    v -> deleteRecord(record)));
        }
    }

    private void showRecordDialog(FutureFeatureRecord record) {
        LinearLayout root = KinUi.vertical(this);
        TextInputLayout titleLayout = KinUi.inputLayout(this, "标题", false);
        TextInputLayout summaryLayout = KinUi.inputLayout(this, "摘要", true);
        TextInputEditText titleEdit = KinUi.edit(titleLayout);
        TextInputEditText summaryEdit = KinUi.edit(summaryLayout);
        titleEdit.setText(record == null ? feature.title : record.title);
        summaryEdit.setText(record == null ? feature.summary : record.summary);
        root.addView(titleLayout);
        root.addView(summaryLayout);
        KinUi.margins(summaryLayout, this, 0, 10, 0, 0);

        JSONObject currentPayload = parsePayload(record == null ? "" : record.payloadJson);
        Map<String, TextInputEditText> edits = new LinkedHashMap<>();
        for (FutureFeatureFormField field : feature.fields) {
            TextInputLayout layout = KinUi.inputLayout(this, field.label, field.multiline);
            TextInputEditText editText = KinUi.edit(layout);
            editText.setText(currentPayload.optString(field.key, field.defaultValue));
            edits.put(field.key, editText);
            KinUi.margins(layout, this, 0, 10, 0, 0);
            root.addView(layout);
        }

        new AlertDialog.Builder(this)
                .setTitle(record == null ? "新建能力记录" : "编辑能力记录")
                .setView(root)
                .setPositiveButton("保存", (dialog, which) -> saveRecord(record, titleEdit, summaryEdit, edits))
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveRecord(FutureFeatureRecord record,
                            TextInputEditText titleEdit,
                            TextInputEditText summaryEdit,
                            Map<String, TextInputEditText> edits) {
        JSONObject payload = new JSONObject();
        try {
            for (Map.Entry<String, TextInputEditText> entry : edits.entrySet()) {
                payload.put(entry.getKey(), text(entry.getValue()));
            }
            payload.put("featureKey", feature.apiFeatureKey);
            payload.put("uiFeatureKey", feature.key);
            payload.put("section", feature.section);
        } catch (Exception ignored) {
        }
        ApiCallback<FutureFeatureRecord> callback = new ApiCallback<>() {
            @Override
            public void onSuccess(FutureFeatureRecord data) {
                loadRecords();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "保存失败：" + exception.getMessage());
            }
        };
        if (record == null) {
            repository.createFutureRecord(feature, text(titleEdit), text(summaryEdit), payload, callback);
        } else {
            repository.updateFutureRecord(record.id, feature, text(titleEdit), text(summaryEdit), record.status, payload, callback);
        }
    }

    private void showStatusDialog(FutureFeatureRecord record) {
        String[] statuses = {"DRAFT", "IN_PROGRESS", "READY", "PUBLISHED", "ARCHIVED"};
        new AlertDialog.Builder(this)
                .setTitle("流转状态")
                .setItems(statuses, (dialog, which) -> repository.updateFutureRecordStatus(feature, record.id, statuses[which], new ApiCallback<>() {
                    @Override
                    public void onSuccess(FutureFeatureRecord data) {
                        loadRecords();
                    }

                    @Override
                    public void onError(ApiException exception) {
                        setLoading(false, "状态更新失败：" + exception.getMessage());
                    }
                }))
                .show();
    }

    private void deleteRecord(FutureFeatureRecord record) {
        repository.deleteFutureRecord(feature, record.id, new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                loadRecords();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "删除失败：" + exception.getMessage());
            }
        });
    }

    private void runQualityCheck() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("featureKey", feature.apiFeatureKey);
            payload.put("uiFeatureKey", feature.key);
            payload.put("title", feature.title);
            payload.put("content", feature.summary);
        } catch (Exception ignored) {
        }
        setLoading(true, "正在进行质量检测...");
        repository.qualityCheckFutureContent(payload, new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                showJsonDialog("质量检测结果", data);
                setLoading(false, "");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, exception.isFeatureUnavailable() ? "质量检测接口未开放。" : "质量检测失败：" + exception.getMessage());
            }
        });
    }

    private void runAiGenerate() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("featureKey", feature.apiFeatureKey);
            payload.put("uiFeatureKey", feature.key);
            payload.put("title", feature.title);
            payload.put("summary", feature.summary);
        } catch (Exception ignored) {
        }
        setLoading(true, "AI 正在生成方案...");
        ApiCallback<JSONObject> callback = new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                showJsonDialog("AI 生成结果", data);
                setLoading(false, "");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, exception.isFeatureUnavailable() ? "AI 生成接口未开放。" : "AI 生成失败：" + exception.getMessage());
            }
        };
        if ("ai-chat".equals(feature.apiFeatureKey)) {
            repository.aiChat(payload, callback);
            return;
        }
        if ("ai-utility-recommendation".equals(feature.apiFeatureKey)) {
            repository.aiRecommendUtility(payload, callback);
            return;
        }
        if ("ai-search-answer".equals(feature.apiFeatureKey)) {
            repository.aiSearchAnswer(payload, callback);
            return;
        }
        repository.aiGenerate(feature.apiFeatureKey, payload, callback);
    }

    private void createTask() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("featureKey", feature.apiFeatureKey);
            payload.put("uiFeatureKey", feature.key);
            payload.put("summary", feature.summary);
        } catch (Exception ignored) {
        }
        repository.createFutureTask(feature.apiFeatureKey, "FEATURE_ASYNC", feature.title, payload, new ApiCallback<>() {
            @Override
            public void onSuccess(FutureAsyncTask data) {
                showJsonDialog("异步任务已创建", taskJson(data));
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, exception.isFeatureUnavailable() ? "任务接口未开放。" : "任务创建失败：" + exception.getMessage());
            }
        });
    }

    private void saveCanvasRecord() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("points", tacticBoardView == null ? new org.json.JSONArray() : tacticBoardView.exportPoints());
            payload.put("details", "CS2 画布点位/路线 JSON");
        } catch (Exception ignored) {
        }
        repository.createFutureRecord(feature, feature.title + " 画布记录", "从交互式画布保存的点位/路线。", payload, new ApiCallback<>() {
            @Override
            public void onSuccess(FutureFeatureRecord data) {
                loadRecords();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "画布记录保存失败：" + exception.getMessage());
            }
        });
    }

    private JSONObject taskJson(FutureAsyncTask task) {
        JSONObject json = new JSONObject();
        try {
            json.put("taskId", task.taskId);
            json.put("featureKey", task.featureKey);
            json.put("status", task.status);
            json.put("progressPercent", task.progressPercent);
        } catch (Exception ignored) {
        }
        return json;
    }

    private JSONObject parsePayload(String payloadJson) {
        if (TextUtils.isEmpty(payloadJson)) {
            return new JSONObject();
        }
        try {
            return new JSONObject(payloadJson);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }

    private void showJsonDialog(String title, JSONObject data) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(data == null ? "{}" : data.toString())
                .setPositiveButton("关闭", null)
                .show();
    }

    private String text(TextInputEditText editText) {
        return String.valueOf(editText.getText()).trim();
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusView.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        statusView.setText(message);
    }
}
