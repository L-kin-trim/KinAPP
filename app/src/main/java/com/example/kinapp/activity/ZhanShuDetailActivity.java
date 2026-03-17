package com.example.kinapp.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.kinapp.R;
import com.example.kinapp.utils.MapDAO;
import com.example.kinapp.utils.ZhanShuInformationDAO;

public class ZhanShuDetailActivity extends AppCompatActivity {

    private ZhanShuInformationDAO zhanShuInformationDAO;
    private int zhanshuId;

    private TextView zhanshuNameTextView;
    private TextView zhanshuTypeTextView;
    private TextView zhanshuMapTextView;
    private TextView zhanshuDescriptionTextView;
    private TextView member1;
    private TextView member1Role;
    private TextView member2;
    private TextView member2Role;
    private TextView member3;
    private TextView member3Role;
    private TextView member4;
    private TextView member4Role;
    private TextView member5;
    private TextView member5Role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zhanshu_detail);

        // 初始化UI组件
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("战术详情");
        
        // 设置返回按钮点击事件
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        zhanshuNameTextView = findViewById(R.id.zhanshu_name);
        zhanshuTypeTextView = findViewById(R.id.zhanshu_type);
        zhanshuMapTextView = findViewById(R.id.zhanshu_map);
        zhanshuDescriptionTextView = findViewById(R.id.zhanshu_description);
        member1 = findViewById(R.id.member1);
        member1Role = findViewById(R.id.member1_role);
        member2 = findViewById(R.id.member2);
        member2Role = findViewById(R.id.member2_role);
        member3 = findViewById(R.id.member3);
        member3Role = findViewById(R.id.member3_role);
        member4 = findViewById(R.id.member4);
        member4Role = findViewById(R.id.member4_role);
        member5 = findViewById(R.id.member5);
        member5Role = findViewById(R.id.member5_role);

        // 获取传入的战术ID
        zhanshuId = getIntent().getIntExtra("zhanshuId", -1);

        // 初始化数据库访问对象
        zhanShuInformationDAO = new ZhanShuInformationDAO(this);

        // 加载战术详情
        loadZhanShuDetail();
    }

    private void loadZhanShuDetail() {
        if (zhanshuId != -1) {
            ZhanShuInformationDAO.ZhanShuInformationWithMapAndType item = zhanShuInformationDAO.getZhanShuInformationWithMapAndTypeById(zhanshuId);
            if (item != null) {
                zhanshuNameTextView.setText(item.getName());
                zhanshuTypeTextView.setText(getZhanShuTypeName(item.getType()));
                zhanshuMapTextView.setText(getMapName(item.getMapId()));
                zhanshuDescriptionTextView.setText(item.getDescription());
                
                // 显示成员信息
                if (item.getMember1() != null) {
                    member1.setText(item.getMember1());
                } else {
                    member1.setText("成员1");
                }
                if (item.getMember1Role() != null) {
                    member1Role.setText(item.getMember1Role());
                }
                
                if (item.getMember2() != null) {
                    member2.setText(item.getMember2());
                } else {
                    member2.setText("成员2");
                }
                if (item.getMember2Role() != null) {
                    member2Role.setText(item.getMember2Role());
                }
                
                if (item.getMember3() != null) {
                    member3.setText(item.getMember3());
                } else {
                    member3.setText("成员3");
                }
                if (item.getMember3Role() != null) {
                    member3Role.setText(item.getMember3Role());
                }
                
                if (item.getMember4() != null) {
                    member4.setText(item.getMember4());
                } else {
                    member4.setText("成员4");
                }
                if (item.getMember4Role() != null) {
                    member4Role.setText(item.getMember4Role());
                }
                
                if (item.getMember5() != null) {
                    member5.setText(item.getMember5());
                } else {
                    member5.setText("成员5");
                }
                if (item.getMember5Role() != null) {
                    member5Role.setText(item.getMember5Role());
                }
            }
        }
    }

    private String getZhanShuTypeName(int type) {
        switch (type) {
            case 1:
                return "RUSH";
            case 2:
                return "防守";
            case 3:
                return "进攻";
            case 4:
                return "经济";
            default:
                return "其他";
        }
    }

    private String getMapName(int mapId) {
        MapDAO mapDAO = new MapDAO(this);
        MapDAO.MapItem mapItem = mapDAO.getMapById(mapId);
        return mapItem != null ? mapItem.getName() : "未知地图";
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
