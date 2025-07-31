package com.example.kinapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.kinapp.utils.DaojuInformationDAO;
import com.example.kinapp.utils.MapDAO;

import java.io.File;
import java.io.FileOutputStream;

public class DaojuInformationActivity extends AppCompatActivity {
    private EditText etThrowingMethod, etToolName, etPosition;
    private TextView tvMapInfo;
    private Button btnSave, btnSelectStanceImage, btnSelectAimPointImage, btnSelectLandingPointImage;
    private RadioGroup rgDaojuType;
    private RadioButton rbSmoke, rbMolotov, rbGrenade, rbFlashbang;
    private ImageView ivStanceImagePreview, ivAimPointImagePreview, ivLandingPointImagePreview;
    private DaojuInformationDAO daojuInformationDAO;
    private MapDAO mapDAO;

    // 图片路径变量
    private String stanceImagePath = "";
    private String aimPointImagePath = "";
    private String landingPointImagePath = "";

    // 地图相关信息
    private int selectedMapId = 1; // 默认地图ID
    private String selectedMapName = "默认地图"; // 默认地图名称

    // 道具类型常量
    private static final int TYPE_SMOKE = 1;      // 烟雾弹
    private static final int TYPE_MOLOTOV = 2;    // 燃烧瓶
    private static final int TYPE_GRENADE = 3;    // 手雷
    private static final int TYPE_FLASHBANG = 4;  // 闪光弹

    // 请求码
    private static final int REQUEST_STANCE_IMAGE = 1;
    private static final int REQUEST_AIM_POINT_IMAGE = 2;
    private static final int REQUEST_LANDING_POINT_IMAGE = 3;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_daoju_information);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 检查并请求存储权限
        checkAndRequestPermissions();

        // 获取传递的地图信息
        Intent intent = getIntent();
        selectedMapId = intent.getIntExtra("map_id", 1);
        selectedMapName = intent.getStringExtra("map_name");
        if (selectedMapName == null) {
            selectedMapName = "默认地图";
        }

        // 初始化数据库访问对象
        daojuInformationDAO = new DaojuInformationDAO(this);
        mapDAO = new MapDAO(this);

        // 初始化视图
        initViews();

        // 设置提示信息
        tvMapInfo.setText("您正在向 " + selectedMapName + " 添加道具");

        // 设置保存按钮点击事件
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDaojuInformation();
            }
        });
    }

    // 检查并请求存储权限
    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void initViews() {
        tvMapInfo = findViewById(R.id.tv_map_info);
        etThrowingMethod = findViewById(R.id.et_throwing_method);
        etToolName = findViewById(R.id.et_tool_name);
        etPosition = findViewById(R.id.et_position);
        btnSave = findViewById(R.id.btn_save);

        // 初始化图片选择按钮
        btnSelectStanceImage = findViewById(R.id.btn_select_stance_image);
        btnSelectAimPointImage = findViewById(R.id.btn_select_aim_point_image);
        btnSelectLandingPointImage = findViewById(R.id.btn_select_landing_point_image);

        // 初始化图片预览ImageView
        ivStanceImagePreview = findViewById(R.id.iv_stance_image_preview);
        ivAimPointImagePreview = findViewById(R.id.iv_aim_point_image_preview);
        ivLandingPointImagePreview = findViewById(R.id.iv_landing_point_image_preview);

        // 初始化单选框组和选项
        rgDaojuType = findViewById(R.id.rg_daoju_type);
        rbSmoke = findViewById(R.id.rb_smoke);
        rbMolotov = findViewById(R.id.rb_molotov);
        rbGrenade = findViewById(R.id.rb_grenade);
        rbFlashbang = findViewById(R.id.rb_flashbang);

        // 设置图片选择按钮点击事件
        btnSelectStanceImage.setOnClickListener(v -> selectImage(REQUEST_STANCE_IMAGE));
        btnSelectAimPointImage.setOnClickListener(v -> selectImage(REQUEST_AIM_POINT_IMAGE));
        btnSelectLandingPointImage.setOnClickListener(v -> selectImage(REQUEST_LANDING_POINT_IMAGE));
    }

    // 选择图片的方法
    private void selectImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    String imagePath = saveImageToStorage(bitmap, requestCode);

                    switch (requestCode) {
                        case REQUEST_STANCE_IMAGE:
                            stanceImagePath = imagePath;
                            ivStanceImagePreview.setImageBitmap(bitmap);
                            break;
                        case REQUEST_AIM_POINT_IMAGE:
                            aimPointImagePath = imagePath;
                            ivAimPointImagePreview.setImageBitmap(bitmap);
                            break;
                        case REQUEST_LANDING_POINT_IMAGE:
                            landingPointImagePath = imagePath;
                            ivLandingPointImagePreview.setImageBitmap(bitmap);
                            break;
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "图片选择失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // 保存图片到存储并返回路径
    private String saveImageToStorage(Bitmap bitmap, int requestCode) {
        // 创建专属文件夹
        File imageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "kinapp_images");
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }

        // 根据请求码生成文件名
        String fileName;
        switch (requestCode) {
            case REQUEST_STANCE_IMAGE:
                fileName = "stance_" + System.currentTimeMillis() + ".jpg";
                break;
            case REQUEST_AIM_POINT_IMAGE:
                fileName = "aimpoint_" + System.currentTimeMillis() + ".jpg";
                break;
            case REQUEST_LANDING_POINT_IMAGE:
                fileName = "landingpoint_" + System.currentTimeMillis() + ".jpg";
                break;
            default:
                fileName = "image_" + System.currentTimeMillis() + ".jpg";
        }

        File imageFile = new File(imageDir, fileName);
        try {
            FileOutputStream out = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.flush();
            out.close();
            return imageFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void saveDaojuInformation() {
        String throwingMethod = etThrowingMethod.getText().toString().trim();
        String toolName = etToolName.getText().toString().trim();
        String position = etPosition.getText().toString().trim();

        // 检查输入是否为空
        if (toolName.isEmpty()) {
            Toast.makeText(this, "道具名称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查是否选择了道具类型
        int selectedTypeId = rgDaojuType.getCheckedRadioButtonId();
        if (selectedTypeId == -1) {
            Toast.makeText(this, "请选择道具类型", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取选中的道具类型
        int daojuType = getTypeFromRadioButton(selectedTypeId);

        // 插入数据到数据库
        long infoId = daojuInformationDAO.insertDaojuInformation(
                throwingMethod,
                stanceImagePath.isEmpty() ? "" : stanceImagePath,
                aimPointImagePath.isEmpty() ? "" : aimPointImagePath,
                landingPointImagePath.isEmpty() ? "" : landingPointImagePath,
                toolName);

        if (infoId != -1) {
            Toast.makeText(this, "道具信息保存成功", Toast.LENGTH_SHORT).show();

            // 为选中的道具类型创建关联记录，使用正确的地图ID
            mapDAO.insertDaoju(selectedMapId, daojuType, position, (int) infoId);

            // 返回DaojuFragment界面
            finish();
        } else {
            Toast.makeText(this, "道具信息保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 根据选中的单选按钮ID获取对应的道具类型
     * @param radioButtonId 选中的单选按钮ID
     * @return 道具类型
     */
    private int getTypeFromRadioButton(int radioButtonId) {
        if (radioButtonId == R.id.rb_smoke) {
            return TYPE_SMOKE;
        } else if (radioButtonId == R.id.rb_molotov) {
            return TYPE_MOLOTOV;
        } else if (radioButtonId == R.id.rb_grenade) {
            return TYPE_GRENADE;
        } else if (radioButtonId == R.id.rb_flashbang) {
            return TYPE_FLASHBANG;
        }
        return TYPE_SMOKE; // 默认返回烟雾弹
    }
}
