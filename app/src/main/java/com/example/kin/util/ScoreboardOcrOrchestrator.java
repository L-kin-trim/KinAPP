package com.example.kin.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.example.kin.model.ScoreboardSnapshot;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Layout-agnostic scoreboard OCR. Instead of cropping fixed fractions of a
 * fixed 16:9 layout, this reconstructs the scoreboard table from ML Kit element
 * bounding boxes: elements are clustered into rows by vertical position and
 * sorted left-to-right inside each row, so the true reading order of every row
 * is recovered regardless of where the panel sits in the screenshot. The two
 * big team-score digits are detected by font height. The result is emitted as
 * the labelled section text that {@link ScoreboardParser} already parses.
 */
public class ScoreboardOcrOrchestrator {

    public interface Callback {
        void onSuccess(ScoreboardSnapshot snapshot);

        void onError(String message);
    }

    public void recognize(Context context, Uri imageUri, Callback callback) {
        TextRecognizer latinRecognizer = null;
        TextRecognizer chineseRecognizer = null;
        try {
            Bitmap source = loadBitmap(context, imageUri);
            Bitmap ocrBitmap = maybeUpscale(source);
            int width = ocrBitmap.getWidth();

            latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            chineseRecognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());

            InputImage image = InputImage.fromBitmap(ocrBitmap, 0);
            Task<Text> chineseTask = chineseRecognizer.process(image);
            Task<Text> latinTask = latinRecognizer.process(image);

            TextRecognizer finalLatin = latinRecognizer;
            TextRecognizer finalChinese = chineseRecognizer;
            Tasks.whenAllComplete(chineseTask, latinTask).addOnCompleteListener(task -> {
                try {
                    Text chineseResult = resultOf(chineseTask);
                    Text latinResult = resultOf(latinTask);

                    List<Element> chineseElements = collectElements(chineseResult);
                    List<Element> latinElements = collectElements(latinResult);
                    if (chineseElements.isEmpty() && latinElements.isEmpty()) {
                        callback.onError("OCR did not find readable scoreboard text.");
                        return;
                    }

                    String labeled = buildLabeledText(chineseResult, latinResult,
                            chineseElements, latinElements, width);
                    ScoreboardSnapshot snapshot = ScoreboardParser.parse(labeled);
                    snapshot.chineseRawText = textOf(chineseResult);
                    snapshot.latinRawText = textOf(latinResult);
                    snapshot.rawText = buildReadableRaw(snapshot, reconstructRows(chineseElements));
                    callback.onSuccess(snapshot);
                } finally {
                    if (ocrBitmap != source && !ocrBitmap.isRecycled()) {
                        ocrBitmap.recycle();
                    }
                    if (!source.isRecycled()) {
                        source.recycle();
                    }
                    finalLatin.close();
                    finalChinese.close();
                }
            });
        } catch (Exception exception) {
            if (latinRecognizer != null) {
                latinRecognizer.close();
            }
            if (chineseRecognizer != null) {
                chineseRecognizer.close();
            }
            callback.onError(exception.getMessage() == null ? "OCR initialization failed." : exception.getMessage());
        }
    }

    private Bitmap loadBitmap(Context context, Uri imageUri) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), imageUri);
            return ImageDecoder.decodeBitmap(source, (decoder, info, src) ->
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
        }
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
    }

    // Small screenshots (phone-captured) carry tiny scoreboard text; upscale so
    // ML Kit reads the digits more reliably. Large captures are used as-is.
    private Bitmap maybeUpscale(Bitmap source) {
        int width = source.getWidth();
        if (width >= 1600) {
            return source;
        }
        return Bitmap.createScaledBitmap(source, width * 2, source.getHeight() * 2, true);
    }

    private List<Element> collectElements(Text text) {
        List<Element> elements = new ArrayList<>();
        if (text == null) {
            return elements;
        }
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                for (Text.Element element : line.getElements()) {
                    Rect box = element.getBoundingBox();
                    String value = element.getText();
                    if (box == null || value == null) {
                        continue;
                    }
                    String trimmed = value.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    elements.add(new Element(trimmed, box.centerX(), box.centerY(), box.height()));
                }
            }
        }
        return elements;
    }

    /**
     * Clusters elements into rows by vertical proximity, then orders each row
     * left-to-right and joins the element texts with spaces.
     */
    private List<String> reconstructRows(List<Element> elements) {
        List<String> rows = new ArrayList<>();
        if (elements.isEmpty()) {
            return rows;
        }
        List<Element> sorted = new ArrayList<>(elements);
        sorted.sort((a, b) -> Integer.compare(a.centerY, b.centerY));
        int medianHeight = medianHeight(sorted);
        int tolerance = Math.max(8, Math.round(medianHeight * 0.7f));

        List<Element> current = new ArrayList<>();
        int lastCenterY = Integer.MIN_VALUE;
        for (Element element : sorted) {
            if (current.isEmpty() || element.centerY - lastCenterY <= tolerance) {
                current.add(element);
            } else {
                rows.add(joinRow(current));
                current = new ArrayList<>();
                current.add(element);
            }
            lastCenterY = element.centerY;
        }
        if (!current.isEmpty()) {
            rows.add(joinRow(current));
        }
        return rows;
    }

    private String joinRow(List<Element> row) {
        row.sort((a, b) -> Integer.compare(a.centerX, b.centerX));
        StringBuilder builder = new StringBuilder();
        for (Element element : row) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(element.text);
        }
        return builder.toString();
    }

    /**
     * The two team scores are by far the tallest digits on the panel and sit on
     * its left edge. Returns {top, bottom} or null when not confidently found.
     */
    private String[] detectScore(List<Element> elements, int width) {
        if (elements.isEmpty()) {
            return null;
        }
        int medianHeight = medianHeight(elements);
        List<Element> candidates = new ArrayList<>();
        for (Element element : elements) {
            if (!element.text.matches("\\d{1,2}")) {
                continue;
            }
            int value = Integer.parseInt(element.text);
            if (value > 30) {
                continue;
            }
            if (element.height >= medianHeight * 1.6f && element.centerX < width * 0.45f) {
                candidates.add(element);
            }
        }
        if (candidates.size() < 2) {
            return null;
        }
        candidates.sort((a, b) -> Integer.compare(a.centerY, b.centerY));
        return new String[]{candidates.get(0).text, candidates.get(1).text};
    }

    private int medianHeight(List<Element> elements) {
        List<Integer> heights = new ArrayList<>();
        for (Element element : elements) {
            heights.add(element.height);
        }
        Collections.sort(heights);
        if (heights.isEmpty()) {
            return 1;
        }
        return Math.max(1, heights.get(heights.size() / 2));
    }

    private String buildLabeledText(Text chineseResult,
                                    Text latinResult,
                                    List<Element> chineseElements,
                                    List<Element> latinElements,
                                    int width) {
        List<String> chineseRows = reconstructRows(chineseElements);
        List<String> latinRows = reconstructRows(latinElements);
        String[] score = detectScore(chineseElements, width);
        if (score == null) {
            score = detectScore(latinElements, width);
        }

        StringBuilder builder = new StringBuilder();
        // Full Chinese text feeds map-name detection (which scans the whole input).
        builder.append("[ocr-full]\n").append(textOf(chineseResult)).append("\n\n");
        if (score != null) {
            builder.append("[score-a]\n").append(score[0]).append("\n\n");
            builder.append("[score-b]\n").append(score[1]).append("\n\n");
        }
        builder.append("[team-a-rows]\n").append(TextUtils.join("\n", chineseRows)).append("\n\n");
        builder.append("[team-a-rows-binary]\n").append(TextUtils.join("\n", latinRows));
        return builder.toString();
    }

    private String buildReadableRaw(ScoreboardSnapshot snapshot, List<String> chineseRows) {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(snapshot.mapName)) {
            builder.append("地图 ").append(snapshot.mapName).append('\n');
        }
        if (!TextUtils.isEmpty(snapshot.scoreText)) {
            builder.append("比分 ").append(snapshot.scoreText).append('\n');
        }
        for (String row : chineseRows) {
            builder.append(row).append('\n');
        }
        return builder.toString().trim();
    }

    private Text resultOf(Task<Text> task) {
        if (task == null || !task.isSuccessful()) {
            return null;
        }
        return task.getResult();
    }

    private String textOf(Text text) {
        if (text == null || text.getText() == null) {
            return "";
        }
        return text.getText().trim();
    }

    private static final class Element {
        final String text;
        final int centerX;
        final int centerY;
        final int height;

        Element(String text, int centerX, int centerY, int height) {
            this.text = text;
            this.centerX = centerX;
            this.centerY = centerY;
            this.height = height;
        }
    }
}
