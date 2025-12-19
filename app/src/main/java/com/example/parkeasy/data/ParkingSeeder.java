package com.example.parkeasy.data;

import com.example.parkeasy.model.ParkingLocation;
import com.example.parkeasy.model.Slot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class ParkingSeeder {

    public static void seedData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // STEP 1: NUKE EVERYTHING (Delete old locations & slots)
        db.collection("parking_locations").get().addOnSuccessListener(snapshots -> {
            WriteBatch nukeBatch = db.batch();
            for (QueryDocumentSnapshot doc : snapshots) {
                nukeBatch.delete(doc.getReference());
            }

            db.collection("slots").get().addOnSuccessListener(slotSnapshots -> {
                for (QueryDocumentSnapshot slotDoc : slotSnapshots) {
                    nukeBatch.delete(slotDoc.getReference());
                }

                // Commit the DELETE
                nukeBatch.commit().addOnSuccessListener(aVoid -> {
                    android.util.Log.d("Seeder", "Database Nuked! Starting fresh seed...");
                    // STEP 2: PLANT FRESH DATA
                    plantSeeds(db);
                });
            });
        });
    }

    private static void plantSeeds(FirebaseFirestore db) {
        WriteBatch batch = db.batch();

        // --- 1. NEON CYBER MALL ---
        String loc1_id = "loc_neon_mall";
        ParkingLocation loc1 = new ParkingLocation();
        loc1.setLocationId(loc1_id);
        loc1.setName("Neon Cyber Mall");
        loc1.setAddress("Sector 4, Cyber City");
        loc1.setRatePerHour(40);
        loc1.setTotalSlots(12); // <--- NOW ACTIVE
        batch.set(db.collection("parking_locations").document(loc1_id), loc1);

        for (int i = 1; i <= 12; i++) {
            boolean isOccupied = (i == 1 || i == 4 || i == 7);
            long expiry = isOccupied ? System.currentTimeMillis() + 7200000 : 0;
            Slot slot = new Slot(loc1_id + "_A" + i, "A" + i, loc1_id, isOccupied, expiry);
            batch.set(db.collection("slots").document(slot.getSlotId()), slot);
        }

        // --- 2. SOLARIS TECH PARK ---
        String loc2_id = "loc_solaris_tech";
        ParkingLocation loc2 = new ParkingLocation();
        loc2.setLocationId(loc2_id);
        loc2.setName("Solaris Tech Park");
        loc2.setAddress("Business Bay");
        loc2.setRatePerHour(60);
        loc2.setTotalSlots(20); // <--- NOW ACTIVE
        batch.set(db.collection("parking_locations").document(loc2_id), loc2);

        for (int i = 1; i <= 20; i++) {
            boolean isOccupied = (i == 2 || i == 5 || i == 15 || i == 19);
            long expiry = isOccupied ? System.currentTimeMillis() + 3600000 : 0;
            Slot slot = new Slot(loc2_id + "_B" + i, "B" + i, loc2_id, isOccupied, expiry);
            batch.set(db.collection("slots").document(slot.getSlotId()), slot);
        }

        // --- 3. QUANTUM PLAZA ---
        String loc3_id = "loc_quantum_plaza";
        ParkingLocation loc3 = new ParkingLocation();
        loc3.setLocationId(loc3_id);
        loc3.setName("Quantum Plaza");
        loc3.setAddress("Gate 2, Westside");
        loc3.setRatePerHour(100);
        loc3.setTotalSlots(8); // <--- NOW ACTIVE
        batch.set(db.collection("parking_locations").document(loc3_id), loc3);

        for (int i = 1; i <= 8; i++) {
            boolean isOccupied = (i == 1);
            long expiry = isOccupied ? System.currentTimeMillis() + 18000000 : 0;
            Slot slot = new Slot(loc3_id + "_C" + i, "C" + i, loc3_id, isOccupied, expiry);
            batch.set(db.collection("slots").document(slot.getSlotId()), slot);
        }

        // Commit... (Rest stays the same)
        batch.commit()
                .addOnSuccessListener(v -> android.util.Log.d("Seeder", "✅ Fresh Data Planted!"))
                .addOnFailureListener(e -> android.util.Log.e("Seeder", "❌ Seed Failed", e));
    }
}