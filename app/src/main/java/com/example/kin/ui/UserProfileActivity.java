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
import com.example.kin.model.UserProfileModel;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.KinUi;
import com.example.kin.ui.future.FutureFeatureDetailActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class UserProfileActivity extends AppCompatActivity {
    private KinRepository repository;
    private LinearLayout contentLayout;
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
        toolbar.setTitle("我的主页");
        toolbar.setNavigationIcon(R.drawable.ic_nav_back);
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
        scrollView.addView(contentLayout);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        loadProfile();
    }

    private void loadProfile() {
        setLoading(true, "正在加载个人主页…");
        repository.getUserProfile("", new ApiCallback<>() {
            @Override
            public void onSuccess(UserProfileModel data) {
                render(data);
                setLoading(false, "");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, exception.isFeatureUnavailable() ? "后端个人主页接口未开放。" : "个人主页加载失败：" + exception.getMessage());
            }
        });
    }

    private void render(UserProfileModel data) {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        body.addView(KinUi.text(this, data.username, 22, true));
        body.addView(info("等级", "Lv." + Math.max(1, data.level)));
        body.addView(info("经验", data.levelProgressExperience + "/" + data.nextLevelExperience + "（总经验 " + data.experience + "）"));
        body.addView(info("发帖数", String.valueOf(data.postCount)));
        body.addView(info("通过发帖数", String.valueOf(data.approvedPostCount)));
        body.addView(info("通过率", String.format("%.2f", data.approvalRate)));
        body.addView(info("收到收藏", String.valueOf(data.favoriteReceivedCount)));
        body.addView(info("收到点赞", String.valueOf(data.likeReceivedCount)));
        body.addView(info("收到评论", String.valueOf(data.commentReceivedCount)));
        body.addView(info("连续活跃天数", String.valueOf(data.activeStreakDays)));
        body.addView(info("徽章", TextUtils.join("、", data.badges)));
        card.addView(body);
        contentLayout.addView(card);
        contentLayout.addView(growthCard());
    }

    private View growthCard() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        body.addView(KinUi.text(this, "用户成长与安全", 18, true));
        body.addView(KinUi.muted(this, "等级、经验、徽章、信誉、隐私、黑名单、安全中心、第三方登录和认证标识已接入。", 14));
        body.addView(openFeatureButton("等级/徽章/成就", "user.level_xp"));
        body.addView(openFeatureButton("隐私与黑名单", "user.privacy_settings"));
        body.addView(openFeatureButton("账号安全中心", "user.security_center"));
        card.addView(body);
        return card;
    }

    private View openFeatureButton(String label, String featureKey) {
        MaterialButton button = KinUi.outlinedButton(this, label);
        KinUi.margins(button, this, 0, 10, 0, 0);
        button.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, FutureFeatureDetailActivity.class);
            intent.putExtra(FutureFeatureDetailActivity.EXTRA_FEATURE_KEY, featureKey);
            startActivity(intent);
        });
        return button;
    }

    private View info(String label, String value) {
        TextView textView = KinUi.muted(this, label + "：" + value, 14);
        KinUi.margins(textView, this, 0, 10, 0, 0);
        return textView;
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusView.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        statusView.setText(message);
    }
}
