package com.example.kin.ui;

import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import com.example.kin.MainActivity;
import com.example.kin.data.AiConfigStore;
import com.example.kin.model.AiConfig;
import com.example.kin.model.LibraryItem;
import com.example.kin.model.PageResult;
import com.example.kin.model.ScoreboardSnapshot;
import com.example.kin.net.ApiCallback;
import com.example.kin.net.ApiException;
import com.example.kin.net.OpenAiStreamClient;
import com.example.kin.ui.common.BasePageFragment;
import com.example.kin.ui.common.KinUi;
import com.example.kin.util.ScoreboardOcrOrchestrator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AiRecommendFragment extends BasePageFragment {
    private AiConfigStore configStore;
    private OpenAiStreamClient streamClient;
    private ScoreboardOcrOrchestrator ocrOrchestrator;
    private OpenAiStreamClient.StreamSession activeSession;

    private TextInputEditText mapEdit;
    private TextInputEditText scoreEdit;
    private TextInputEditText moneyEdit;
    private TextInputEditText kdaEdit;
    private TextInputEditText noteEdit;

    private TextView imageState;
    private ImageView imagePreview;
    private TextView rawOcrView;
    private TextView structuredView;
    private TextView streamStatus;
    private TextView outputView;
    private MaterialButton startButton;
    private MaterialButton stopButton;
    private MaterialButton retryButton;
    private MaterialCardView configHintCard;

    private Uri selectedImageUri;
    private Uri pendingCameraUri;
    private ScoreboardSnapshot lastOcrSnapshot;

    private final ActivityResultLauncher<String> galleryPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri == null) {
                    return;
                }
                selectedImageUri = uri;
                renderSelectedImage(uri);
                setLoading(false, "\u5df2\u9009\u62e9\u56fe\u7247\uff0c\u8bf7\u70b9\u51fb\u300c\u8bc6\u522b\u8ba1\u5206\u677f\u300d\u3002");
            });

    private final ActivityResultLauncher<Uri> cameraCapture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (!success || pendingCameraUri == null) {
                    setLoading(false, "\u5df2\u53d6\u6d88\u62cd\u6444\u3002");
                    return;
                }
                selectedImageUri = pendingCameraUri;
                renderSelectedImage(selectedImageUri);
                setLoading(false, "\u62cd\u6444\u6210\u529f\uff0c\u8bf7\u70b9\u51fb\u300c\u8bc6\u522b\u8ba1\u5206\u677f\u300d\u3002");
            });

    @Override
    protected void onPageReady() {
        MainActivity activity = (MainActivity) requireActivity();
        activity.setTopBar("AI \u63a8\u8350", "");
        configStore = new AiConfigStore(activity);
        streamClient = new OpenAiStreamClient();
        ocrOrchestrator = new ScoreboardOcrOrchestrator();

        configHintCard = buildConfigHintCard(activity);
        contentLayout.addView(configHintCard);
        contentLayout.addView(buildImageCard(activity));
        contentLayout.addView(buildOcrCard(activity));
        contentLayout.addView(buildOutputCard(activity));

        refreshConfigHint();
        setLoading(false, "");
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshConfigHint();
    }

    private MaterialCardView buildConfigHintCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 20);
        body.addView(KinUi.text(activity, "\u5148\u914d\u7f6e AI \u6a21\u578b\u624d\u80fd\u63a8\u8350", 20, true));
        TextView hint = KinUi.muted(activity,
                "\u5f53\u524d\u672a\u914d\u7f6e AI\uff0c\u53ef\u70b9\u51fb\u53f3\u4e0a\u89d2\u9f7f\u8f6e\u6216\u300c\u6211 > AI \u6a21\u578b\u914d\u7f6e\u300d\u3002",
                14);
        KinUi.margins(hint, activity, 0, 8, 0, 0);
        body.addView(hint);

        MaterialButton configure = KinUi.filledButton(activity, "\u524d\u5f80 AI \u914d\u7f6e");
        configure.setOnClickListener(v -> activity.openAiSettings());
        KinUi.margins(configure, activity, 0, 12, 0, 0);
        body.addView(configure);
        card.addView(body);
        return card;
    }

    private View buildImageCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 20);
        body.addView(KinUi.text(activity, "\u8ba1\u5206\u677f\u56fe\u7247", 20, true));
        TextView subtitle = KinUi.muted(activity, "\u652f\u6301\u76f8\u518c\u6216\u62cd\u7167\uff0c\u672c\u5730 OCR \u63d0\u53d6\u5730\u56fe\u3001\u6bd4\u5206\u3001\u73a9\u5bb6\u3001\u7ecf\u6d4e\u3002", 14);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton pickButton = KinUi.filledButton(activity, "\u76f8\u518c\u9009\u56fe");
        pickButton.setOnClickListener(v -> galleryPicker.launch("image/*"));
        MaterialButton cameraButton = KinUi.outlinedButton(activity, "\u62cd\u7167");
        cameraButton.setOnClickListener(v -> launchCamera());
        actions.addView(pickButton);
        actions.addView(cameraButton);
        KinUi.margins(cameraButton, activity, 10, 0, 0, 0);
        KinUi.margins(actions, activity, 0, 14, 0, 0);
        body.addView(actions);

        imageState = KinUi.muted(activity, "\u5c1a\u672a\u9009\u62e9\u56fe\u7247\u3002", 13);
        body.addView(imageState);
        KinUi.margins(imageState, activity, 0, 10, 0, 0);

        imagePreview = new ImageView(activity);
        imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagePreview.setAdjustViewBounds(true);
        imagePreview.setVisibility(View.GONE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                KinUi.dp(activity, 220)
        );
        imagePreview.setLayoutParams(params);
        KinUi.margins(imagePreview, activity, 0, 12, 0, 0);
        body.addView(imagePreview);

        MaterialButton ocrButton = KinUi.outlinedButton(activity, "\u8bc6\u522b\u8ba1\u5206\u677f");
        ocrButton.setOnClickListener(v -> runOcr());
        KinUi.margins(ocrButton, activity, 0, 14, 0, 0);
        body.addView(ocrButton);
        card.addView(body);
        return card;
    }

    private View buildOcrCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 20);
        body.addView(KinUi.text(activity, "OCR \u7ed3\u6784\u5316", 20, true));
        TextView subtitle = KinUi.muted(activity, "\u53d1\u9001 AI \u524d\u8bf7\u5148\u6838\u5bf9\u5e76\u53ef\u624b\u52a8\u4fee\u6b63\u3002", 14);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);

        mapEdit = addInput(body, "\u5730\u56fe\uff08\u5982\uff1a\u6c99\u4e8c\uff09", false);
        scoreEdit = addInput(body, "\u6bd4\u5206\uff08\u5982 9:2\uff09", false);
        moneyEdit = addInput(body, "\u7ecf\u6d4e\uff08\u5982 $5650, $1200\uff09", false);
        kdaEdit = addInput(body, "K/D/A \u6458\u8981", false);
        noteEdit = addInput(body, "\u8865\u5145\u4e0a\u4e0b\u6587\uff08\u53ef\u9009\uff09", true);

        TextView structuredTitle = KinUi.muted(activity, "\u7ed3\u6784\u5316\u6458\u8981", 13);
        KinUi.margins(structuredTitle, activity, 0, 12, 0, 0);
        body.addView(structuredTitle);

        structuredView = KinUi.muted(activity, "", 13);
        structuredView.setVisibility(View.GONE);
        KinUi.margins(structuredView, activity, 0, 6, 0, 0);
        body.addView(structuredView);

        TextView rawTitle = KinUi.muted(activity, "OCR \u539f\u6587", 13);
        KinUi.margins(rawTitle, activity, 0, 12, 0, 0);
        body.addView(rawTitle);

        rawOcrView = KinUi.muted(activity, "", 12);
        rawOcrView.setVisibility(View.GONE);
        KinUi.margins(rawOcrView, activity, 0, 6, 0, 0);
        body.addView(rawOcrView);

        card.addView(body);
        return card;
    }

    private View buildOutputCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 20);
        body.addView(KinUi.text(activity, "AI \u6218\u672f\u63a8\u8350", 20, true));
        streamStatus = KinUi.muted(activity, "\u7b49\u5f85\u5f00\u59cb", 13);
        KinUi.margins(streamStatus, activity, 0, 8, 0, 0);
        body.addView(streamStatus);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        startButton = KinUi.filledButton(activity, "\u5f00\u59cb\u5206\u6790");
        stopButton = KinUi.outlinedButton(activity, "\u505c\u6b62");
        retryButton = KinUi.outlinedButton(activity, "\u91cd\u8bd5");
        startButton.setOnClickListener(v -> startStreaming(false));
        retryButton.setOnClickListener(v -> startStreaming(true));
        stopButton.setOnClickListener(v -> stopStreaming("\u5df2\u505c\u6b62"));
        actions.addView(startButton);
        actions.addView(stopButton);
        actions.addView(retryButton);
        KinUi.margins(stopButton, activity, 10, 0, 0, 0);
        KinUi.margins(retryButton, activity, 10, 0, 0, 0);
        KinUi.margins(actions, activity, 0, 12, 0, 0);
        body.addView(actions);
        stopButton.setEnabled(false);

        outputView = KinUi.muted(activity, "", 15);
        outputView.setTextIsSelectable(true);
        outputView.setMinLines(8);
        KinUi.margins(outputView, activity, 0, 14, 0, 0);
        body.addView(outputView);
        card.addView(body);
        return card;
    }

    private TextInputEditText addInput(LinearLayout parent, String hint, boolean multiline) {
        MainActivity activity = (MainActivity) requireActivity();
        TextInputLayout layout = KinUi.inputLayout(activity, hint, multiline);
        if (parent.getChildCount() > 0) {
            KinUi.margins(layout, activity, 0, 10, 0, 0);
        }
        parent.addView(layout);
        return KinUi.edit(layout);
    }

    private void launchCamera() {
        try {
            MainActivity activity = (MainActivity) requireActivity();
            File dir = new File(activity.getCacheDir(), "images");
            if (!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }
            File file = new File(dir, "ai_capture_" + System.currentTimeMillis() + ".jpg");
            pendingCameraUri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider",
                    file);
            cameraCapture.launch(pendingCameraUri);
        } catch (Exception exception) {
            setLoading(false, "\u62cd\u6444\u521d\u59cb\u5316\u5931\u8d25\uff1a" + exception.getMessage());
        }
    }

    private void renderSelectedImage(Uri uri) {
        imageState.setText("\u5df2\u9009\u62e9\uff1a" + uri.getLastPathSegment());
        imagePreview.setImageURI(uri);
        imagePreview.setVisibility(View.VISIBLE);
    }

    private void runOcr() {
        if (selectedImageUri == null) {
            setLoading(false, "\u8bf7\u5148\u9009\u62e9\u56fe\u7247\u3002");
            return;
        }
        setLoading(true, "\u6b63\u5728\u8fd0\u884c\u672c\u5730 OCR\uff08\u4e2d\u6587 + \u82f1\u6587\uff09...");
        ocrOrchestrator.recognize(requireContext(), selectedImageUri, new ScoreboardOcrOrchestrator.Callback() {
            @Override
            public void onSuccess(ScoreboardSnapshot snapshot) {
                lastOcrSnapshot = snapshot;
                mapEdit.setText(snapshot.mapName);
                scoreEdit.setText(snapshot.scoreText);
                moneyEdit.setText(snapshot.moneyText);
                kdaEdit.setText(snapshot.kdaText);
                rawOcrView.setText(snapshot.rawText);
                rawOcrView.setVisibility(TextUtils.isEmpty(snapshot.rawText) ? View.GONE : View.VISIBLE);

                String structured = buildStructuredText(snapshot);
                structuredView.setText(structured);
                structuredView.setVisibility(TextUtils.isEmpty(structured) ? View.GONE : View.VISIBLE);
                setLoading(false, "OCR \u5b8c\u6210\uff0c\u8bf7\u786e\u8ba4\u540e\u5f00\u59cb AI \u5206\u6790\u3002");
            }

            @Override
            public void onError(String message) {
                setLoading(false, "OCR \u5931\u8d25\uff1a" + message);
            }
        });
    }

    private void startStreaming(boolean retry) {
        AiConfig config = configStore.load();
        if (!config.isValid()) {
            refreshConfigHint();
            setLoading(false, "\u8bf7\u5148\u914d\u7f6e AI \u670d\u52a1\u5546\u3002");
            return;
        }

        ScoreboardSnapshot snapshot = buildSnapshotFromUi();
        if (!snapshot.hasCoreStats()) {
            setLoading(false, "\u8bf7\u5148\u8fdb\u884c OCR \u6216\u586b\u5199\u5173\u952e\u8ba1\u5206\u677f\u4fe1\u606f\u3002");
            return;
        }

        stopStreaming("");
        if (!retry) {
            outputView.setText("");
        }
        streamStatus.setText("\u6b63\u5728\u52a0\u8f7d\u672c\u5730\u5e93\u4e0a\u4e0b\u6587...");
        setStreaming(true);

        loadLibraryContext(snapshot, context -> {
            if (!isAdded()) {
                setStreaming(false);
                return;
            }
            streamStatus.setText("\u6b63\u5728\u751f\u6210 AI \u63a8\u8350...");
            activeSession = streamClient.streamScoreboardAdvice(
                    config,
                    snapshot,
                    text(noteEdit),
                    context,
                    new OpenAiStreamClient.StreamListener() {
                        @Override
                        public void onStart() {
                            streamStatus.setText("AI \u5df2\u8fde\u63a5\uff0c\u6b63\u5728\u8f93\u51fa...");
                        }

                        @Override
                        public void onDelta(String content) {
                            outputView.append(content);
                        }

                        @Override
                        public void onComplete() {
                            setStreaming(false);
                            streamStatus.setText("\u5206\u6790\u5b8c\u6210\u3002");
                        }

                        @Override
                        public void onError(String message) {
                            setStreaming(false);
                            streamStatus.setText("\u5206\u6790\u5931\u8d25\uff1a" + message);
                        }
                    }
            );
        });
    }

    private ScoreboardSnapshot buildSnapshotFromUi() {
        ScoreboardSnapshot snapshot = new ScoreboardSnapshot();
        snapshot.mapName = text(mapEdit);
        snapshot.scoreText = text(scoreEdit);
        snapshot.moneyText = text(moneyEdit);
        snapshot.kdaText = text(kdaEdit);
        snapshot.rawText = rawOcrView.getVisibility() == View.GONE ? "" : String.valueOf(rawOcrView.getText()).trim();

        if (lastOcrSnapshot != null) {
            snapshot.playerStatsText = lastOcrSnapshot.playerStatsText;
            snapshot.hotHandSummary = lastOcrSnapshot.hotHandSummary;
            snapshot.players.addAll(lastOcrSnapshot.players);
            snapshot.latinRawText = lastOcrSnapshot.latinRawText;
            snapshot.chineseRawText = lastOcrSnapshot.chineseRawText;
        }
        return snapshot;
    }

    private String buildStructuredText(ScoreboardSnapshot snapshot) {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(snapshot.mapName)) {
            builder.append("\u5730\u56fe\uff1a").append(snapshot.mapName).append('\n');
        }
        if (!TextUtils.isEmpty(snapshot.scoreText)) {
            builder.append("\u6bd4\u5206\uff1a").append(snapshot.scoreText).append('\n');
        }
        if (!TextUtils.isEmpty(snapshot.playerStatsText)) {
            builder.append("\u73a9\u5bb6\u7edf\u8ba1\uff1a\n").append(snapshot.playerStatsText).append('\n');
        }
        if (!TextUtils.isEmpty(snapshot.hotHandSummary)) {
            builder.append("\u624b\u611f\u5019\u9009\uff1a").append(snapshot.hotHandSummary).append('\n');
        }
        return builder.toString().trim();
    }

    private void loadLibraryContext(ScoreboardSnapshot snapshot, LibraryContextCallback callback) {
        MainActivity activity = (MainActivity) requireActivity();
        activity.getRepository().getLibraryItems("", 0, 30, new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<LibraryItem> selfItems) {
                loadFavoritesAndBuild(snapshot, selfItems.items, callback);
            }

            @Override
            public void onError(ApiException exception) {
                loadFavoritesAndBuild(snapshot, Collections.emptyList(), callback);
            }
        });
    }

    private void loadFavoritesAndBuild(ScoreboardSnapshot snapshot,
                                       List<LibraryItem> selfItems,
                                       LibraryContextCallback callback) {
        MainActivity activity = (MainActivity) requireActivity();
        activity.getRepository().getFavorites("", 0, 30, new ApiCallback<>() {
            @Override
            public void onSuccess(PageResult<LibraryItem> favoriteItems) {
                callback.onReady(buildLibraryContext(snapshot, selfItems, favoriteItems.items));
            }

            @Override
            public void onError(ApiException exception) {
                callback.onReady(buildLibraryContext(snapshot, selfItems, Collections.emptyList()));
            }
        });
    }

    private String buildLibraryContext(ScoreboardSnapshot snapshot,
                                       List<LibraryItem> selfItems,
                                       List<LibraryItem> favoriteItems) {
        Map<String, Candidate> unique = new LinkedHashMap<>();
        for (LibraryItem item : selfItems) {
            upsertCandidate(unique, item, false, snapshot);
        }
        for (LibraryItem item : favoriteItems) {
            upsertCandidate(unique, item, true, snapshot);
        }

        List<Candidate> ranked = new ArrayList<>(unique.values());
        ranked.sort(Comparator.comparingInt((Candidate c) -> c.score).reversed()
                .thenComparingInt(c -> c.item.favoriteCount));

        if (ranked.isEmpty()) {
            return "\u672a\u627e\u5230\u53ef\u7528\u7684\u672c\u5730\u5e93\u8bb0\u5f55\uff0c\u53ef\u4f7f\u7528\u901a\u7528 CS2 \u77e5\u8bc6\u8865\u5168\u5efa\u8bae\u3002";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\u8bf7\u4f18\u5148\u53c2\u8003\u4ee5\u4e0b\u672c\u5730\u5e93\u8bb0\u5f55\uff1a\n");
        int limit = Math.min(ranked.size(), 6);
        for (int i = 0; i < limit; i++) {
            Candidate candidate = ranked.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(candidate.favorite ? "[\u6536\u85cf] " : "[\u81ea\u5efa] ")
                    .append(safe(candidate.item.title))
                    .append(" | ")
                    .append(translateType(candidate.item.postType))
                    .append(" | \u5730\u56fe=")
                    .append(safe(candidate.item.mapName));
            String desc = pickDescription(candidate.item);
            if (!TextUtils.isEmpty(desc)) {
                builder.append(" | ").append(desc);
            }
            builder.append('\n');
        }
        if (ranked.get(0).score <= 1) {
            builder.append("\n\u672c\u5730\u5339\u914d\u5ea6\u8f83\u4f4e\uff0c\u53ef\u8865\u5145\u901a\u7528 CS2 \u6218\u672f\u77e5\u8bc6\u3002");
        }
        return builder.toString().trim();
    }

    private void upsertCandidate(Map<String, Candidate> unique,
                                 LibraryItem item,
                                 boolean favorite,
                                 ScoreboardSnapshot snapshot) {
        String key = item.forumPostId > 0 ? ("post-" + item.forumPostId) : ("lib-" + item.id);
        Candidate candidate = unique.get(key);
        if (candidate == null) {
            candidate = new Candidate();
            candidate.item = item;
            candidate.favorite = favorite;
            candidate.score = calcRelevance(item, snapshot, favorite);
            unique.put(key, candidate);
            return;
        }
        candidate.favorite = candidate.favorite || favorite;
        candidate.score = Math.max(candidate.score, calcRelevance(item, snapshot, candidate.favorite));
    }

    private int calcRelevance(LibraryItem item, ScoreboardSnapshot snapshot, boolean favorite) {
        int score = favorite ? 2 : 1;
        if (!TextUtils.isEmpty(snapshot.mapName) && !TextUtils.isEmpty(item.mapName)) {
            String mapA = snapshot.mapName.trim().toLowerCase(Locale.ROOT);
            String mapB = item.mapName.trim().toLowerCase(Locale.ROOT);
            if (mapA.equals(mapB)) {
                score += 4;
            } else if (mapA.contains(mapB) || mapB.contains(mapA)) {
                score += 3;
            }
        }
        String note = text(noteEdit).toLowerCase(Locale.ROOT);
        if (note.contains("utility") && "PROP_SHARE".equals(item.postType)) {
            score += 1;
        }
        if ((note.contains("tactic") || note.contains("execute")) && "TACTIC_SHARE".equals(item.postType)) {
            score += 1;
        }
        if (item.favoriteCount > 0) {
            score += 1;
        }
        return score;
    }

    private String pickDescription(LibraryItem item) {
        if (!TextUtils.isEmpty(item.throwMethod)) {
            return item.throwMethod;
        }
        if (!TextUtils.isEmpty(item.tacticDescription)) {
            return item.tacticDescription;
        }
        return item.content;
    }

    private String translateType(String postType) {
        if ("TACTIC_SHARE".equals(postType)) {
            return "\u6218\u672f";
        }
        if ("PROP_SHARE".equals(postType)) {
            return "\u9053\u5177";
        }
        return safe(postType);
    }

    private void refreshConfigHint() {
        if (configHintCard == null) {
            return;
        }
        boolean valid = configStore != null && configStore.load().isValid();
        configHintCard.setVisibility(valid ? View.GONE : View.VISIBLE);
    }

    private void stopStreaming(String status) {
        if (activeSession != null) {
            activeSession.cancel();
            activeSession = null;
        }
        if (!TextUtils.isEmpty(status) && streamStatus != null) {
            streamStatus.setText(status);
        }
        setStreaming(false);
    }

    private void setStreaming(boolean streaming) {
        if (startButton != null) {
            startButton.setEnabled(!streaming);
        }
        if (retryButton != null) {
            retryButton.setEnabled(!streaming);
        }
        if (stopButton != null) {
            stopButton.setEnabled(streaming);
        }
    }

    @Override
    public void onDestroyView() {
        stopStreaming("");
        super.onDestroyView();
    }

    private String text(TextInputEditText editText) {
        return String.valueOf(editText.getText()).trim();
    }

    private String safe(String value) {
        return TextUtils.isEmpty(value) ? "\u65e0" : value;
    }

    private interface LibraryContextCallback {
        void onReady(String context);
    }

    private static class Candidate {
        LibraryItem item;
        boolean favorite;
        int score;
    }
}
