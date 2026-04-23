package com.example.kin.net;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.example.kin.data.SessionManager;
import com.example.kin.util.AppExecutors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiClient {
    private final Context appContext;
    private final SessionManager sessionManager;

    public ApiClient(Context context) {
        this.appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);
    }

    public void get(String path, Map<String, String> query, boolean authRequired, ApiCallback<JSONObject> callback) {
        executeJson("GET", path, query, null, authRequired, callback);
    }

    public void postJson(String path, JSONObject body, boolean authRequired, ApiCallback<JSONObject> callback) {
        executeJson("POST", path, null, body, authRequired, callback);
    }

    public void postJson(String path, Map<String, String> query, JSONObject body, boolean authRequired, ApiCallback<JSONObject> callback) {
        executeJson("POST", path, query, body, authRequired, callback);
    }

    public void putJson(String path, JSONObject body, boolean authRequired, ApiCallback<JSONObject> callback) {
        executeJson("PUT", path, null, body, authRequired, callback);
    }

    public void patchJson(String path, JSONObject body, boolean authRequired, ApiCallback<JSONObject> callback) {
        executeJson("PATCH", path, null, body, authRequired, callback);
    }

    public void deleteJson(String path, JSONObject body, boolean authRequired, ApiCallback<JSONObject> callback) {
        executeJson("DELETE", path, null, body, authRequired, callback);
    }

    public void deleteJson(String path, Map<String, String> query, JSONObject body, boolean authRequired, ApiCallback<JSONObject> callback) {
        executeJson("DELETE", path, query, body, authRequired, callback);
    }

    public void getText(String path, Map<String, String> query, boolean authRequired, ApiCallback<String> callback) {
        executeText("GET", path, query, null, authRequired, callback);
    }

    public void postMultipart(String path,
                              Map<String, String> fields,
                              List<MultipartPart> parts,
                              boolean authRequired,
                              ApiCallback<JSONObject> callback) {
        AppExecutors.io().execute(() -> {
            try {
                String boundary = "KinBoundary" + System.currentTimeMillis();
                HttpURLConnection connection = openConnection("POST", buildUrl(path, null), authRequired);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                    if (fields != null) {
                        for (Map.Entry<String, String> entry : fields.entrySet()) {
                            writeField(out, boundary, entry.getKey(), entry.getValue());
                        }
                    }
                    for (MultipartPart part : parts) {
                        writePart(out, boundary, part);
                    }
                    out.writeBytes("--" + boundary + "--\r\n");
                }
                handleResponse(connection, callback);
            } catch (Exception exception) {
                postError(callback, new ApiException(-1, safeMessage(exception)));
            }
        });
    }

    public List<MultipartPart> createParts(Context context, String fieldName, List<Uri> uris) throws IOException {
        List<MultipartPart> parts = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        for (Uri uri : uris) {
            String mimeType = resolver.getType(uri);
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            String fileName = "upload_" + System.currentTimeMillis() + (TextUtils.isEmpty(extension) ? ".jpg" : "." + extension);
            byte[] bytes = readBytes(resolver.openInputStream(uri));
            parts.add(new MultipartPart(fieldName, fileName, mimeType == null ? "application/octet-stream" : mimeType, bytes));
        }
        return parts;
    }

    private void executeJson(String method,
                             String path,
                             Map<String, String> query,
                             JSONObject body,
                             boolean authRequired,
                             ApiCallback<JSONObject> callback) {
        AppExecutors.io().execute(() -> {
            try {
                HttpURLConnection connection = openConnection(method, buildUrl(path, query), authRequired);
                if (body != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                    connection.setFixedLengthStreamingMode(payload.length);
                    try (OutputStream outputStream = connection.getOutputStream()) {
                        outputStream.write(payload);
                    }
                }
                handleResponse(connection, callback);
            } catch (Exception exception) {
                postError(callback, new ApiException(-1, safeMessage(exception)));
            }
        });
    }

    private void executeText(String method,
                             String path,
                             Map<String, String> query,
                             JSONObject body,
                             boolean authRequired,
                             ApiCallback<String> callback) {
        AppExecutors.io().execute(() -> {
            try {
                HttpURLConnection connection = openConnection(method, buildUrl(path, query), authRequired);
                if (body != null) {
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                    connection.setFixedLengthStreamingMode(payload.length);
                    try (OutputStream outputStream = connection.getOutputStream()) {
                        outputStream.write(payload);
                    }
                }
                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
                String responseText = stream == null ? "" : new String(readBytes(stream), StandardCharsets.UTF_8);
                if (status >= 200 && status < 300) {
                    AppExecutors.main(() -> callback.onSuccess(responseText));
                    return;
                }
                JSONObject error = parseBody(responseText);
                String message = error.optString("message");
                if (TextUtils.isEmpty(message)) {
                    message = TextUtils.isEmpty(responseText) ? "请求失败" : responseText;
                }
                postError(callback, new ApiException(status, message));
            } catch (Exception exception) {
                postError(callback, new ApiException(-1, safeMessage(exception)));
            }
        });
    }

    private HttpURLConnection openConnection(String method, String url, boolean authRequired) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(18000);
        connection.setRequestProperty("Accept", "application/json");
        if (authRequired && sessionManager.isLoggedIn()) {
            connection.setRequestProperty("Authorization", "Bearer " + sessionManager.getToken());
        }
        return connection;
    }

    private void handleResponse(HttpURLConnection connection, ApiCallback<JSONObject> callback) throws Exception {
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        String body = stream == null ? "" : new String(readBytes(stream), StandardCharsets.UTF_8);
        if (status >= 200 && status < 300) {
            JSONObject json = parseBody(body);
            AppExecutors.main(() -> callback.onSuccess(json));
            return;
        }
        JSONObject error = parseBody(body);
        String message = error.optString("message");
        if (TextUtils.isEmpty(message)) {
            message = TextUtils.isEmpty(body) ? "请求失败" : body;
        }
        postError(callback, new ApiException(status, message));
    }

    private JSONObject parseBody(String body) throws Exception {
        if (TextUtils.isEmpty(body)) {
            return new JSONObject();
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("[")) {
            JSONObject json = new JSONObject();
            json.put("items", new JSONArray(trimmed));
            return json;
        }
        return new JSONObject(trimmed);
    }

    private void postError(ApiCallback<?> callback, ApiException exception) {
        AppExecutors.main(() -> callback.onError(exception));
    }

    private void writeField(DataOutputStream out, String boundary, String key, String value) throws IOException {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n");
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.writeBytes("\r\n");
    }

    private void writePart(DataOutputStream out, String boundary, MultipartPart part) throws IOException {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + part.fieldName + "\"; filename=\"" + part.fileName + "\"\r\n");
        out.writeBytes("Content-Type: " + part.contentType + "\r\n\r\n");
        out.write(part.bytes);
        out.writeBytes("\r\n");
    }

    private String buildUrl(String path, Map<String, String> query) {
        StringBuilder builder = new StringBuilder(sessionManager.getBaseUrl()).append(path);
        if (query == null || query.isEmpty()) {
            return builder.toString();
        }
        boolean first = true;
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if (TextUtils.isEmpty(entry.getValue())) {
                continue;
            }
            if (!first) {
                builder.append("&");
            } else {
                builder.append("?");
            }
            builder.append(Uri.encode(entry.getKey())).append("=").append(Uri.encode(entry.getValue()));
            first = false;
        }
        return builder.toString();
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) {
                return new byte[0];
            }
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            return out.toByteArray();
        }
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? "网络异常" : exception.getMessage();
    }

    public static class MultipartPart {
        public final String fieldName;
        public final String fileName;
        public final String contentType;
        public final byte[] bytes;

        public MultipartPart(String fieldName, String fileName, String contentType, byte[] bytes) {
            this.fieldName = fieldName;
            this.fileName = fileName;
            this.contentType = contentType;
            this.bytes = bytes;
        }
    }
}
