package com.example.kin;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.kin.data.KinRepository;
import com.example.kin.model.SessionUser;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.AiRecommendFragment;
import com.example.kin.ui.AiSettingsActivity;
import com.example.kin.ui.AuthActivity;
import com.example.kin.ui.HomeFragment;
import com.example.kin.ui.LibraryFragment;
import com.example.kin.ui.MessagesActivity;
import com.example.kin.ui.PostDetailActivity;
import com.example.kin.ui.ProfileFragment;
import com.example.kin.ui.PublishFragment;
import com.example.kin.ui.adapter.MainPagerAdapter;
import com.example.kin.ui.admin.AdminCenterActivity;
import com.example.kin.ui.common.RemoteImageLoader;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener {
    private KinRepository repository;
    private RemoteImageLoader imageLoader;
    private MaterialToolbar topBar;
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private List<Fragment> rootFragments;
    private boolean autoLoginInFlight;

    private final int[] navIds = new int[]{
            R.id.nav_home,
            R.id.nav_library,
            R.id.nav_publish,
            R.id.nav_ai,
            R.id.nav_profile
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        repository = new KinRepository(this);
        imageLoader = new RemoteImageLoader();

        rootFragments = Arrays.asList(
                new HomeFragment(),
                new LibraryFragment(),
                new PublishFragment(),
                new AiRecommendFragment(),
                new ProfileFragment()
        );

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.topBar).setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            findViewById(R.id.bottomNavigation).setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        topBar = findViewById(R.id.topBar);
        topBar.setNavigationIcon(null);
        topBar.setTitle("Kin");
        topBar.inflateMenu(R.menu.menu_main_top);
        topBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_ai_settings) {
                openAiSettings();
                return true;
            }
            return false;
        });

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnItemSelectedListener(this);

        viewPager = findViewById(R.id.mainPager);
        viewPager.setAdapter(new MainPagerAdapter(this, rootFragments));
        viewPager.setOffscreenPageLimit(rootFragments.size());
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (bottomNavigationView.getSelectedItemId() != navIds[position]) {
                    bottomNavigationView.setSelectedItemId(navIds[position]);
                }
                updateToolbarForPage(position);
            }
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            viewPager.setCurrentItem(0, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshToolbarSubtitle();
        updateToolbarForPage(viewPager.getCurrentItem());
        ensureSessionAlive();
    }

    public KinRepository getRepository() {
        return repository;
    }

    public RemoteImageLoader getImageLoader() {
        return imageLoader;
    }

    public void setTopBar(String title, String subtitle) {
        topBar.setTitle(title);
        topBar.setSubtitle(subtitle);
    }

    public void refreshToolbarSubtitle() {
        SessionUser user = repository.getSessionManager().getUser();
        String subtitle = repository.getSessionManager().isLoggedIn()
                ? user.username + " | " + user.role
                : "";
        topBar.setSubtitle(subtitle);
    }

    public void openPostDetail(long postId, boolean mine) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, postId);
        intent.putExtra(PostDetailActivity.EXTRA_MINE, mine);
        startActivity(intent);
    }

    public void openMessages() {
        startActivity(new Intent(this, MessagesActivity.class));
    }

    public void openAdminCenter() {
        startActivity(new Intent(this, AdminCenterActivity.class));
    }

    public void openAiSettings() {
        startActivity(new Intent(this, AiSettingsActivity.class));
    }

    public void switchToPublish() {
        viewPager.setCurrentItem(2, false);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        for (int i = 0; i < navIds.length; i++) {
            if (navIds[i] == itemId) {
                if (viewPager.getCurrentItem() != i) {
                    viewPager.setCurrentItem(i, false);
                } else {
                    updateToolbarForPage(i);
                }
                return true;
            }
        }
        return false;
    }

    private void updateToolbarForPage(int position) {
        String[] titles = {"\u9996\u9875", "\u8d44\u6599\u5e93", "\u53d1\u5e16", "AI\u63a8\u8350", "\u6211"};
        setTopBar(titles[position], "");
        refreshToolbarSubtitle();
    }

    private void ensureSessionAlive() {
        if (repository.getSessionManager().isLoggedIn() || autoLoginInFlight) {
            return;
        }
        autoLoginInFlight = true;
        topBar.setSubtitle("\u6b63\u5728\u6062\u590d\u767b\u5f55...");
        repository.tryAutoLogin(new ApiCallback<>() {
            @Override
            public void onSuccess(SessionUser data) {
                autoLoginInFlight = false;
                refreshToolbarSubtitle();
            }

            @Override
            public void onError(ApiException exception) {
                autoLoginInFlight = false;
                openAuthAndFinish();
            }
        });
    }

    private void openAuthAndFinish() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
