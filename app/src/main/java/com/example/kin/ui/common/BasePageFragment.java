package com.example.kin.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.kin.R;

public abstract class BasePageFragment extends Fragment {
    protected LinearLayout contentLayout;
    protected ProgressBar progressBar;
    protected TextView statusView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(requireContext().getColor(KinUi.isNight(requireContext()) ? R.color.kin_dark_bg : R.color.kin_light_bg));

        contentLayout = KinUi.vertical(requireContext());
        contentLayout.setPadding(KinUi.dp(requireContext(), 18), KinUi.dp(requireContext(), 12), KinUi.dp(requireContext(), 18), KinUi.dp(requireContext(), 24));

        progressBar = new ProgressBar(requireContext());
        statusView = KinUi.muted(requireContext(), "", 13);
        statusView.setVisibility(View.GONE);

        contentLayout.addView(progressBar);
        contentLayout.addView(statusView);
        scrollView.addView(contentLayout, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        onPageReady();
        return scrollView;
    }

    protected abstract void onPageReady();

    protected void setLoading(boolean loading, String message) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (statusView != null) {
            statusView.setVisibility(message == null || message.isEmpty() ? View.GONE : View.VISIBLE);
            statusView.setText(message);
        }
    }
}
