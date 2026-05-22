package com.example.kin.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
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
import java.util.List;

public class ScoreboardOcrOrchestrator {
    private static final CropSpec[] SCOREBOARD_CROPS = new CropSpec[]{
            new CropSpec("top-hud", 0.32f, 0.00f, 0.70f, 0.18f, 3),
            new CropSpec("scoreboard", 0.25f, 0.25f, 0.76f, 0.82f, 2),
            new CropSpec("team-a-rows", 0.31f, 0.34f, 0.72f, 0.52f, 3),
            new CropSpec("team-b-rows", 0.31f, 0.58f, 0.72f, 0.76f, 3),
            new CropSpec("player-names", 0.35f, 0.34f, 0.55f, 0.76f, 3),
            new CropSpec("stat-columns", 0.54f, 0.34f, 0.72f, 0.76f, 3),
            new CropSpec("left-score", 0.25f, 0.37f, 0.34f, 0.75f, 3),
            new CropSpec("mini-map", 0.00f, 0.00f, 0.19f, 0.30f, 2)
    };

    public interface Callback {
        void onSuccess(ScoreboardSnapshot snapshot);

        void onError(String message);
    }

    public void recognize(Context context, Uri imageUri, Callback callback) {
        TextRecognizer latinRecognizer = null;
        TextRecognizer chineseRecognizer = null;
        try {
            Bitmap sourceBitmap = loadBitmap(context, imageUri);
            latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            chineseRecognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());

            List<Bitmap> ownedBitmaps = new ArrayList<>();
            List<PendingRecognition> pending = new ArrayList<>();
            addRecognitionTasks(sourceBitmap, latinRecognizer, chineseRecognizer, ownedBitmaps, pending);

            TextRecognizer finalLatinRecognizer = latinRecognizer;
            TextRecognizer finalChineseRecognizer = chineseRecognizer;
            Tasks.whenAllComplete(tasksFrom(pending))
                    .addOnCompleteListener(task -> {
                        String latinText = collectText(pending, "latin", false);
                        String chineseText = collectText(pending, "chinese", false);
                        String plainText = mergeTexts(latinText, chineseText);
                        if (TextUtils.isEmpty(plainText)) {
                            callback.onError("OCR did not find readable scoreboard text.");
                        } else {
                            ScoreboardSnapshot snapshot = ScoreboardParser.parse(plainText);
                            snapshot.latinRawText = collectText(pending, "latin", true);
                            snapshot.chineseRawText = collectText(pending, "chinese", true);
                            snapshot.rawText = mergeTexts(snapshot.latinRawText, snapshot.chineseRawText);
                            callback.onSuccess(snapshot);
                        }
                        recycleAll(ownedBitmaps);
                        finalLatinRecognizer.close();
                        finalChineseRecognizer.close();
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

    private void addRecognitionTasks(Bitmap source,
                                     TextRecognizer latinRecognizer,
                                     TextRecognizer chineseRecognizer,
                                     List<Bitmap> ownedBitmaps,
                                     List<PendingRecognition> pending) {
        Bitmap fullImage = prepareForOcr(source, 1);
        ownedBitmaps.add(source);
        ownedBitmaps.add(fullImage);
        addSectionTasks("full", fullImage, latinRecognizer, chineseRecognizer, pending);

        for (CropSpec cropSpec : SCOREBOARD_CROPS) {
            Bitmap cropped = crop(source, cropSpec);
            Bitmap enhanced = prepareForOcr(cropped, cropSpec.scale);
            Bitmap binary = binarizeLightText(cropped, cropSpec.scale);
            ownedBitmaps.add(cropped);
            ownedBitmaps.add(enhanced);
            ownedBitmaps.add(binary);
            addSectionTasks(cropSpec.label, enhanced, latinRecognizer, chineseRecognizer, pending);
            addSectionTasks(cropSpec.label + "-binary", binary, latinRecognizer, chineseRecognizer, pending);
        }
    }

    private void addSectionTasks(String section,
                                 Bitmap bitmap,
                                 TextRecognizer latinRecognizer,
                                 TextRecognizer chineseRecognizer,
                                 List<PendingRecognition> pending) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        pending.add(new PendingRecognition(section, "latin", latinRecognizer.process(image)));
        pending.add(new PendingRecognition(section, "chinese", chineseRecognizer.process(image)));
    }

    private List<Task<?>> tasksFrom(List<PendingRecognition> pending) {
        List<Task<?>> tasks = new ArrayList<>();
        for (PendingRecognition recognition : pending) {
            tasks.add(recognition.task);
        }
        return tasks;
    }

    private String collectText(List<PendingRecognition> pending, String language, boolean labeled) {
        StringBuilder builder = new StringBuilder();
        for (PendingRecognition recognition : pending) {
            if (!language.equals(recognition.language)) {
                continue;
            }
            String text = readTaskText(recognition.task);
            if (TextUtils.isEmpty(text)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            if (labeled) {
                builder.append('[').append(recognition.section).append("]\n");
            }
            builder.append(text);
        }
        return builder.toString().trim();
    }

    private Bitmap crop(Bitmap source, CropSpec spec) {
        int width = source.getWidth();
        int height = source.getHeight();
        int left = clamp(Math.round(width * spec.left), 0, width - 2);
        int top = clamp(Math.round(height * spec.top), 0, height - 2);
        int right = clamp(Math.round(width * spec.right), left + 1, width);
        int bottom = clamp(Math.round(height * spec.bottom), top + 1, height);
        return Bitmap.createBitmap(source, left, top, right - left, bottom - top);
    }

    private Bitmap prepareForOcr(Bitmap source, int scale) {
        int safeScale = Math.max(1, scale);
        Bitmap scaled = safeScale == 1
                ? source.copy(Bitmap.Config.ARGB_8888, false)
                : Bitmap.createScaledBitmap(source, source.getWidth() * safeScale, source.getHeight() * safeScale, false);
        Bitmap output = Bitmap.createBitmap(scaled.getWidth(), scaled.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        ColorMatrix grayscale = new ColorMatrix();
        grayscale.setSaturation(0f);
        ColorMatrix contrast = new ColorMatrix(new float[]{
                1.45f, 0f, 0f, 0f, -28f,
                0f, 1.45f, 0f, 0f, -28f,
                0f, 0f, 1.45f, 0f, -28f,
                0f, 0f, 0f, 1f, 0f
        });
        contrast.postConcat(grayscale);
        paint.setColorFilter(new ColorMatrixColorFilter(contrast));
        canvas.drawBitmap(scaled, 0f, 0f, paint);
        if (scaled != source) {
            scaled.recycle();
        }
        return output;
    }

    private Bitmap binarizeLightText(Bitmap source, int scale) {
        int safeScale = Math.max(2, scale);
        Bitmap scaled = Bitmap.createScaledBitmap(source, source.getWidth() * safeScale, source.getHeight() * safeScale, false);
        Bitmap output = Bitmap.createBitmap(scaled.getWidth(), scaled.getHeight(), Bitmap.Config.ARGB_8888);
        int width = scaled.getWidth();
        int height = scaled.getHeight();
        int[] pixels = new int[width * height];
        scaled.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];
            int red = (color >> 16) & 0xff;
            int green = (color >> 8) & 0xff;
            int blue = color & 0xff;
            int max = Math.max(red, Math.max(green, blue));
            int min = Math.min(red, Math.min(green, blue));
            int luminance = (red * 299 + green * 587 + blue * 114) / 1000;
            boolean brightText = luminance >= 148 && max - min <= 95;
            boolean yellowText = red >= 150 && green >= 120 && blue <= 105;
            boolean blueWhiteText = blue >= 135 && red >= 105 && green >= 115;
            pixels[i] = (brightText || yellowText || blueWhiteText) ? 0xff000000 : 0xffffffff;
        }
        output.setPixels(pixels, 0, width, 0, 0, width, height);
        scaled.recycle();
        return output;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void recycleAll(List<Bitmap> bitmaps) {
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    private String readTaskText(Task<Text> task) {
        if (task == null || !task.isSuccessful() || task.getResult() == null) {
            return "";
        }
        String text = task.getResult().getText();
        return text == null ? "" : text.trim();
    }

    private String mergeTexts(String latinText, String chineseText) {
        if (TextUtils.isEmpty(latinText)) {
            return empty(chineseText);
        }
        if (TextUtils.isEmpty(chineseText)) {
            return empty(latinText);
        }
        if (latinText.equals(chineseText)) {
            return latinText;
        }
        return latinText + "\n\n-----\n\n" + chineseText;
    }

    private String empty(String value) {
        return value == null ? "" : value;
    }

    private static class CropSpec {
        final String label;
        final float left;
        final float top;
        final float right;
        final float bottom;
        final int scale;

        CropSpec(String label, float left, float top, float right, float bottom, int scale) {
            this.label = label;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.scale = scale;
        }
    }

    private static class PendingRecognition {
        final String section;
        final String language;
        final Task<Text> task;

        PendingRecognition(String section, String language, Task<Text> task) {
            this.section = section;
            this.language = language;
            this.task = task;
        }
    }
}
