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
import com.example.kin.model.MessageBoardEntryModel;
import com.example.kin.model.PageResult;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MessageBoardActivity extends AppCompatActivity {
    private KinRepository repository;
    private LinearLayout listLayout;
    private ProgressBar progressBar;
    private TextView statusView;
    private boolean mineMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KinRepository(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));
        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("留言板");
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
        content.addView(header());
        listLayout = KinUi.vertical(this);
        content.addView(listLayout);
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        loadEntries();
    }

    private View header() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        Chip allChip = KinUi.chip(this, "公开留言");
        Chip myChip = KinUi.chip(this, "我的留言");
        allChip.setCheckable(true);
        myChip.setCheckable(true);
        allChip.setChecked(true);
        allChip.setOnClickListener(v -> {
            mineMode = false;
            loadEntries();
        });
        myChip.setOnClickListener(v -> {
            mineMode = true;
            loadEntries();
        });
        chips.addView(allChip);
        chips.addView(myChip);
        KinUi.margins(myChip, this, 10, 0, 0, 0);
        body.addView(chips);
        MaterialButton createButton = KinUi.filledButton(this, "发布留言");
        createButton.setOnClickListener(v -> showCreateDialog());
        KinUi.margins(createButton, this, 14, 16, 0, 0);
        body.addView(createButton);
        card.addView(body);
        return card;
    }

    private void showCreateDialog() {
        TextInputLayout layout = KinUi.inputLayout(this, "留言内容", true);
        TextInputEditText editText = KinUi.edit(layout);
        new AlertDialog.Builder(this)
                .setTitle("发布留言")
                .setView(layout)
                .setPositiveButton("提交", (dialog, which) -> repository.createMessageBoardEntry(text(editText), new ApiCallback<>() {
                    @Override
                    public void onSuccess(MessageBoardEntryModel data) {
                        loadEntries();
                    }

                    @Override
                    public void onError(ApiException exception) {
                        setLoading(false, "留言发布失败：" + exception.getMessage());
                    }
                }))
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadEntries() {
        setLoading(true, "正在读取留言…");
        listLayout.removeAllViews();
        ApiCallback<PageResult<MessageBoardEntryModel>> callback = new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<MessageBoardEntryModel> data) {
                for (MessageBoardEntryModel entry : data.items) {
                    listLayout.addView(entryCard(entry));
                }
                setLoading(false, data.items.isEmpty() ? "暂无留言。" : "");
            }

            @Override
            public void onError(ApiException exception) {
                setLoading(false, exception.isFeatureUnavailable() ? "后端留言板接口未开放。" : "留言加载失败：" + exception.getMessage());
            }
        };
        if (mineMode) {
            repository.getMyMessageBoardEntries(0, 20, callback);
        } else {
            repository.getMessageBoardEntries("", 0, 20, callback);
        }
    }

    private View entryCard(MessageBoardEntryModel entry) {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 16);
        body.addView(KinUi.text(this, entry.authorUsername, 16, true));
        TextView meta = KinUi.muted(this, entry.status + " · " + entry.createdAt, 12);
        KinUi.margins(meta, this, 0, 8, 0, 0);
        body.addView(meta);
        TextView content = KinUi.muted(this, entry.content, 14);
        KinUi.margins(content, this, 0, 10, 0, 0);
        body.addView(content);
        if (mineMode) {
            MaterialButton revokeButton = KinUi.outlinedButton(this, "撤回留言");
            revokeButton.setOnClickListener(v -> repository.revokeMessageBoardEntry(entry.id, "用户主动撤回", new ApiCallback<>() {
                @Override
                public void onSuccess(MessageBoardEntryModel data) {
                    loadEntries();
                }

                @Override
                public void onError(ApiException exception) {
                    setLoading(false, "撤回失败：" + exception.getMessage());
                }
            }));
            KinUi.margins(revokeButton, this, 0, 14, 0, 0);
            body.addView(revokeButton);
        }
        card.addView(body);
        return card;
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
