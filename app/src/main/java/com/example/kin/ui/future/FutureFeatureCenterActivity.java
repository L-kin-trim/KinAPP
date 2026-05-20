package com.example.kin.ui.future;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.R;
import com.example.kin.data.KinRepository;
import com.example.kin.model.FutureFeatureGroup;
import com.example.kin.model.FutureFeatureRegistry;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.FutureUi;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

public class FutureFeatureCenterActivity extends AppCompatActivity {
    private KinRepository repository;
    private LinearLayout contentLayout;
    private ProgressBar progressBar;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KinRepository(this);
        buildShell();
        render();
        syncCatalog();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("平台能力诊断");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());
        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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

    private void render() {
        contentLayout.removeAllViews();
        contentLayout.addView(progressBar);
        contentLayout.addView(statusView);
        setLoading(false, "");

        contentLayout.addView(FutureUi.featureCard(
                this,
                "全量能力目录",
                "用于管理员核对 2-15 章能力接口、记录表单、状态流转、AI/任务执行状态；普通用户入口已分散到对应页面。",
                "管理员",
                v -> showOpenApi()));

        MaterialButton taskCenter = KinUi.filledButton(this, "查看异步任务中心");
        taskCenter.setOnClickListener(v -> startActivity(new Intent(this, FutureTaskCenterActivity.class)));
        contentLayout.addView(taskCenter);

        for (FutureFeatureGroup group : FutureFeatureRegistry.groups()) {
            contentLayout.addView(FutureUi.featureCard(
                    this,
                    group.title,
                    group.summary + " · " + group.features.size() + " 个功能点",
                    group.key,
                    v -> openGroup(group.key)));
        }
    }

    private void openGroup(String groupKey) {
        Intent intent = new Intent(this, FutureFeatureDomainActivity.class);
        intent.putExtra(FutureFeatureDomainActivity.EXTRA_GROUP_KEY, groupKey);
        startActivity(intent);
    }

    private void syncCatalog() {
        setLoading(true, "正在校验后端功能目录...");
        repository.getFutureCatalog(new ApiCallback<>() {
            @Override
            public void onSuccess(JSONObject data) {
                setLoading(false, "后端功能目录可用，本地注册表已准备同步。");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, exception.isFeatureUnavailable()
                        ? "后端目录接口未开放，已使用客户端内置全量注册表。"
                        : "功能目录校验失败：" + exception.getMessage());
            }
        });
    }

    private void showOpenApi() {
        setLoading(true, "正在读取 OpenAPI...");
        repository.getOpenApi(new ApiCallback<>() {
            @Override
            public void onSuccess(String data) {
                new androidx.appcompat.app.AlertDialog.Builder(FutureFeatureCenterActivity.this)
                        .setTitle("OpenAPI 骨架")
                        .setMessage(data.length() > 4000 ? data.substring(0, 4000) + "\n..." : data)
                        .setPositiveButton("关闭", null)
                        .show();
                setLoading(false, "");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, exception.isFeatureUnavailable() ? "OpenAPI 骨架接口未开放。" : "OpenAPI 读取失败：" + exception.getMessage());
            }
        });
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        statusView.setVisibility(message == null || message.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
        statusView.setText(message == null ? "" : message);
    }
}
