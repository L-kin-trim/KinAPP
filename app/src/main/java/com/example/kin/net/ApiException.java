package com.example.kin.net;

public class ApiException extends Exception {
    private final int statusCode;

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isFeatureUnavailable() {
        return statusCode == 404 || statusCode == 405 || statusCode == 501;
    }
}
