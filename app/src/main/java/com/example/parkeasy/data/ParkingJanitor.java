package com.example.parkeasy.utils;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class ParkingJanitor {

    public static void freeExpiredSlots() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long currentTime = System.currentTimeMillis();

        // 🔍 QUERY: Find all slots that are "Occupied" BUT have "Expired"
        db.collection("slots")
                .whereEqualTo("occupied", true)
                .whereLessThan("expiryTime", currentTime) // Expired!
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) return; // Nothing to clean

                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        // 🧹 CLEANUP: Set occupied to false, reset expiry
                        batch.update(doc.getReference(), "occupied", false);
                        batch.update(doc.getReference(), "expiryTime", 0);
                        batch.update(doc.getReference(), "bookingId", null);
                    }

                    // 🚀 COMMIT CHANGES
                    batch.commit().addOnSuccessListener(aVoid ->
                            Log.d("ParkingJanitor", "🧹 Cleaned " + snapshots.size() + " expired slots!")
                    );
                })
                .addOnFailureListener(e ->
                        Log.e("ParkingJanitor", "Cleanup failed", e)
                );
    }
}