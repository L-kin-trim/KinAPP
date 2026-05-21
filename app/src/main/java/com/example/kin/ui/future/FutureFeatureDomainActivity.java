package com.example.kin.ui.future;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.R;
import com.example.kin.model.FutureFeatureDefinition;
import com.example.kin.model.FutureFeatureGroup;
import com.example.kin.model.FutureFeatureRegistry;
import com.example.kin.ui.common.FutureUi;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.appbar.MaterialToolbar;

public class FutureFeatureDomainActivity extends AppCompatActivity {
    public static final String EXTRA_GROUP_KEY = "extra_group_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String groupKey = getIntent().getStringExtra(EXTRA_GROUP_KEY);
        FutureFeatureGroup group = FutureFeatureRegistry.groupByKey(groupKey);
        if (group == null) {
            finish();
            return;
        }
        build(group);
    }

    private void build(FutureFeatureGroup group) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle(group.title);
        toolbar.setSubtitle(group.apiPrefix);
        toolbar.setNavigationIcon(R.drawable.ic_nav_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        root.addView(toolbar);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = KinUi.vertical(this);
        content.setPadding(KinUi.dp(this, 18), KinUi.dp(this, 12), KinUi.dp(this, 18), KinUi.dp(this, 24));
        content.addView(FutureUi.featureCard(this, group.title, group.summary, group.features.size() + " 项", v -> { }));

        for (FutureFeatureDefinition feature : group.features) {
            String badge = feature.section + (feature.aiEnabled ? " · AI" : "") + (feature.taskEnabled ? " · 任务" : "");
            content.addView(FutureUi.featureCard(this, feature.title, feature.summary, badge, v -> openFeature(feature.key)));
        }
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void openFeature(String featureKey) {
        Intent intent = new Intent(this, FutureFeatureDetailActivity.class);
        intent.putExtra(FutureFeatureDetailActivity.EXTRA_FEATURE_KEY, featureKey);
        startActivity(intent);
    }
}
