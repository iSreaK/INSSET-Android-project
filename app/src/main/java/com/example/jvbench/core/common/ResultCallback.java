package com.example.jvbench.core.common;

public interface ResultCallback<T> {
    void onSuccess(T result);

    void onError(String errorMessage);
}
