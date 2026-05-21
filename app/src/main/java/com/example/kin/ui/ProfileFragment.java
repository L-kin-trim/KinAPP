package com.example.kin.ui;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.kin.R;
import com.example.kin.MainActivity;
import com.example.kin.data.SessionManager;
import com.example.kin.model.SessionUser;
import com.example.kin.ui.admin.AdminCenterActivity;
import com.example.kin.ui.common.BasePageFragment;
import com.example.kin.ui.common.KinUi;
import com.example.kin.ui.future.FutureFeatureCenterActivity;
import com.example.kin.ui.future.FutureFeatureDetailActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class ProfileFragment extends BasePageFragment {
    @Override
    protected void onPageReady() {
        render();
    }

    @Override
    public void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        MainActivity activity = (MainActivity) requireActivity();
        activity.setTopBar("\u6211", "");
        contentLayout.removeAllViews();
        contentLayout.addView(progressBar);
        contentLayout.addView(statusView);

        SessionManager sessionManager = activity.getRepository().getSessionManager();
        SessionUser user = sessionManager.getUser();

        contentLayout.addView(profileCard(activity, sessionManager, user));
        contentLayout.addView(updateCard(activity));
        contentLayout.addView(actionsCard(activity, sessionManager, user));
        setLoading(false, "");
    }

    private View profileCard(MainActivity activity, SessionManager sessionManager, SessionUser user) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 18);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        String username = sessionManager.isLoggedIn() && user != null ? user.username : "\u672a\u767b\u5f55";
        TextView avatar = KinUi.text(activity, avatarText(username), 24, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTextColor(activity.getColor(R.color.kin_text_inverse));
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(activity.getColor(R.color.kin_accent));
        avatar.setBackground(avatarBg);
        row.addView(avatar, new LinearLayout.LayoutParams(KinUi.dp(activity, 72), KinUi.dp(activity, 72)));

        LinearLayout info = KinUi.vertical(activity);
        TextView name = KinUi.text(activity, username, 24, true);
        TextView level = KinUi.muted(activity, "Lv.0", 15);
        info.addView(name);
        KinUi.margins(level, activity, 0, 6, 0, 0);
        info.addView(level);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        infoParams.leftMargin = KinUi.dp(activity, 16);
        row.addView(info, infoParams);

        body.addView(row);
        card.addView(body);
        return card;
    }

    private String avatarText(String value) {
        if (TextUtils.isEmpty(value) || "\u672a\u767b\u5f55".equals(value)) {
            return "\u6211";
        }
        return value.substring(0, 1).toUpperCase();
    }

    private View updateCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 18);
        body.addView(KinUi.text(activity, "\u5e94\u7528\u66f4\u65b0", 19, true));
        TextView version = KinUi.muted(activity, "\u5f53\u524d\u7248\u672c\uff1a" + activity.getCurrentVersionName(), 14);
        KinUi.margins(version, activity, 0, 8, 0, 0);
        body.addView(version);
        View checkButton = actionButton(activity, "\u624b\u52a8\u68c0\u67e5\u66f4\u65b0", v -> activity.checkForUpdatesManually());
        body.addView(checkButton);
        card.addView(body);
        return card;
    }

    private View actionsCard(MainActivity activity, SessionManager sessionManager, SessionUser user) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 18);

        body.addView(actionButton(activity, sessionManager.isLoggedIn() ? "\u5207\u6362\u8d26\u53f7" : "\u767b\u5f55 / \u6ce8\u518c",
                v -> startActivity(new Intent(activity, AuthActivity.class))));
        body.addView(actionButton(activity, "AI \u6a21\u578b\u914d\u7f6e",
                v -> startActivity(new Intent(activity, AiSettingsActivity.class))));
        body.addView(actionButton(activity, "\u6211\u7684\u4e3b\u9875",
                v -> startActivity(new Intent(activity, UserProfileActivity.class))));
        body.addView(actionButton(activity, "\u8349\u7a3f\u7bb1",
                v -> startActivity(new Intent(activity, DraftsActivity.class))));
        body.addView(actionButton(activity, "\u6d88\u606f\u4e2d\u5fc3",
                v -> startActivity(new Intent(activity, MessagesActivity.class))));
        body.addView(actionButton(activity, "每日签到",
                v -> startActivity(new Intent(activity, CheckInActivity.class))));
        body.addView(actionButton(activity, "\u7559\u8a00\u677f",
                v -> startActivity(new Intent(activity, MessageBoardActivity.class))));
        body.addView(actionButton(activity, "\u6211\u7684\u4e3e\u62a5",
                v -> startActivity(new Intent(activity, MyReportsActivity.class))));
        body.addView(featureButton(activity, "账号安全", "user.security_center"));
        body.addView(featureButton(activity, "成长/徽章", "user.level_xp"));
        body.addView(featureButton(activity, "隐私与黑名单", "user.privacy_settings"));
        body.addView(featureButton(activity, "通知偏好", "interaction.notification_preferences"));
        body.addView(featureButton(activity, "战队空间", "team.space"));
        body.addView(featureButton(activity, "作者中心", "analytics.creator_center"));
        if (user != null && user.isAdmin()) {
            body.addView(actionButton(activity, "\u7ba1\u7406\u5458\u4e2d\u5fc3",
                    v -> startActivity(new Intent(activity, AdminCenterActivity.class))));
            body.addView(actionButton(activity, "平台/接口诊断",
                    v -> startActivity(new Intent(activity, FutureFeatureCenterActivity.class))));
        }
        if (sessionManager.isLoggedIn()) {
            body.addView(actionButton(activity, "\u9000\u51fa\u767b\u5f55", v -> {
                activity.getRepository().logout();
                activity.refreshToolbarSubtitle();
                startActivity(new Intent(activity, AuthActivity.class));
            }));
        }
        card.addView(body);
        return card;
    }

    private View featureButton(MainActivity activity, String label, String featureKey) {
        return actionButton(activity, label, v -> {
            Intent intent = new Intent(activity, FutureFeatureDetailActivity.class);
            intent.putExtra(FutureFeatureDetailActivity.EXTRA_FEATURE_KEY, featureKey);
            startActivity(intent);
        });
    }

    private View actionButton(MainActivity activity, String label, View.OnClickListener listener) {
        MaterialButton button = KinUi.outlinedButton(activity, label);
        if (contentLayout.getChildCount() > 0) {
            KinUi.margins(button, activity, 0, 12, 0, 0);
        }
        button.setOnClickListener(listener);
        return button;
    }
}
