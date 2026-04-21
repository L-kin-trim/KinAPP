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
import com.example.kin.model.ScoreboardSnapshot;
import com.example.kin.net.OpenAiStreamClient;
import com.example.kin.ui.common.BasePageFragment;
import com.example.kin.ui.common.KinUi;
import com.example.kin.util.ScoreboardParser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;

public class AiRecommendFragment extends BasePageFragment {
    private AiConfigStore configStore;
    private OpenAiStreamClient streamClient;
    private OpenAiStreamClient.StreamSession activeSession;

    private TextInputEditText baseUrlEdit;
    private TextInputEditText apiKeyEdit;
    private TextInputEditText modelEdit;
    private TextInputEditText systemPromptEdit;
    private TextInputEditText scoreEdit;
    private TextInputEditText moneyEdit;
    private TextInputEditText kdaEdit;
    private TextInputEditText noteEdit;

    private TextView imageState;
    private ImageView imagePreview;
    private TextView rawOcrView;
    private TextView streamStatus;
    private TextView outputView;
    private MaterialButton startButton;
    private MaterialButton stopButton;
    private MaterialButton retryButton;

    private Uri selectedImageUri;
    private Uri pendingCameraUri;

    private final ActivityResultLauncher<String> galleryPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri == null) {
                    return;
                }
                selectedImageUri = uri;
                renderSelectedImage(uri);
                setLoading(false, "已选择图片，点击“识别计分板”。");
            });

    private final ActivityResultLauncher<Uri> cameraCapture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (!success || pendingCameraUri == null) {
                    setLoading(false, "拍照已取消。");
                    return;
                }
                selectedImageUri = pendingCameraUri;
                renderSelectedImage(selectedImageUri);
                setLoading(false, "拍照成功，点击“识别计分板”。");
            });

    @Override
    protected void onPageReady() {
        MainActivity activity = (MainActivity) requireActivity();
        activity.setTopBar("AI 推荐", "");
        configStore = new AiConfigStore(activity);
        streamClient = new OpenAiStreamClient();
        contentLayout.addView(buildConfigCard(activity));
        contentLayout.addView(buildImageCard(activity));
        contentLayout.addView(buildOcrCard(activity));
        contentLayout.addView(buildOutputCard(activity));
        bindConfigToUi(configStore.load());
        setLoading(false, "");
    }

    private View buildConfigCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 20);
        body.addView(KinUi.text(activity, "AI 配置", 22, true));
        TextView subtitle = KinUi.muted(activity, "配置 OpenAI 兼容接口，API Key 使用本地加密存储。", 14);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);
        baseUrlEdit = addInput(body, "Base URL（例：https://api.openai.com）", false);
        apiKeyEdit = addInput(body, "API Key", false);
        modelEdit = addInput(body, "Model（例：gpt-4o-mini）", false);
        systemPromptEdit = addInput(body, "System Prompt（可选）", true);

        MaterialButton saveButton = KinUi.filledButton(activity, "保存 AI 配置");
        saveButton.setOnClickListener(v -> {
            AiConfig config = readConfigFromUi();
            if (!config.isValid()) {
                setLoading(false, "请先填写完整的 Base URL、API Key、Model。");
                return;
            }
            configStore.save(config);
            setLoading(false, "AI 配置已保存。");
        });
        KinUi.margins(saveButton, activity, 0, 14, 0, 0);
        body.addView(saveButton);
        card.addView(body);
        return card;
    }

    private View buildImageCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 20);
        body.addView(KinUi.text(activity, "计分板图片", 20, true));
        TextView subtitle = KinUi.muted(activity, "支持相册导入或拍照（拍摄电脑屏幕计分板）。", 14);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton pickButton = KinUi.filledButton(activity, "相册选图");
        pickButton.setOnClickListener(v -> galleryPicker.launch("image/*"));
        MaterialButton cameraButton = KinUi.outlinedButton(activity, "拍照");
        cameraButton.setOnClickListener(v -> launchCamera());
        actions.addView(pickButton);
        actions.addView(cameraButton);
        KinUi.margins(cameraButton, activity, 10, 0, 0, 0);
        KinUi.margins(actions, activity, 0, 14, 0, 0);
        body.addView(actions);

        imageState = KinUi.muted(activity, "尚未选择图片。", 13);
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

        MaterialButton ocrButton = KinUi.outlinedButton(activity, "识别计分板");
        ocrButton.setOnClickListener(v -> runOcr());
        KinUi.margins(ocrButton, activity, 0, 14, 0, 0);
        body.addView(ocrButton);
        card.addView(body);
        return card;
    }

    private View buildOcrCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 20);
        body.addView(KinUi.text(activity, "OCR 校对区", 20, true));
        TextView subtitle = KinUi.muted(activity, "先校对再发送给 AI，可显著提升建议准确度。", 14);
        KinUi.margins(subtitle, activity, 0, 8, 0, 0);
        body.addView(subtitle);

        scoreEdit = addInput(body, "比分（例：8:7）", false);
        moneyEdit = addInput(body, "经济（例：$4200, $3100, $1800）", false);
        kdaEdit = addInput(body, "战绩（例：16/10/3; 12/13/4）", false);
        noteEdit = addInput(body, "补充上下文（可选）", true);

        TextView rawTitle = KinUi.muted(activity, "OCR 原文：", 13);
        KinUi.margins(rawTitle, activity, 0, 12, 0, 0);
        body.addView(rawTitle);
        rawOcrView = KinUi.muted(activity, "", 13);
        rawOcrView.setVisibility(View.GONE);
        KinUi.margins(rawOcrView, activity, 0, 6, 0, 0);
        body.addView(rawOcrView);

        card.addView(body);
        return card;
    }

    private View buildOutputCard(MainActivity activity) {
        MaterialCardView card = KinUi.card(activity);
        LinearLayout body = KinUi.sectionContainer(activity, 20);
        body.addView(KinUi.text(activity, "AI 流式建议", 20, true));
        streamStatus = KinUi.muted(activity, "等待开始。", 13);
        KinUi.margins(streamStatus, activity, 0, 8, 0, 0);
        body.addView(streamStatus);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        startButton = KinUi.filledButton(activity, "开始分析");
        stopButton = KinUi.outlinedButton(activity, "停止");
        retryButton = KinUi.outlinedButton(activity, "重试");
        startButton.setOnClickListener(v -> startStreaming(false));
        retryButton.setOnClickListener(v -> startStreaming(true));
        stopButton.setOnClickListener(v -> stopStreaming("已停止。"));
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

    private void bindConfigToUi(AiConfig config) {
        baseUrlEdit.setText(config.baseUrl);
        apiKeyEdit.setText(config.apiKey);
        modelEdit.setText(config.model);
        systemPromptEdit.setText(config.systemPrompt);
    }

    private AiConfig readConfigFromUi() {
        AiConfig config = new AiConfig();
        config.baseUrl = text(baseUrlEdit);
        config.apiKey = text(apiKeyEdit);
        config.model = text(modelEdit);
        config.systemPrompt = text(systemPromptEdit);
        return config;
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
            setLoading(false, "拍照初始化失败：" + exception.getMessage());
        }
    }

    private void renderSelectedImage(Uri uri) {
        imageState.setText("已选择图片：" + uri.getLastPathSegment());
        imagePreview.setImageURI(uri);
        imagePreview.setVisibility(View.VISIBLE);
    }

    private void runOcr() {
        if (selectedImageUri == null) {
            setLoading(false, "请先选择一张计分板图片。");
            return;
        }
        setLoading(true, "正在识别计分板…");
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), selectedImageUri);
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener(visionText -> {
                        ScoreboardSnapshot snapshot = ScoreboardParser.parse(visionText.getText());
                        scoreEdit.setText(snapshot.scoreText);
                        moneyEdit.setText(snapshot.moneyText);
                        kdaEdit.setText(snapshot.kdaText);
                        rawOcrView.setText(snapshot.rawText);
                        rawOcrView.setVisibility(TextUtils.isEmpty(snapshot.rawText) ? View.GONE : View.VISIBLE);
                        if (TextUtils.isEmpty(snapshot.rawText)) {
                            setLoading(false, "OCR 未识别到文本，请重试更清晰截图。");
                        } else {
                            setLoading(false, "识别完成，请确认后点击“开始分析”。");
                        }
                    })
                    .addOnFailureListener(exception -> setLoading(false, "OCR 失败：" + exception.getMessage()));
        } catch (Exception exception) {
            setLoading(false, "OCR 处理失败：" + exception.getMessage());
        }
    }

    private void startStreaming(boolean retry) {
        AiConfig config = readConfigFromUi();
        if (!config.isValid()) {
            setLoading(false, "请先填写并保存完整 AI 配置。");
            return;
        }
        configStore.save(config);

        ScoreboardSnapshot snapshot = new ScoreboardSnapshot();
        snapshot.scoreText = text(scoreEdit);
        snapshot.moneyText = text(moneyEdit);
        snapshot.kdaText = text(kdaEdit);
        snapshot.rawText = rawOcrView.getVisibility() == View.GONE ? "" : String.valueOf(rawOcrView.getText());
        if (TextUtils.isEmpty(snapshot.scoreText)
                && TextUtils.isEmpty(snapshot.moneyText)
                && TextUtils.isEmpty(snapshot.kdaText)
                && TextUtils.isEmpty(snapshot.rawText)) {
            setLoading(false, "请先完成 OCR 或手动填写信息。");
            return;
        }

        stopStreaming("");
        if (!retry) {
            outputView.setText("");
        }
        streamStatus.setText("正在流式分析…");
        setStreaming(true);

        activeSession = streamClient.streamScoreboardAdvice(config, snapshot, text(noteEdit),
                new OpenAiStreamClient.StreamListener() {
                    @Override
                    public void onStart() {
                        streamStatus.setText("AI 已连接，正在输出建议…");
                    }

                    @Override
                    public void onDelta(String content) {
                        outputView.append(content);
                    }

                    @Override
                    public void onComplete() {
                        setStreaming(false);
                        streamStatus.setText("分析完成。");
                    }

                    @Override
                    public void onError(String message) {
                        setStreaming(false);
                        streamStatus.setText("分析失败：" + message);
                    }
                });
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
}
