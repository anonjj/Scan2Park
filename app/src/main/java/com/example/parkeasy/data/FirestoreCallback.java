package com.example.parkeasy.data;

/**
 * A generic interface to handle asynchronous Firestore results.
 * @param <T> The type of data to return on success (e.g., User, List<Slot>, Void).
 */
public interface FirestoreCallback<T> {
    void onSuccess(T result);
    void onFailure(Exception e);
}