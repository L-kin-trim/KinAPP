package com.example.kin.ui.future;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.R;
import com.example.kin.data.KinRepository;
import com.example.kin.model.FutureAsyncTask;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.FutureUi;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class FutureTaskCenterActivity extends AppCompatActivity {
    private KinRepository repository;
    private LinearLayout contentLayout;
    private ProgressBar progressBar;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KinRepository(this);
        buildShell();
        loadTasks();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));
        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("异步任务中心");
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

    private void loadTasks() {
        setLoading(true, "正在同步视频、AI、导出、扫描等异步任务...");
        repository.getFutureTasks("", "", new ApiCallback<>() {
            @Override
            public void onSuccess(List<FutureAsyncTask> data) {
                renderTasks(data);
                setLoading(false, data.isEmpty() ? "暂无异步任务。" : "");
            }

            @Override
            public void onError(ApiException exception) {
                contentLayout.addView(FutureUi.statusCard(FutureTaskCenterActivity.this,
                        "任务中心不可用",
                        exception.isFeatureUnavailable() ? "后端任务接口未开放。" : exception.getMessage(),
                        v -> loadTasks()));
                setLoading(false, "");
            }
        });
    }

    private void renderTasks(List<FutureAsyncTask> tasks) {
        contentLayout.removeAllViews();
        contentLayout.addView(progressBar);
        contentLayout.addView(statusView);
        MaterialButton refresh = KinUi.filledButton(this, "刷新任务");
        refresh.setOnClickListener(v -> loadTasks());
        contentLayout.addView(refresh);

        for (FutureAsyncTask task : tasks) {
            MaterialCardView card = KinUi.card(this);
            LinearLayout body = KinUi.sectionContainer(this, 16);
            body.addView(KinUi.text(this, TextUtils.isEmpty(task.title) ? task.taskType : task.title, 17, true));
            body.addView(KinUi.muted(this, task.featureKey + " · " + task.status + " · " + task.progressPercent + "%", 13));
            if (!TextUtils.isEmpty(task.failureReason)) {
                TextView error = KinUi.muted(this, "失败原因：" + task.failureReason, 13);
                KinUi.margins(error, this, 0, 8, 0, 0);
                body.addView(error);
            }
            MaterialButton retry = KinUi.outlinedButton(this, "重试任务");
            retry.setOnClickListener(v -> repository.retryFutureTask(TextUtils.isEmpty(task.taskId) ? String.valueOf(task.id) : task.taskId, new ApiCallback<>() {
                @Override
                public void onSuccess(FutureAsyncTask data) {
                    loadTasks();
                }

                @Override
                public void onError(ApiException exception) {
                    setLoading(false, "重试失败：" + exception.getMessage());
                }
            }));
            KinUi.margins(retry, this, 0, 12, 0, 0);
            body.addView(retry);
            card.addView(body);
            contentLayout.addView(card);
        }
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        statusView.setVisibility(TextUtils.isEmpty(message) ? android.view.View.GONE : android.view.View.VISIBLE);
        statusView.setText(message);
    }
}
