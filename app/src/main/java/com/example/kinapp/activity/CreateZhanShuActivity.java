package com.example.kinapp.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kinapp.R;
import com.example.kinapp.utils.ZhanShuInformationDAO;

public class CreateZhanShuActivity extends AppCompatActivity {
    private Spinner zhanShuTypeSpinner;
    private EditText zhanShuNameEditText;
    private EditText zhanShuDescriptionEditText;
    private EditText[] memberEditTexts;
    private EditText[] memberRoleEditTexts;
    private Button cancelButton;
    private Button saveButton;
    private ZhanShuInformationDAO zhanShuInformationDAO;
    private SharedPreferences sharedPreferences;
    private int mapId;
    private int zhanShuId = -1; // 用于修改模式

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_zhanshu);

        // 初始化UI组件
        zhanShuTypeSpinner = findViewById(R.id.zhanshu_type);
        zhanShuNameEditText = findViewById(R.id.zhanshu_name);
        zhanShuDescriptionEditText = findViewById(R.id.zhanshu_description);
        memberEditTexts = new EditText[5];
        memberRoleEditTexts = new EditText[5];

        for (int i = 0; i < 5; i++) {
            int memberId = getResources().getIdentifier("member" + (i + 1), "id", getPackageName());
            int roleId = getResources().getIdentifier("member" + (i + 1) + "_role", "id", getPackageName());
            memberEditTexts[i] = findViewById(memberId);
            memberRoleEditTexts[i] = findViewById(roleId);
        }

        cancelButton = findViewById(R.id.cancel_button);
        saveButton = findViewById(R.id.save_button);

        // 初始化数据库
        zhanShuInformationDAO = new ZhanShuInformationDAO(this);
        sharedPreferences = getSharedPreferences("kinapp", MODE_PRIVATE);

        // 获取传递的参数
        Intent intent = getIntent();
        mapId = intent.getIntExtra("mapId", -1);
        zhanShuId = intent.getIntExtra("zhanShuId", -1);

        // 设置战术类型 spinner
        String[] typeNames = {"Rush", "Split", "Contain", "Lurk", "Default"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, typeNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        zhanShuTypeSpinner.setAdapter(adapter);

        // 如果是修改模式，加载战术信息
        if (zhanShuId != -1) {
            loadZhanShuInformation();
        } else {
            // 加载成员名称
            loadMemberNames();
        }

        // 设置按钮点击事件
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveZhanShu();
            }
        });
    }

    private void loadMemberNames() {
        for (int i = 0; i < 5; i++) {
            String memberName = sharedPreferences.getString("member" + (i + 1), "成员" + (i + 1));
            memberEditTexts[i].setText(memberName);
        }
    }

    private void saveMemberNames() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (int i = 0; i < 5; i++) {
            String memberName = memberEditTexts[i].getText().toString().trim();
            if (!memberName.isEmpty()) {
                editor.putString("member" + (i + 1), memberName);
            }
        }
        editor.apply();
    }

    private void loadZhanShuInformation() {
        // 加载战术信息
        ZhanShuInformationDAO.ZhanShuInformationWithMapAndType item = zhanShuInformationDAO.getZhanShuInformationWithMapAndTypeById(zhanShuId);
        if (item != null) {
            zhanShuNameEditText.setText(item.getName());
            zhanShuDescriptionEditText.setText(item.getDescription());
            zhanShuTypeSpinner.setSelection(item.getType() - 1); // 类型从1开始，所以减1
            mapId = item.getMapId(); // 确保mapId正确
            
            // 加载成员信息
            if (item.getMember1() != null) {
                memberEditTexts[0].setText(item.getMember1());
            }
            if (item.getMember1Role() != null) {
                memberRoleEditTexts[0].setText(item.getMember1Role());
            }
            if (item.getMember2() != null) {
                memberEditTexts[1].setText(item.getMember2());
            }
            if (item.getMember2Role() != null) {
                memberRoleEditTexts[1].setText(item.getMember2Role());
            }
            if (item.getMember3() != null) {
                memberEditTexts[2].setText(item.getMember3());
            }
            if (item.getMember3Role() != null) {
                memberRoleEditTexts[2].setText(item.getMember3Role());
            }
            if (item.getMember4() != null) {
                memberEditTexts[3].setText(item.getMember4());
            }
            if (item.getMember4Role() != null) {
                memberRoleEditTexts[3].setText(item.getMember4Role());
            }
            if (item.getMember5() != null) {
                memberEditTexts[4].setText(item.getMember5());
            }
            if (item.getMember5Role() != null) {
                memberRoleEditTexts[4].setText(item.getMember5Role());
            }
        }
    }

    private void saveZhanShu() {
        String name = zhanShuNameEditText.getText().toString().trim();
        String description = zhanShuDescriptionEditText.getText().toString().trim();
        int type = zhanShuTypeSpinner.getSelectedItemPosition() + 1; // 类型从1开始

        if (name.isEmpty()) {
            zhanShuNameEditText.setError("请输入战术名称");
            return;
        }

        // 保存成员名称
        saveMemberNames();

        // 获取成员信息
        String member1 = memberEditTexts[0].getText().toString().trim();
        String member1Role = memberRoleEditTexts[0].getText().toString().trim();
        String member2 = memberEditTexts[1].getText().toString().trim();
        String member2Role = memberRoleEditTexts[1].getText().toString().trim();
        String member3 = memberEditTexts[2].getText().toString().trim();
        String member3Role = memberRoleEditTexts[2].getText().toString().trim();
        String member4 = memberEditTexts[3].getText().toString().trim();
        String member4Role = memberRoleEditTexts[3].getText().toString().trim();
        String member5 = memberEditTexts[4].getText().toString().trim();
        String member5Role = memberRoleEditTexts[4].getText().toString().trim();

        if (zhanShuId == -1) {
            // 新增战术
            zhanShuInformationDAO.insertZhanShuInformation(mapId, type, name, description, 
                                                     member1, member1Role, 
                                                     member2, member2Role, 
                                                     member3, member3Role, 
                                                     member4, member4Role, 
                                                     member5, member5Role);
        } else {
            // 修改战术
            zhanShuInformationDAO.updateZhanShuInformation(zhanShuId, type, name, description, 
                                                     member1, member1Role, 
                                                     member2, member2Role, 
                                                     member3, member3Role, 
                                                     member4, member4Role, 
                                                     member5, member5Role);
        }

        // 返回上一页
        finish();
    }
}
