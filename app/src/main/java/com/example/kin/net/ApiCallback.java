package com.example.kin.net;

public interface ApiCallback<T> {
    void onSuccess(T data);

    void onError(ApiException exception);
}
