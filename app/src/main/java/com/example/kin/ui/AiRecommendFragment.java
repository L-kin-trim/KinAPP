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
                setLoading(false, "Image selected. Tap OCR to extract scoreboard.");
            });

    private final ActivityResultLauncher<Uri> cameraCapture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (!success || pendingCameraUri == null) {
                    setLoading(false, "Camera canceled.");
                    return;
                }
                selectedImageUri = pendingCameraUri;
                renderSelectedImage(selectedImageUri);
                setLoading(false, "Photo captured. Tap OCR to extract scoreboard.");
            });

    @Override
    protected void onPageReady() {
        MainActivity activity = (MainActivity) requireActivity();
        activity.setTopBar("AI", "");
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
        body.addView(KinUi.text(activity, "Configure AI model to enable recommendation", 20, true));
        TextView hint = KinUi.muted(activity,
                "No model configured yet. Open top-right gear or Me > AI Model Settings.",
                14);
        KinUi.margins(hint, activity, 0, 8, 0, 0);
        body.addView(hint);

        MaterialButton configure = KinUi.filledButton(activity, "Open AI Settings");
        configure.setOnClickListener(v -> activity.openAiSettings());
        KinUi.margins(configure, activity, 0, 12, 0, 0);
        body.addView(configure);
        card.addView(body);
        return card;
    }

    private View buildImageCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 20);
        body.addView(KinUi.text(activity, "Scoreboard image", 20, true));
        TextView subtitle = KinUi.muted(activity, "Import from gallery or camera. Local OCR extracts map/score/player/money.", 14);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton pickButton = KinUi.filledButton(activity, "Pick Image");
        pickButton.setOnClickListener(v -> galleryPicker.launch("image/*"));
        MaterialButton cameraButton = KinUi.outlinedButton(activity, "Camera");
        cameraButton.setOnClickListener(v -> launchCamera());
        actions.addView(pickButton);
        actions.addView(cameraButton);
        KinUi.margins(cameraButton, activity, 10, 0, 0, 0);
        KinUi.margins(actions, activity, 0, 14, 0, 0);
        body.addView(actions);

        imageState = KinUi.muted(activity, "No image selected.", 13);
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

        MaterialButton ocrButton = KinUi.outlinedButton(activity, "Run OCR");
        ocrButton.setOnClickListener(v -> runOcr());
        KinUi.margins(ocrButton, activity, 0, 14, 0, 0);
        body.addView(ocrButton);
        card.addView(body);
        return card;
    }

    private View buildOcrCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 20);
        body.addView(KinUi.text(activity, "Structured OCR", 20, true));
        TextView subtitle = KinUi.muted(activity, "Review and adjust OCR values before sending to AI.", 14);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);

        mapEdit = addInput(body, "Map (e.g. Dust II)", false);
        scoreEdit = addInput(body, "Score (e.g. 9:2)", false);
        moneyEdit = addInput(body, "Money (e.g. $5650, $1200)", false);
        kdaEdit = addInput(body, "K/D/A summary", false);
        noteEdit = addInput(body, "Extra context (optional)", true);

        TextView structuredTitle = KinUi.muted(activity, "Structured summary", 13);
        KinUi.margins(structuredTitle, activity, 0, 12, 0, 0);
        body.addView(structuredTitle);

        structuredView = KinUi.muted(activity, "", 13);
        structuredView.setVisibility(View.GONE);
        KinUi.margins(structuredView, activity, 0, 6, 0, 0);
        body.addView(structuredView);

        TextView rawTitle = KinUi.muted(activity, "Raw OCR text", 13);
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
        body.addView(KinUi.text(activity, "AI Recommendation", 20, true));
        streamStatus = KinUi.muted(activity, "Idle", 13);
        KinUi.margins(streamStatus, activity, 0, 8, 0, 0);
        body.addView(streamStatus);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        startButton = KinUi.filledButton(activity, "Start");
        stopButton = KinUi.outlinedButton(activity, "Stop");
        retryButton = KinUi.outlinedButton(activity, "Retry");
        startButton.setOnClickListener(v -> startStreaming(false));
        retryButton.setOnClickListener(v -> startStreaming(true));
        stopButton.setOnClickListener(v -> stopStreaming("Stopped"));
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
            setLoading(false, "Camera init failed: " + exception.getMessage());
        }
    }

    private void renderSelectedImage(Uri uri) {
        imageState.setText("Selected: " + uri.getLastPathSegment());
        imagePreview.setImageURI(uri);
        imagePreview.setVisibility(View.VISIBLE);
    }

    private void runOcr() {
        if (selectedImageUri == null) {
            setLoading(false, "Please select an image first.");
            return;
        }
        setLoading(true, "Running local OCR (Chinese + Latin)...");
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
                setLoading(false, "OCR done. Please verify then run AI analysis.");
            }

            @Override
            public void onError(String message) {
                setLoading(false, "OCR failed: " + message);
            }
        });
    }

    private void startStreaming(boolean retry) {
        AiConfig config = configStore.load();
        if (!config.isValid()) {
            refreshConfigHint();
            setLoading(false, "Please configure AI provider first.");
            return;
        }

        ScoreboardSnapshot snapshot = buildSnapshotFromUi();
        if (!snapshot.hasCoreStats()) {
            setLoading(false, "Please run OCR or fill key scoreboard fields.");
            return;
        }

        stopStreaming("");
        if (!retry) {
            outputView.setText("");
        }
        streamStatus.setText("Loading local library context...");
        setStreaming(true);

        loadLibraryContext(snapshot, context -> {
            if (!isAdded()) {
                setStreaming(false);
                return;
            }
            streamStatus.setText("Streaming AI recommendation...");
            activeSession = streamClient.streamScoreboardAdvice(
                    config,
                    snapshot,
                    text(noteEdit),
                    context,
                    new OpenAiStreamClient.StreamListener() {
                        @Override
                        public void onStart() {
                            streamStatus.setText("AI connected, generating...");
                        }

                        @Override
                        public void onDelta(String content) {
                            outputView.append(content);
                        }

                        @Override
                        public void onComplete() {
                            setStreaming(false);
                            streamStatus.setText("Analysis complete.");
                        }

                        @Override
                        public void onError(String message) {
                            setStreaming(false);
                            streamStatus.setText("Analysis failed: " + message);
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
            builder.append("Map: ").append(snapshot.mapName).append('\n');
        }
        if (!TextUtils.isEmpty(snapshot.scoreText)) {
            builder.append("Score: ").append(snapshot.scoreText).append('\n');
        }
        if (!TextUtils.isEmpty(snapshot.playerStatsText)) {
            builder.append("Players:\n").append(snapshot.playerStatsText).append('\n');
        }
        if (!TextUtils.isEmpty(snapshot.hotHandSummary)) {
            builder.append("Hot Hand: ").append(snapshot.hotHandSummary).append('\n');
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
            return "No local records found. Use general CS2 knowledge to provide recommendations.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Prioritize these local records first:\n");
        int limit = Math.min(ranked.size(), 6);
        for (int i = 0; i < limit; i++) {
            Candidate candidate = ranked.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(candidate.favorite ? "[Favorite] " : "[Library] ")
                    .append(safe(candidate.item.title))
                    .append(" | ")
                    .append(translateType(candidate.item.postType))
                    .append(" | map=")
                    .append(safe(candidate.item.mapName));
            String desc = pickDescription(candidate.item);
            if (!TextUtils.isEmpty(desc)) {
                builder.append(" | ").append(desc);
            }
            builder.append('\n');
        }
        if (ranked.get(0).score <= 1) {
            builder.append("\nLocal matches are weak; supplement with general CS2 tactical knowledge.");
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
            return "Tactic";
        }
        if ("PROP_SHARE".equals(postType)) {
            return "Utility";
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
        return TextUtils.isEmpty(value) ? "N/A" : value;
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
