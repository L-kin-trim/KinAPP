package com.example.kin.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.R;
import com.example.kin.data.KinRepository;
import com.example.kin.model.CheckInSummary;
import com.example.kin.model.SessionUser;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

public class CheckInActivity extends AppCompatActivity {
    private KinRepository repository;
    private LinearLayout contentLayout;
    private ProgressBar progressBar;
    private TextView statusView;
    private CheckInSummary summary = CheckInSummary.currentMonthPreview();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new KinRepository(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("每日签到");
        toolbar.setNavigationIcon(R.drawable.ic_nav_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(this);
        contentLayout = KinUi.vertical(this);
        contentLayout.setPadding(KinUi.dp(this, 18), KinUi.dp(this, 12), KinUi.dp(this, 18), KinUi.dp(this, 24));
        progressBar = new ProgressBar(this);
        statusView = KinUi.muted(this, "", 13);
        statusView.setVisibility(View.GONE);
        contentLayout.addView(progressBar);
        contentLayout.addView(statusView);
        scrollView.addView(contentLayout);
        root.addView(scrollView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSummary();
    }

    private void loadSummary() {
        if (!repository.getSessionManager().isLoggedIn()) {
            summary = CheckInSummary.currentMonthPreview();
            render();
            setLoading(false, "请先登录后再签到。");
            return;
        }
        setLoading(true, "正在同步签到记录…");
        repository.getCheckInSummary(new ApiCallback<>() {
            @Override
            public void onSuccess(CheckInSummary data) {
                summary = data;
                if (!summary.signedToday) {
                    submitCheckIn("今日已自动签到。");
                    return;
                }
                render();
                setLoading(false, "");
            }

            @Override
            public void onError(ApiException exception) {
                summary = CheckInSummary.currentMonthPreview();
                render();
                setLoading(false, exception.isFeatureUnavailable()
                        ? "后端签到接口未开放，当前显示签到页预览。"
                        : "签到记录同步失败：" + exception.getMessage());
            }
        });
    }

    private void submitCheckIn(String successMessage) {
        setLoading(true, "正在签到…");
        repository.checkInToday(new ApiCallback<>() {
            @Override
            public void onSuccess(CheckInSummary data) {
                summary = data;
                render();
                setLoading(false, successMessage);
            }

            @Override
            public void onError(ApiException exception) {
                render();
                setLoading(false, "签到失败：" + exception.getMessage());
            }
        });
    }

    private void render() {
        contentLayout.removeAllViews();
        contentLayout.addView(progressBar);
        contentLayout.addView(statusView);
        contentLayout.addView(headerCard());
        contentLayout.addView(taskCard());
        contentLayout.addView(calendarCard());
        contentLayout.addView(ruleText());
    }

    private View headerCard() {
        MaterialCardView card = KinUi.card(this);
        card.setCardBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_panel_alt : R.color.kin_accent));
        LinearLayout body = KinUi.sectionContainer(this, 20);
        body.setGravity(Gravity.CENTER_HORIZONTAL);

        SessionUser user = repository.getSessionManager().getUser();
        TextView name = KinUi.text(this, user == null || TextUtils.isEmpty(user.username) ? "Kin 用户" : user.username, 24, true);
        name.setTextColor(getColor(R.color.kin_text_inverse));
        TextView level = KinUi.muted(this, "每日任务 · 连续签到 " + summary.currentStreakDays + " 天", 14);
        level.setTextColor(getColor(R.color.kin_accent_soft));

        body.addView(name);
        KinUi.margins(level, this, 0, 8, 0, 0);
        body.addView(level);
        card.addView(body);
        return card;
    }

    private View taskCard() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textBlock = KinUi.vertical(this);
        textBlock.addView(KinUi.text(this, "签到", 20, true));
        TextView reward = KinUi.muted(this, "连续 7 天以上奖励翻倍", 13);
        KinUi.margins(reward, this, 0, 6, 0, 0);
        textBlock.addView(reward);

        TextView streak = KinUi.text(this, summary.currentStreakDays + "天", 28, true);
        streak.setGravity(Gravity.END);
        row.addView(textBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(streak, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        body.addView(row);

        MaterialButton button = summary.signedToday
                ? KinUi.outlinedButton(this, "今日已签到")
                : KinUi.filledButton(this, "立即签到");
        button.setEnabled(!summary.signedToday && repository.getSessionManager().isLoggedIn());
        button.setOnClickListener(v -> submitCheckIn("签到成功。"));
        KinUi.margins(button, this, 0, 14, 0, 0);
        body.addView(button);
        card.addView(body);
        return card;
    }

    private View calendarCard() {
        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 0);
        TextView monthTitle = KinUi.text(this, String.format(Locale.CHINA, "%04d-%02d", summary.year, summary.month), 18, true);
        monthTitle.setGravity(Gravity.CENTER);
        KinUi.pad(monthTitle, this, 0, 16);
        body.addView(monthTitle);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        grid.setRowCount(7);
        String[] weeks = {"日", "一", "二", "三", "四", "五", "六"};
        for (String week : weeks) {
            grid.addView(headerCell(week));
        }
        fillCalendar(grid);
        body.addView(grid);
        card.addView(body);
        return card;
    }

    private TextView headerCell(String text) {
        TextView view = KinUi.text(this, text, 15, false);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_panel : R.color.kin_light_panel_alt));
        GridLayout.LayoutParams params = cellParams();
        params.height = KinUi.dp(this, 44);
        view.setLayoutParams(params);
        return view;
    }

    private void fillCalendar(GridLayout grid) {
        YearMonth month = YearMonth.of(summary.year, summary.month);
        LocalDate first = month.atDay(1);
        int leading = first.getDayOfWeek().getValue() % 7;
        LocalDate cursor = first.minusDays(leading);
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 42; i++) {
            boolean inCurrentMonth = cursor.getMonthValue() == summary.month;
            boolean signed = inCurrentMonth && summary.signedDays.contains(cursor.getDayOfMonth());
            boolean isToday = cursor.equals(today);
            grid.addView(dayCell(cursor.getDayOfMonth(), inCurrentMonth, signed, isToday));
            cursor = cursor.plusDays(1);
        }
    }

    private View dayCell(int day, boolean inCurrentMonth, boolean signed, boolean today) {
        FrameLayout cell = new FrameLayout(this);
        cell.setLayoutParams(cellParams());
        cell.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_panel : R.color.kin_light_panel));

        if (signed) {
            TextView check = KinUi.text(this, "✓", 30, true);
            check.setGravity(Gravity.CENTER);
            check.setAlpha(0.32f);
            check.setTextColor(getColor(R.color.kin_accent));
            FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
            );
            cell.addView(check, checkParams);
        }

        TextView value = KinUi.text(this, String.valueOf(day), today ? 19 : 17, today);
        value.setGravity(Gravity.CENTER);
        value.setTextColor(inCurrentMonth
                ? KinUi.color(this, com.google.android.material.R.attr.colorOnSurface)
                : getColor(R.color.kin_text_muted));
        cell.addView(value, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
        ));
        cell.setAlpha(inCurrentMonth ? 1f : 0.35f);
        return cell;
    }

    private GridLayout.LayoutParams cellParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = KinUi.dp(this, 58);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(1, 1, 1, 1);
        return params;
    }

    private View ruleText() {
        TextView text = KinUi.muted(this,
                "补签规则：\n当年首次补签花费100H币（往年1000H币），补签一次后花费翻倍，单次补签上限最多为1000H币（每年重新计算）",
                14);
        KinUi.margins(text, this, 4, 8, 4, 0);
        return text;
    }

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        statusView.setVisibility(TextUtils.isEmpty(message) ? View.GONE : View.VISIBLE);
        statusView.setText(message);
    }
}
