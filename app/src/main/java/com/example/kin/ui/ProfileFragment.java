package com.example.kin.ui;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.kin.MainActivity;
import com.example.kin.data.SessionManager;
import com.example.kin.model.SessionUser;
import com.example.kin.ui.admin.AdminCenterActivity;
import com.example.kin.ui.common.BasePageFragment;
import com.example.kin.ui.common.KinUi;
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
        activity.setTopBar("我的", "");
        contentLayout.removeAllViews();
        contentLayout.addView(progressBar);
        contentLayout.addView(statusView);

        SessionManager sessionManager = activity.getRepository().getSessionManager();
        SessionUser user = sessionManager.getUser();

        contentLayout.addView(profileCard(activity, sessionManager, user));
        contentLayout.addView(actionsCard(activity, sessionManager, user));
        setLoading(false, "");
    }

    private View profileCard(MainActivity activity, SessionManager sessionManager, SessionUser user) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 18);
        body.addView(KinUi.text(activity, sessionManager.isLoggedIn() ? user.username : "未登录", 24, true));
        TextView subtitle = KinUi.muted(activity,
                sessionManager.isLoggedIn() ? ("角色：" + user.role + " · 用户ID " + user.id) : "登录后可发布、评论、收藏、发送消息。",
                14);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);
        TextView endpoint = KinUi.muted(activity, "服务地址：" + sessionManager.getBaseUrl(), 13);
        KinUi.margins(endpoint, activity, 0, 12, 0, 0);
        body.addView(endpoint);
        card.addView(body);
        return card;
    }

    private View actionsCard(MainActivity activity, SessionManager sessionManager, SessionUser user) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 18);

        body.addView(actionButton(activity, sessionManager.isLoggedIn() ? "切换账号" : "登录 / 注册",
                v -> startActivity(new Intent(activity, AuthActivity.class))));
        body.addView(actionButton(activity, "我的主页",
                v -> startActivity(new Intent(activity, UserProfileActivity.class))));
        body.addView(actionButton(activity, "草稿箱",
                v -> startActivity(new Intent(activity, DraftsActivity.class))));
        body.addView(actionButton(activity, "消息中心",
                v -> startActivity(new Intent(activity, MessagesActivity.class))));
        body.addView(actionButton(activity, "留言板",
                v -> startActivity(new Intent(activity, MessageBoardActivity.class))));
        body.addView(actionButton(activity, "我的举报",
                v -> startActivity(new Intent(activity, MyReportsActivity.class))));
        if (user.isAdmin()) {
            body.addView(actionButton(activity, "管理员中心",
                    v -> startActivity(new Intent(activity, AdminCenterActivity.class))));
        }
        if (sessionManager.isLoggedIn()) {
            body.addView(actionButton(activity, "退出登录", v -> {
                sessionManager.clearSession();
                activity.refreshToolbarSubtitle();
                startActivity(new Intent(activity, AuthActivity.class));
            }));
        }
        card.addView(body);
        return card;
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
