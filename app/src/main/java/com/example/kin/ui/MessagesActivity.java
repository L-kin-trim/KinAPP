package com.example.kin.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.R;
import com.example.kin.data.KinRepository;
import com.example.kin.model.MessageUnreadSummaryModel;
import com.example.kin.model.PageResult;
import com.example.kin.model.StationMessageModel;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MessagesActivity extends AppCompatActivity {
    private KinRepository repository;
    private LinearLayout contentLayout;
    private LinearLayout listLayout;
    private ProgressBar progressBar;
    private TextView statusView;
    private TextView summaryView;
    private boolean inboxMode = true;
    private String messageType = "";
    private String readFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KinRepository(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("消息中心");
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        toolbar.setNavigationOnClickListener(v -> finish());
        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(this);
        contentLayout = KinUi.vertical(this);
        contentLayout.setPadding(KinUi.dp(this, 18), KinUi.dp(this, 12), KinUi.dp(this, 18), KinUi.dp(this, 24));
        progressBar = new ProgressBar(this);
        statusView = KinUi.muted(this, "", 13);
        statusView.setVisibility(View.GONE);
        contentLayout.addView(progressBar);
        contentLayout.addView(statusView);
        contentLayout.addView(buildHeader());
        listLayout = KinUi.vertical(this);
        contentLayout.addView(listLayout);
        scrollView.addView(contentLayout);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        loadSummary();
        loadMessages();
    }

    private View buildHeader() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        summaryView = KinUi.muted(this, "正在读取未读统计…", 14);
        body.addView(summaryView);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        Chip inboxChip = KinUi.chip(this, "收件箱");
        Chip sentChip = KinUi.chip(this, "发件箱");
        inboxChip.setCheckable(true);
        sentChip.setCheckable(true);
        inboxChip.setChecked(true);
        inboxChip.setOnClickListener(v -> {
            inboxMode = true;
            loadMessages();
        });
        sentChip.setOnClickListener(v -> {
            inboxMode = false;
            loadMessages();
        });
        tabs.addView(inboxChip);
        tabs.addView(sentChip);
        KinUi.margins(sentChip, this, 10, 0, 0, 0);
        KinUi.margins(tabs, this, 0, 12, 0, 0);
        body.addView(tabs);

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        filters.addView(filterChip("全部类型", ""));
        filters.addView(filterChip("互动提醒", "INTERACTION_REMINDER"));
        filters.addView(filterChip("审核结果", "REVIEW_RESULT"));
        filters.addView(filterChip("私信", "DIRECT"));
        KinUi.margins(filters, this, 0, 12, 0, 0);
        body.addView(filters);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton composeButton = KinUi.filledButton(this, "发送消息");
        MaterialButton markReadButton = KinUi.outlinedButton(this, "全部已读");
        composeButton.setOnClickListener(v -> showComposeDialog());
        markReadButton.setOnClickListener(v -> repository.markAllMessagesRead(messageType, new ApiCallback<>() {
            @Override
            public void onSuccess(org.json.JSONObject data) {
                loadSummary();
                loadMessages();
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "全部已读失败：" + exception.getMessage());
            }
        }));
        actions.addView(composeButton);
        actions.addView(markReadButton);
        KinUi.margins(markReadButton, this, 10, 0, 0, 0);
        KinUi.margins(actions, this, 0, 14, 0, 0);
        body.addView(actions);

        card.addView(body);
        return card;
    }

    private Chip filterChip(String label, String type) {
        Chip chip = KinUi.chip(this, label);
        chip.setCheckable(true);
        chip.setOnClickListener(v -> {
            messageType = type;
            readFilter = inboxMode ? readFilter : "ALL";
            loadMessages();
        });
        return chip;
    }

    private void loadSummary() {
        repository.getUnreadSummary(new ApiCallback<>() {
            @Override
            public void onSuccess(MessageUnreadSummaryModel data) {
                summaryView.setText("未读 " + data.unreadCount
                        + " · 系统 " + data.systemNoticeUnreadCount
                        + " · 审核 " + data.reviewResultUnreadCount
                        + " · 互动 " + data.interactionReminderUnreadCount
                        + " · 私信 " + data.directUnreadCount);
            }

            @Override
            public void onError(ApiException exception) {
                summaryView.setText(exception.isFeatureUnavailable() ? "未读统计接口未开放。" : "未读统计加载失败。");
            }
        });
    }

    private void loadMessages() {
        setLoading(true, "正在同步消息…");
        listLayout.removeAllViews();
        ApiCallback<PageResult<StationMessageModel>> callback = new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<StationMessageModel> data) {
                for (StationMessageModel item : data.items) {
                    listLayout.addView(messageCard(item));
                }
                setLoading(false, data.items.isEmpty() ? "暂无消息。" : "");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, "消息加载失败：" + exception.getMessage());
            }
        };
        if (inboxMode) {
            repository.getInbox(0, 30, readFilter, messageType, callback);
        } else {
            repository.getSent(0, 30, messageType, callback);
        }
    }

    private View messageCard(StationMessageModel item) {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 16);
        String title = inboxMode ? ("来自 " + item.senderUsername) : ("发送给 " + item.recipientUsername);
        body.addView(KinUi.text(this, title, 17, true));
        TextView meta = KinUi.muted(this, displayType(item.messageType) + " · " + item.sentAt + (item.read ? " · 已读" : " · 未读"), 12);
        KinUi.margins(meta, this, 0, 6, 0, 0);
        body.addView(meta);
        TextView content = KinUi.muted(this, item.content, 15);
        KinUi.margins(content, this, 0, 10, 0, 0);
        body.addView(content);
        if (inboxMode && !item.read) {
            MaterialButton readButton = KinUi.outlinedButton(this, "标记已读");
            readButton.setOnClickListener(v -> repository.markMessageRead(item.id, new ApiCallback<>() {
                @Override
                public void onSuccess(org.json.JSONObject data) {
                    loadSummary();
                    loadMessages();
                }

                @Override
                public void onError(ApiException exception) {
                    setLoading(false, "标记已读失败：" + exception.getMessage());
                }
            }));
            KinUi.margins(readButton, this, 0, 14, 0, 0);
            body.addView(readButton);
        }
        card.addView(body);
        return card;
    }

    private void showComposeDialog() {
        if (!repository.getSessionManager().isLoggedIn()) {
            setLoading(false, "请先登录。");
            return;
        }
        LinearLayout root = KinUi.vertical(this);
        TextInputLayout recipientLayout = KinUi.inputLayout(this, "收件人用户名", false);
        TextInputLayout typeLayout = KinUi.inputLayout(this, "消息类型（默认 DIRECT）", false);
        TextInputLayout contentField = KinUi.inputLayout(this, "消息内容", true);
        TextInputEditText recipientEdit = KinUi.edit(recipientLayout);
        TextInputEditText typeEdit = KinUi.edit(typeLayout);
        TextInputEditText contentEdit = KinUi.edit(contentField);
        typeEdit.setText("DIRECT");
        root.addView(recipientLayout);
        root.addView(typeLayout);
        root.addView(contentField);
        KinUi.margins(typeLayout, this, 0, 12, 0, 0);
        KinUi.margins(contentField, this, 0, 12, 0, 0);

        new AlertDialog.Builder(this)
                .setTitle("发送站内信")
                .setView(root)
                .setPositiveButton("发送", (dialog, which) -> repository.sendMessage(
                        text(recipientEdit),
                        text(contentEdit),
                        text(typeEdit),
                        new ApiCallback<>() {
                            @Override
                            public void onSuccess(StationMessageModel data) {
                                loadMessages();
                            }

                            @Override
                            public void onError(ApiException exception) {
                                setLoading(false, "发送失败：" + exception.getMessage());
                            }
                        }))
                .setNegativeButton("取消", null)
                .show();
    }

    private String displayType(String messageType) {
        if ("INTERACTION_REMINDER".equals(messageType)) {
            return "互动提醒";
        }
        if ("REVIEW_RESULT".equals(messageType)) {
            return "审核结果";
        }
        if ("SYSTEM_NOTICE".equals(messageType)) {
            return "系统通知";
        }
        return "私信";
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusView.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        statusView.setText(message);
    }

    private String text(TextInputEditText editText) {
        return String.valueOf(editText.getText()).trim();
    }
}
