package com.example.kin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
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

public class LaunchActivity extends AppCompatActivity {
    private boolean redirected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));
        ProgressBar progressBar = new ProgressBar(this);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        progressParams.gravity = Gravity.CENTER;
        root.addView(progressBar, progressParams);

        TextView status = KinUi.muted(this, "\u6b63\u5728\u68c0\u67e5\u767b\u5f55\u72b6\u6001...", 14);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        statusParams.topMargin = KinUi.dp(this, 56);
        root.addView(status, statusParams);
        setContentView(root);

        KinRepository repository = new KinRepository(this);
        if (repository.getSessionManager().isLoggedIn()) {
            open(MainActivity.class);
            return;
        }
        repository.tryAutoLogin(new ApiCallback<>() {
            @Override
            public void onSuccess(SessionUser data) {
                open(MainActivity.class);
            }

            @Override
            public void onError(ApiException exception) {
                open(AuthActivity.class);
            }
        });
    }

    private void open(Class<?> target) {
        if (redirected) {
            return;
        }
        redirected = true;
        startActivity(new Intent(this, target));
        finish();
    }
}
