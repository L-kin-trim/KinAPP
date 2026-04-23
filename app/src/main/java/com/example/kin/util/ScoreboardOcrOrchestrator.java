package com.example.kin.util;

import android.content.Context;
import android.net.Uri;
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

public class ScoreboardOcrOrchestrator {
    public interface Callback {
        void onSuccess(ScoreboardSnapshot snapshot);

        void onError(String message);
    }

    public void recognize(Context context, Uri imageUri, Callback callback) {
        try {
            InputImage image = InputImage.fromFilePath(context, imageUri);
            TextRecognizer latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            TextRecognizer chineseRecognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());

            Task<Text> latinTask = latinRecognizer.process(image);
            Task<Text> chineseTask = chineseRecognizer.process(image);

            Tasks.whenAllComplete(latinTask, chineseTask)
                    .addOnSuccessListener(tasks -> {
                        String latinText = readTaskText(latinTask);
                        String chineseText = readTaskText(chineseTask);
                        String merged = mergeTexts(latinText, chineseText);
                        if (TextUtils.isEmpty(merged)) {
                            callback.onError("OCR did not detect readable text.");
                        } else {
                            ScoreboardSnapshot snapshot = ScoreboardParser.parse(merged);
                            snapshot.latinRawText = latinText;
                            snapshot.chineseRawText = chineseText;
                            callback.onSuccess(snapshot);
                        }
                        latinRecognizer.close();
                        chineseRecognizer.close();
                    })
                    .addOnFailureListener(exception -> {
                        latinRecognizer.close();
                        chineseRecognizer.close();
                        callback.onError(exception.getMessage() == null ? "OCR failed." : exception.getMessage());
                    });
        } catch (Exception exception) {
            callback.onError(exception.getMessage() == null ? "OCR prepare failed." : exception.getMessage());
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
}
