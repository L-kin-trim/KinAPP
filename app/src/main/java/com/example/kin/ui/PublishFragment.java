package com.example.kin.ui;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.kin.MainActivity;
import com.example.kin.ui.common.BasePageFragment;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class PublishFragment extends BasePageFragment {

    @Override
    protected void onPageReady() {
        MainActivity activity = (MainActivity) requireActivity();
        activity.setTopBar("发布", "");
        contentLayout.addView(header(activity));
        setLoading(false, "");
    }

    private View header(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 20);
        body.addView(KinUi.text(activity, "发布帖子", 22, true));
        TextView subtitle = KinUi.muted(activity, "支持道具分享帖、战术分享帖、日常闲聊帖。编辑器已接入本地缓存和服务端草稿。", 14);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);
        MaterialButton openButton = KinUi.filledButton(activity, "打开发布器");
        openButton.setOnClickListener(v -> startActivity(new Intent(activity, PublishEditorActivity.class)));
        KinUi.margins(openButton, activity, 0, 16, 0, 0);
        body.addView(openButton);
        card.addView(body);
        return card;
    }
}
