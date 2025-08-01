package com.example.kinapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.kinapp.fragment.DaoJuFragment;
import com.example.kinapp.fragment.ZhanShuFragment;
import com.example.kinapp.utils.MyDBOpenHelper;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ViewPager mViewPager;
    private RadioGroup mRadioGroup;
    private Spinner spinner;

    // 底部导航栏按钮 ID 数组
    private final int[] radioButtonIds = {
            R.id.rb_daoju,
            R.id.rb_zhanshu,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化控件
        initView();

        // 初始化 Spinner
        spinner = findViewById(R.id.GameValues);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGame = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 可选实现
            }
        });

        MyDBOpenHelper myDBOpenHelper = new MyDBOpenHelper(this, "KinApp.db", null, 1);
    }

    private void initView() {
        mViewPager = findViewById(R.id.viewpager);
        mRadioGroup = findViewById(R.id.rg_tab);

        if (mViewPager == null || mRadioGroup == null) {
            Log.e(TAG, "控件未正确初始化");
            return;
        }

        // 设置适配器
        mViewPager.setAdapter(new MyViewPagerAdapter(getSupportFragmentManager()));

        // 设置页面切换监听，同步底部导航栏选中状态
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                updateRadioButtonSelection(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        // 设置 RadioGroup 点击事件
        mRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(TAG, "选中按钮 ID: " + checkedId);

            if (checkedId == R.id.rb_daoju) {
                mViewPager.setCurrentItem(0, true);
            } else if (checkedId == R.id.rb_zhanshu) {
                mViewPager.setCurrentItem(1, true);
            }  else {
                Log.w(TAG, "未知按钮被点击");
            }
        });
    }

    // 更新底部导航栏选中状态
    private void updateRadioButtonSelection(int position) {
        for (int i = 0; i < radioButtonIds.length; i++) {
            RadioButton rb = findViewById(radioButtonIds[i]);
            if (rb != null) {
                rb.setChecked(i == position);
            }
        }
    }

    // ViewPager 适配器
    private class MyViewPagerAdapter extends FragmentPagerAdapter {

        public MyViewPagerAdapter(@NonNull FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new DaoJuFragment();
                case 1: return new ZhanShuFragment();
                default: return new DaoJuFragment();
            }
        }

        @Override
        public int getCount() {
            return 2; // 修改为实际的 Fragment 数量
        }
    }

    @Override
    public void onBackPressed() {
        // 获取当前显示的 Fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager + ":" + mViewPager.getCurrentItem());

        // 检查是否是 DaoJuFragment 并且是否处理了返回键事件
        if (currentFragment instanceof DaoJuFragment) {
            if (((DaoJuFragment) currentFragment).onBackPressed()) {
                // 如果 Fragment 处理了返回键事件，则不调用 super.onBackPressed()
                return;
            }
        }

        // 如果 Fragment 没有处理返回键事件，则调用默认的返回键处理
        super.onBackPressed();
    }
}
