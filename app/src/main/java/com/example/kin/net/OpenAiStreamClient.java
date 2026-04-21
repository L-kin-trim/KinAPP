package com.example.kin.net;

import com.example.kin.model.AiConfig;
import com.example.kin.model.ScoreboardSnapshot;
import com.example.kin.util.AppExecutors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OpenAiStreamClient {
    public interface StreamListener {
        void onStart();

        void onDelta(String content);

        void onComplete();

        void onError(String message);
    }

    public static class StreamSession {
        private volatile boolean canceled;
        private volatile HttpURLConnection connection;

        public void cancel() {
            canceled = true;
            HttpURLConnection conn = connection;
            if (conn != null) {
                conn.disconnect();
            }
        }

        private boolean isCanceled() {
            return canceled;
        }

        private void bind(HttpURLConnection connection) {
            this.connection = connection;
        }
    }

    public StreamSession streamScoreboardAdvice(AiConfig config,
                                                ScoreboardSnapshot snapshot,
                                                String note,
                                                StreamListener listener) {
        StreamSession session = new StreamSession();
        AppExecutors.main(listener::onStart);
        AppExecutors.io().execute(() -> runStream(session, config, snapshot, note, listener));
        return session;
    }

    private void runStream(StreamSession session,
                           AiConfig config,
                           ScoreboardSnapshot snapshot,
                           String note,
                           StreamListener listener) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(buildChatUrl(config.baseUrl)).openConnection();
            session.bind(connection);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Authorization", "Bearer " + config.apiKey);

            byte[] payload = buildPayload(config, snapshot, note).toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(payload);
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            if (status < 200 || status >= 300) {
                String error = readText(stream);
                postError(listener, isEmpty(error) ? "AI 请求失败：" + status : error);
                return;
            }
            if (stream == null) {
                postError(listener, "AI 响应为空。");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while (!session.isCanceled() && (line = reader.readLine()) != null) {
                    String delta = extractDeltaContent(line);
                    if (!isEmpty(delta)) {
                        AppExecutors.main(() -> listener.onDelta(delta));
                    }
                    if ("data: [DONE]".equals(line.trim())) {
                        break;
                    }
                }
            }
            if (!session.isCanceled()) {
                AppExecutors.main(listener::onComplete);
            }
        } catch (Exception exception) {
            if (!session.isCanceled()) {
                postError(listener, exception.getMessage() == null ? "AI 流式请求失败。" : exception.getMessage());
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private JSONObject buildPayload(AiConfig config, ScoreboardSnapshot snapshot, String note) throws Exception {
        JSONObject root = new JSONObject();
        root.put("model", config.model);
        root.put("stream", true);

        JSONArray messages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", isEmpty(config.systemPrompt)
                ? "你是 CS2 助手。结合比分、经济和战绩，给出起枪建议、站位和战术执行建议，输出简明、可执行。"
                : config.systemPrompt);
        messages.put(system);

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", buildUserPrompt(snapshot, note));
        messages.put(user);

        root.put("messages", messages);
        return root;
    }

    private String buildUserPrompt(ScoreboardSnapshot snapshot, String note) {
        StringBuilder builder = new StringBuilder();
        builder.append("请基于当前计分板信息给出下一回合建议。\n");
        builder.append("比分：").append(empty(snapshot.scoreText)).append('\n');
        builder.append("经济：").append(empty(snapshot.moneyText)).append('\n');
        builder.append("战绩：").append(empty(snapshot.kdaText)).append('\n');
        builder.append("补充信息：").append(isEmpty(note) ? "无" : note).append('\n');
        builder.append("OCR原文：\n").append(empty(snapshot.rawText)).append('\n');
        builder.append("请按以下结构输出：\n");
        builder.append("1) 推荐起枪与道具\n");
        builder.append("2) 进攻/防守战术建议\n");
        builder.append("3) 风险提示与备选方案");
        return builder.toString();
    }

    private String buildChatUrl(String baseUrl) {
        String normalized = (baseUrl == null ? "" : baseUrl.trim());
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized + "/v1/chat/completions";
    }

    public static String extractDeltaContent(String line) {
        if (isEmpty(line)) {
            return "";
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("data:")) {
            return "";
        }
        String data = trimmed.substring(5).trim();
        if (isEmpty(data) || "[DONE]".equals(data)) {
            return "";
        }
        try {
            JSONObject json = new JSONObject(data);
            JSONArray choices = json.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return "";
            }
            JSONObject first = choices.optJSONObject(0);
            if (first == null) {
                return "";
            }
            JSONObject delta = first.optJSONObject("delta");
            if (delta == null) {
                return "";
            }
            return delta.optString("content", "");
        } catch (Exception ignored) {
            return "";
        }
    }

    private String readText(InputStream stream) {
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString().trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void postError(StreamListener listener, String message) {
        AppExecutors.main(() -> listener.onError(isEmpty(message) ? "AI 请求失败。" : message));
    }

    private String empty(String value) {
        return isEmpty(value) ? "未识别" : value;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
