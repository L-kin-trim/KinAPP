package com.example.kinapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.util.List;


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

        // 获取传递的地图信息和道具信息ID
        Intent intent = getIntent();
        selectedMapId = intent.getIntExtra("map_id", 1);
        selectedMapName = intent.getStringExtra("map_name");
        int daojuInfoId = intent.getIntExtra("info_id", -1);

        if (selectedMapName == null) {
            selectedMapName = "默认地图";
        }

        // 初始化数据库访问对象
        daojuInformationDAO = new DaojuInformationDAO(this);
        mapDAO = new MapDAO(this);

        // 初始化视图
        initViews();

        // 设置提示信息
        tvMapInfo.setText("您正在修改 " + selectedMapName + " 地图的道具");

        // 如果有道具信息ID，则加载道具信息
        if (daojuInfoId != -1) {
            loadDaojuInformation(daojuInfoId);
        }

        // 设置保存按钮点击事件
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDaojuInformation(daojuInfoId);
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


    // ... existing code ...
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
// ... existing code ...



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

    // ... existing code ...
    private void loadDaojuInformation(int daojuInfoId) {
        DaojuInformationDAO.DaojuInformation info = daojuInformationDAO.getDaojuInformationById(daojuInfoId);
        if (info != null) {
            etThrowingMethod.setText(info.getThrowingMethod());
            etToolName.setText(info.getToolName());
            // position字段需要从daoju表中获取

            // 设置图片预览
            stanceImagePath = info.getStanceImagePath();
            aimPointImagePath = info.getAimPointImagePath();
            landingPointImagePath = info.getLandingPointImagePath();

            if (!stanceImagePath.isEmpty()) {
                Bitmap stanceBitmap = BitmapFactory.decodeFile(stanceImagePath);
                if (stanceBitmap != null) {
                    ivStanceImagePreview.setImageBitmap(stanceBitmap);
                }
            }

            if (!aimPointImagePath.isEmpty()) {
                Bitmap aimPointBitmap = BitmapFactory.decodeFile(aimPointImagePath);
                if (aimPointBitmap != null) {
                    ivAimPointImagePreview.setImageBitmap(aimPointBitmap);
                }
            }

            if (!landingPointImagePath.isEmpty()) {
                Bitmap landingPointBitmap = BitmapFactory.decodeFile(landingPointImagePath);
                if (landingPointBitmap != null) {
                    ivLandingPointImagePreview.setImageBitmap(landingPointBitmap);
                }
            }

            // 从daoju表中获取道具类型和位置信息
            MapDAO mapDAO = new MapDAO(this);
            List<MapDAO.DaojuItem> daojuItems = mapDAO.getAllDaojuItemsByInfoId(daojuInfoId);
            if (!daojuItems.isEmpty()) {
                MapDAO.DaojuItem daojuItem = daojuItems.get(0); // 取第一个匹配项

                // 设置道具类型
                int typeId = daojuItem.getType();
                switch (typeId) {
                    case TYPE_SMOKE:
                        rbSmoke.setChecked(true);
                        break;
                    case TYPE_MOLOTOV:
                        rbMolotov.setChecked(true);
                        break;
                    case TYPE_GRENADE:
                        rbGrenade.setChecked(true);
                        break;
                    case TYPE_FLASHBANG:
                        rbFlashbang.setChecked(true);
                        break;
                }

                // 设置位置信息
                String position = daojuItem.getPosition();
                if (position != null) {
                    etPosition.setText(position);
                }
            }
        }
    }
// ... existing code ...


    // ... existing code ...
    private void saveDaojuInformation(int daojuInfoId) {
        String throwingMethod = etThrowingMethod.getText().toString().trim();
        String toolName = etToolName.getText().toString().trim();
        String position = etPosition.getText().toString().trim();
        // position字段在数据库中不存在，所以不需要获取

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

        long result = -1;
        long daojuResult = -1; // 用于保存daoju表的插入结果
        // 判断是新增还是更新
        if (daojuInfoId == -1) {
            // 新增道具信息
            result = daojuInformationDAO.insertDaojuInformation(
                    throwingMethod,
                    stanceImagePath.isEmpty() ? "" : stanceImagePath,
                    aimPointImagePath.isEmpty() ? "" : aimPointImagePath,
                    landingPointImagePath.isEmpty() ? "" : landingPointImagePath,
                    toolName);

            // 如果道具信息插入成功，则在daoju表中添加关联记录
            if (result > 0) {
                // 插入daoju表记录，关联地图和道具信息
                MapDAO mapDAO = new MapDAO(this);
                daojuResult = mapDAO.insertDaoju(selectedMapId, daojuType, position, (int) result);
            }
        } else {
            // 更新数据到数据库
            result = daojuInformationDAO.updateDaojuInformation(
                    daojuInfoId,
                    throwingMethod,
                    stanceImagePath.isEmpty() ? "" : stanceImagePath,
                    aimPointImagePath.isEmpty() ? "" : aimPointImagePath,
                    landingPointImagePath.isEmpty() ? "" : landingPointImagePath,
                    toolName);
        }

        if (result > 0) {
            Toast.makeText(this, "道具信息保存成功", Toast.LENGTH_SHORT).show();

            // 返回DaojuFragment界面
            finish();
        } else {
            Toast.makeText(this, "道具信息保存失败", Toast.LENGTH_SHORT).show();
        }
    }
// ... existing code ...





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
