package com.example.kin.ui;

import android.content.Intent;
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
import com.example.kin.model.DraftModel;
import com.example.kin.model.PageResult;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class DraftsActivity extends AppCompatActivity {
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
        toolbar.setTitle("草稿箱");
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDrafts();
    }

    private void loadDrafts() {
        setLoading(true, "正在读取草稿…");
        listLayout.removeAllViews();
        DraftModel local = repository.getLocalDraftStore().get("publish_forum_post");
        if (local != null && !TextUtils.isEmpty(local.payloadJson)) {
            listLayout.addView(draftCard(local, true));
        }
        repository.getDrafts("FORUM_POST", 0, 20, new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<DraftModel> data) {
                for (DraftModel draft : data.items) {
                    listLayout.addView(draftCard(draft, false));
                }
                setLoading(false, listLayout.getChildCount() == 0 ? "暂无草稿。" : "");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, exception.isFeatureUnavailable() ? "后端草稿接口未开放，仅显示本地草稿。" : "草稿加载失败：" + exception.getMessage());
            }
        });
    }

    private View draftCard(DraftModel draft, boolean local) {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 16);
        body.addView(KinUi.text(this, local ? "本地草稿" : draft.title, 17, true));
        TextView subtitle = KinUi.muted(this, local ? "保存在本地数据库" : "服务端草稿", 13);
        KinUi.margins(subtitle, this, 0, 8, 0, 0);
        body.addView(subtitle);
        MaterialButton editButton = KinUi.filledButton(this, "继续编辑");
        editButton.setOnClickListener(v -> startActivity(new Intent(this, PublishEditorActivity.class)));
        KinUi.margins(editButton, this, 0, 14, 0, 0);
        body.addView(editButton);
        card.addView(body);
        return card;
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusView.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
        statusView.setText(message);
    }
}
