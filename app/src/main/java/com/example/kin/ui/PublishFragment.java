package com.example.kin.ui;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.kin.MainActivity;
import com.example.kin.model.DraftModel;
import com.example.kin.model.PageResult;
import com.example.kin.ui.common.BasePageFragment;
import com.example.kin.ui.common.KinUi;
import com.example.kin.ui.future.FutureFeatureDetailActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class PublishFragment extends BasePageFragment {
    private TextView draftSummary;

    @Override
    protected void onPageReady() {
        MainActivity activity = (MainActivity) requireActivity();
        activity.setTopBar("发布", "");
        contentLayout.addView(header(activity));
        contentLayout.addView(upgradeCard(activity));
        contentLayout.addView(draftCard(activity));
        setLoading(false, "");
    }

    private View upgradeCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 18);
        body.addView(KinUi.text(activity, "创作工具箱", 19, true));
        TextView subtitle = KinUi.muted(activity, "Markdown/富文本、模板、质量检测、协作草稿与定时发布按发帖流程提供。", 14);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);
        LinearLayout row = KinUi.buttonRow(activity,
                action(activity, "富文本", "content.markdown_editor"),
                action(activity, "模板", "content.post_templates"),
                action(activity, "质检", "content.quality_check"));
        KinUi.margins(row, activity, 0, 12, 0, 0);
        body.addView(row);
        LinearLayout row2 = KinUi.buttonRow(activity,
                action(activity, "协作草稿", "content.collaborative_draft"),
                action(activity, "定时发布", "content.scheduled_publish"),
                action(activity, "引用卡片", "content.quote_cards"));
        KinUi.margins(row2, activity, 0, 10, 0, 0);
        body.addView(row2);
        card.addView(body);
        return card;
    }

    private MaterialButton action(MainActivity activity, String label, String featureKey) {
        MaterialButton button = KinUi.outlinedButton(activity, label);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(activity, FutureFeatureDetailActivity.class);
            intent.putExtra(FutureFeatureDetailActivity.EXTRA_FEATURE_KEY, featureKey);
            startActivity(intent);
        });
        return button;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDraftSummary();
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

    private View draftCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 20);
        body.addView(KinUi.text(activity, "草稿箱", 20, true));
        draftSummary = KinUi.muted(activity, "正在读取草稿…", 14);
        KinUi.margins(draftSummary, activity, 0, 8, 0, 0);
        body.addView(draftSummary);
        MaterialButton editButton = KinUi.outlinedButton(activity, "继续编辑");
        editButton.setOnClickListener(v -> startActivity(new Intent(activity, PublishEditorActivity.class)));
        KinUi.margins(editButton, activity, 0, 16, 0, 0);
        body.addView(editButton);
        card.addView(body);
        return card;
    }

    private void refreshDraftSummary() {
        MainActivity activity = (MainActivity) requireActivity();
        DraftModel localDraft = activity.getRepository().getLocalDraftStore().get("publish_forum_post");
        if (localDraft != null && localDraft.payloadJson != null && !localDraft.payloadJson.isEmpty()) {
            draftSummary.setText("本地草稿已保存，可直接继续编辑。");
            return;
        }
        activity.getRepository().getDrafts("FORUM_POST", 0, 1, new com.example.kin.net.ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<DraftModel> data) {
                if (data.items.isEmpty()) {
                    draftSummary.setText("暂无服务端草稿。");
                    return;
                }
                DraftModel draft = data.items.get(0);
                draftSummary.setText("最近服务端草稿：" + draft.title);
            }

            @Override
            public void onError(com.example.kin.net.ApiException exception) {
                draftSummary.setText(exception.isFeatureUnavailable() ? "后端草稿接口未开放。" : "草稿读取失败：" + exception.getMessage());
            }
        });
    }
}
