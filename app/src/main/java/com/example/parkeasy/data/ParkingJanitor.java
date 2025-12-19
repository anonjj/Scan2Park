package com.example.parkeasy.data;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class ParkingJanitor {

    /**
     * Checks all slots. If a slot is marked 'occupied' but the time has passed,
     * it forces the slot to become FREE again.
     */
    public static void freeExpiredSlots() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long currentTime = System.currentTimeMillis();

        // Query: Find slots that claim to be occupied
        db.collection("slots")
                .whereEqualTo("occupied", true)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) return;

                    WriteBatch batch = db.batch();
                    int count = 0;

                    for (DocumentSnapshot doc : snapshots) {
                        Long expiryTime = doc.getLong("expiryTime");
                        if (expiryTime != null && expiryTime < currentTime) {
                            // 🧹 CLEANUP: This slot expired! Free it.
                            batch.update(doc.getReference(), "occupied", false);
                            batch.update(doc.getReference(), "expiryTime", 0);
                            count++;
                        }
                    }

                    if (count > 0) {
                        final int cleanedCount = count;
                        batch.commit().addOnSuccessListener(aVoid ->
                                Log.d("ParkingJanitor", "🧹 Cleaned up " + cleanedCount + " expired slots.")
                        );
                    }
                })
                .addOnFailureListener(e -> Log.e("ParkingJanitor", "Cleanup failed", e));
    }
}