package com.example.kin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.MainActivity;
import com.example.kin.R;
import com.example.kin.data.KinRepository;
import com.example.kin.model.SessionUser;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AuthActivity extends AppCompatActivity {
    private KinRepository repository;
    private boolean registerMode;
    private ProgressBar progressBar;
    private TextView statusView;
    private TextInputEditText usernameEdit;
    private TextInputEditText passwordEdit;
    private TextInputEditText emailEdit;
    private TextInputLayout emailLayout;
    private MaterialButton submitButton;
    private MaterialButton switchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KinRepository(this);
        if (repository.getSessionManager().isLoggedIn()) {
            openMain();
            return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));

        MaterialCardView card = KinUi.card(this);
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = KinUi.dp(this, 22);
        cardParams.setMargins(margin, margin, margin, margin);
        cardParams.gravity = Gravity.CENTER;
        card.setLayoutParams(cardParams);

        LinearLayout body = KinUi.sectionContainer(this, 22);
        body.addView(KinUi.text(this, "登录 Kin", 24, true));
        TextView intro = KinUi.muted(this, "首次进入需要先登录，登录状态会保存在本地数据库中。", 14);
        KinUi.margins(intro, this, 0, 8, 0, 0);
        body.addView(intro);

        TextInputLayout userLayout = KinUi.inputLayout(this, "用户名", false);
        TextInputLayout passwordLayout = KinUi.inputLayout(this, "密码", false);
        emailLayout = KinUi.inputLayout(this, "邮箱", false);
        usernameEdit = KinUi.edit(userLayout);
        passwordEdit = KinUi.edit(passwordLayout);
        emailEdit = KinUi.edit(emailLayout);
        passwordEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        KinUi.margins(userLayout, this, 0, 16, 0, 0);
        KinUi.margins(passwordLayout, this, 0, 12, 0, 0);
        KinUi.margins(emailLayout, this, 0, 12, 0, 0);
        body.addView(userLayout);
        body.addView(passwordLayout);
        body.addView(emailLayout);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        KinUi.margins(progressBar, this, 0, 16, 0, 0);
        body.addView(progressBar);

        statusView = KinUi.muted(this, "", 13);
        statusView.setVisibility(View.GONE);
        KinUi.margins(statusView, this, 0, 10, 0, 0);
        body.addView(statusView);

        submitButton = KinUi.filledButton(this, "登录");
        switchButton = KinUi.outlinedButton(this, "没有账号？去注册");
        submitButton.setOnClickListener(v -> submit());
        switchButton.setOnClickListener(v -> {
            registerMode = !registerMode;
            renderMode();
        });
        KinUi.margins(submitButton, this, 0, 16, 0, 0);
        KinUi.margins(switchButton, this, 0, 12, 0, 0);
        body.addView(submitButton);
        body.addView(switchButton);
        card.addView(body);
        root.addView(card);
        setContentView(root);
        renderMode();
    }

    @Override
    public void onBackPressed() {
        if (!repository.getSessionManager().isLoggedIn()) {
            finishAffinity();
            return;
        }
        super.onBackPressed();
    }

    private void renderMode() {
        emailLayout.setVisibility(registerMode ? View.VISIBLE : View.GONE);
        submitButton.setText(registerMode ? "注册" : "登录");
        switchButton.setText(registerMode ? "已有账号？去登录" : "没有账号？去注册");
        status("");
    }

    private void submit() {
        progress(true);
        if (registerMode) {
            repository.register(text(usernameEdit), text(passwordEdit), text(emailEdit), new ApiCallback<>() {
                @Override
                public void onSuccess(SessionUser data) {
                    progress(false);
                    registerMode = false;
                    renderMode();
                    status("注册成功，请直接登录。");
                }

                @Override
                public void onError(ApiException exception) {
                    progress(false);
                    status("注册失败：" + exception.getMessage());
                }
            });
            return;
        }
        repository.login(text(usernameEdit), text(passwordEdit), new ApiCallback<>() {
            @Override
            public void onSuccess(SessionUser data) {
                progress(false);
                openMain();
            }

            @Override
            public void onError(ApiException exception) {
                progress(false);
                status("登录失败：" + exception.getMessage());
            }
        });
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void progress(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!loading);
        switchButton.setEnabled(!loading);
    }

    private void status(String message) {
        statusView.setText(message);
        statusView.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String text(TextInputEditText editText) {
        return String.valueOf(editText.getText()).trim();
    }
}
