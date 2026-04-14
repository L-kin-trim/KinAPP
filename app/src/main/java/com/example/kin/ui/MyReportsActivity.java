package com.example.kin.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.R;
import com.example.kin.data.KinRepository;
import com.example.kin.model.PageResult;
import com.example.kin.model.ReportModel;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class MyReportsActivity extends AppCompatActivity {
    private KinRepository repository;
    private LinearLayout listLayout;
    private ProgressBar progressBar;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KinRepository(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));
        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("我的举报");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());
        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = KinUi.vertical(this);
        content.setPadding(KinUi.dp(this, 18), KinUi.dp(this, 12), KinUi.dp(this, 18), KinUi.dp(this, 24));
        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        statusView = KinUi.muted(this, "", 13);
        statusView.setVisibility(View.GONE);
        content.addView(progressBar);
        content.addView(statusView);
        listLayout = KinUi.vertical(this);
        content.addView(listLayout);
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        loadReports();
    }

    private void loadReports() {
        setLoading(true, "正在读取举报记录…");
        listLayout.removeAllViews();
        repository.getMyReports(0, 30, new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<ReportModel> data) {
                for (ReportModel report : data.items) {
                    listLayout.addView(reportCard(report));
                }
                setLoading(false, data.items.isEmpty() ? "暂无举报记录。" : "");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, exception.isFeatureUnavailable() ? "后端举报接口未开放。" : "举报记录加载失败：" + exception.getMessage());
            }
        });
    }

    private View reportCard(ReportModel report) {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 16);
        body.addView(KinUi.text(this, report.targetType + " #" + report.targetId, 16, true));
        TextView meta = KinUi.muted(this, report.reasonType + " · " + report.status, 12);
        KinUi.margins(meta, this, 0, 8, 0, 0);
        body.addView(meta);
        TextView detail = KinUi.muted(this, report.reasonDetail, 14);
        KinUi.margins(detail, this, 0, 10, 0, 0);
        body.addView(detail);
        card.addView(body);
        return card;
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusView.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        statusView.setText(message);
    }
}
