package com.example.kin.ui;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.kin.MainActivity;
import com.example.kin.ui.common.BasePageFragment;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class AiRecommendFragment extends BasePageFragment {
    @Override
    protected void onPageReady() {
        MainActivity activity = (MainActivity) requireActivity();
        activity.setTopBar("AI 推荐", "");
        contentLayout.addView(buildPlaceholder(activity));
        setLoading(false, "");
    }

    private View buildPlaceholder(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 20);
        body.addView(KinUi.text(activity, "AI 推荐", 22, true));
        TextView subtitle = KinUi.muted(activity, "该模块按你的要求暂不制作，只保留入口与待完成提示。", 14);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);
        TextView desc = KinUi.muted(activity, "后续可以在这里接个性化推荐、偏好分析和训练路线。", 14);
        KinUi.margins(desc, activity, 0, 12, 0, 0);
        body.addView(desc);
        MaterialButton messagesButton = KinUi.outlinedButton(activity, "去消息中心");
        messagesButton.setOnClickListener(v -> activity.openMessages());
        KinUi.margins(messagesButton, activity, 0, 16, 0, 0);
        body.addView(messagesButton);
        card.addView(body);
        return card;
    }
}
