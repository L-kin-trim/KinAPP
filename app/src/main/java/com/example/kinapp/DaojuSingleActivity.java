package com.example.kinapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kinapp.utils.DaojuInformationDAO;
import com.example.kinapp.utils.ZoomableImageView;

import java.io.File;

public class DaojuSingleActivity extends AppCompatActivity {
    private TextView tvThrowingMethod;
    private ImageView ivStanceImage, ivAimPointImage, ivLandingPointImage;
    private DaojuInformationDAO daojuInformationDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_daoju_single);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化视图
        initViews();

        // 初始化数据库访问对象
        daojuInformationDAO = new DaojuInformationDAO(this);

        // 获取传递的数据
        int infoId = getIntent().getIntExtra("info_id", -1);
        if (infoId != -1) {
            loadDaojuDetails(infoId);
        }
    }

    private void initViews() {
        tvThrowingMethod = findViewById(R.id.tv_throwing_method);
        ivStanceImage = findViewById(R.id.iv_stance_image);
        ivAimPointImage = findViewById(R.id.iv_aim_point_image);
        ivLandingPointImage = findViewById(R.id.iv_landing_point_image);

        // 为图片设置点击事件
        ivStanceImage.setOnClickListener(v -> showImagePopup((ImageView) v));
        ivAimPointImage.setOnClickListener(v -> showImagePopup((ImageView) v));
        ivLandingPointImage.setOnClickListener(v -> showImagePopup((ImageView) v));
    }

    private void loadDaojuDetails(int infoId) {
        DaojuInformationDAO.DaojuInformation info = daojuInformationDAO.getDaojuInformationById(infoId);
        if (info != null) {
            // 显示投掷方式
            tvThrowingMethod.setText(info.getThrowingMethod());

            // 显示站位图片
            if (info.getStanceImagePath() != null && !info.getStanceImagePath().isEmpty()) {
                File imgFile = new File(info.getStanceImagePath());
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    ivStanceImage.setImageBitmap(bitmap);
                }
            }

            // 显示瞄点图片
            if (info.getAimPointImagePath() != null && !info.getAimPointImagePath().isEmpty()) {
                File imgFile = new File(info.getAimPointImagePath());
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    ivAimPointImage.setImageBitmap(bitmap);
                }
            }

            // 显示落点图片
            if (info.getLandingPointImagePath() != null && !info.getLandingPointImagePath().isEmpty()) {
                File imgFile = new File(info.getLandingPointImagePath());
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    ivLandingPointImage.setImageBitmap(bitmap);
                }
            }
        }
    }

    private void showImagePopup(ImageView clickedImageView) {
        // 创建弹窗视图
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_image, null);

        // 创建PopupWindow
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = ViewGroup.LayoutParams.MATCH_PARENT;
        boolean focusable = true;
        PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // 设置弹窗外的背景半透明
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 0.7f;
        getWindow().setAttributes(params);

        // 监听弹窗关闭事件以恢复背景透明度
        popupWindow.setOnDismissListener(() -> {
            WindowManager.LayoutParams params1 = getWindow().getAttributes();
            params1.alpha = 1.0f;
            getWindow().setAttributes(params1);
        });

        // 获取弹窗中的ZoomableImageView并设置图片
        ZoomableImageView popupImageView = popupView.findViewById(R.id.iv_popup_image);
        popupImageView.setImageDrawable(clickedImageView.getDrawable());

        // 设置关闭按钮
        ImageButton btnClose = popupView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> popupWindow.dismiss());

        // 显示弹窗
        popupWindow.showAtLocation(findViewById(R.id.main), Gravity.CENTER, 0, 0);
    }
}
