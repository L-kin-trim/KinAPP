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
        activity.setTopBar("\u6211", "");
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
        body.addView(KinUi.text(activity, sessionManager.isLoggedIn() ? user.username : "\u672a\u767b\u5f55", 24, true));
        TextView subtitle = KinUi.muted(activity,
                sessionManager.isLoggedIn() ? ("\u89d2\u8272: " + user.role + " | \u7528\u6237ID " + user.id) : "\u767b\u5f55\u540e\u53ef\u53d1\u5e16\u3001\u8bc4\u8bba\u3001\u6536\u85cf\u4e0e\u79c1\u4fe1\u3002",
                14);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);
        TextView endpoint = KinUi.muted(activity, "\u670d\u52a1\u5730\u5740: " + sessionManager.getBaseUrl(), 13);
        KinUi.margins(endpoint, activity, 0, 12, 0, 0);
        body.addView(endpoint);
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
        body.addView(actionButton(activity, "\u7559\u8a00\u677f",
                v -> startActivity(new Intent(activity, MessageBoardActivity.class))));
        body.addView(actionButton(activity, "\u6211\u7684\u4e3e\u62a5",
                v -> startActivity(new Intent(activity, MyReportsActivity.class))));
        if (user.isAdmin()) {
            body.addView(actionButton(activity, "\u7ba1\u7406\u5458\u4e2d\u5fc3",
                    v -> startActivity(new Intent(activity, AdminCenterActivity.class))));
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

    private View actionButton(MainActivity activity, String label, View.OnClickListener listener) {
        MaterialButton button = KinUi.outlinedButton(activity, label);
        if (contentLayout.getChildCount() > 0) {
            KinUi.margins(button, activity, 0, 12, 0, 0);
        }
        button.setOnClickListener(listener);
        return button;
    }
}
