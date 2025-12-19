package com.example.parkeasy.data;

import com.example.parkeasy.model.Booking;
import com.example.parkeasy.model.ParkingLocation;
import com.example.parkeasy.model.Slot;
import com.example.parkeasy.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized manager for all Firebase interactions.
 * Handles Auth, Firestore reads/writes, and complex transactions.
 * Designed as a singleton to prevent multiple instances.
 */
public class FirebaseManager {

    private static FirebaseManager instance;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mDb;



    // Generic callback for Firestore operations.
    public interface FirestoreCallback<T> {
        void onSuccess(T result);

        void onFailure(Exception e);
    }

    // Private constructor for the singleton pattern.
    private FirebaseManager() {
        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();
    }

    /**
     * Provides a single, shared instance of FirebaseManager.
     * Lazy initialization for efficiency.
     */
    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // ----------------------------------------------------------------
    //  AUTH & USER
    // ----------------------------------------------------------------

    public void createUser(String email, String password, String name, String phone, FirestoreCallback<Void> callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        // Business logic: new users get a welcome bonus.
                        User user = new User(uid, name, email, 500L, phone);
                        // Create the user profile document in Firestore.
                        mDb.collection("users").document(uid)
                                .set(user)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                                .addOnFailureListener(callback::onFailure);
                    } else {
                        // Pass the auth failure (e.g., email already exists) up the chain.
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void loginUser(String email, String password, final FirestoreCallback<User> callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        // On successful login, fetch the complete user profile data.
                        getUserData(mAuth.getCurrentUser().getUid(), callback);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    /**
     * Fetches a user's profile from the 'users' collection.
     */
    public void getUserData(String userId, final FirestoreCallback<User> callback) {
        mDb.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Deserialize the document into our User model.
                        callback.onSuccess(documentSnapshot.toObject(User.class));
                    } else {
                        // Sanity check: user is authed but has no db record.
                        callback.onFailure(new Exception("User data not found in Firestore."));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void logout() {
        mAuth.signOut();
    }

    // ----------------------------------------------------------------
    //  LOCATIONS & SLOTS
    // ----------------------------------------------------------------

    public void fetchParkingLocations(FirestoreCallback<List<ParkingLocation>> callback) {
        mDb.collection("parking_locations").get()
                .addOnSuccessListener(snapshots -> {
                    List<ParkingLocation> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        ParkingLocation loc = doc.toObject(ParkingLocation.class);
                        // The document ID is the locationId, set it manually.
                        loc.setLocationId(doc.getId());
                        list.add(loc);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // ----------------------------------------------------------------
    //  BOOKING ENGINE (TRANSACTIONS)
    // ----------------------------------------------------------------

    /**
     * Atomically confirms a booking. Uses a transaction to prevent race conditions
     * like double-booking a slot or charging a user for a slot that was just taken.
     * The whole operation will fail if any step fails.
     */
    public void confirmBooking(String userId, String userName, String userEmail, String locationId, String slotId, int ratePerHour, int durationHours, FirestoreCallback<Booking> callback) {
        final int totalCost = ratePerHour * durationHours;
        final String bookingId = "bk_" + System.currentTimeMillis();

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationHours * 3600000L); // 1 hour in millis

        mDb.runTransaction(transaction -> {
            // Step 1: Validate user's wallet balance.
            DocumentSnapshot userSnap = transaction.get(mDb.collection("users").document(userId));
            Long balance = userSnap.getLong("walletBalance");
            if (balance == null) balance = 0L;

            if (balance < totalCost) {
                // Abort the transaction if funds are insufficient.
                throw new FirebaseFirestoreException("Insufficient Wallet Balance!", FirebaseFirestoreException.Code.ABORTED);
            }

            // Step 2: Check for slot availability. Critical for preventing double-booking.
            DocumentSnapshot slotSnap = transaction.get(mDb.collection("slots").document(slotId));
            boolean isOccupied = Boolean.TRUE.equals(slotSnap.getBoolean("occupied"));
            Long expiryTime = slotSnap.getLong("expiryTime");
            long slotExpiry = (expiryTime != null) ? expiryTime : 0;

            // "Lazy unlock" logic: if a slot is marked 'occupied' but its timer has expired,
            // we treat it as available and allow the booking to proceed.
            if (isOccupied && slotExpiry > System.currentTimeMillis()) {
                // Abort if another user has a valid, active booking for this slot.
                throw new FirebaseFirestoreException("Slot is currently in use!", FirebaseFirestoreException.Code.ABORTED);
            }

            // Step 3: Atomically update wallet and slot state.
            // This only happens if the above checks pass.
            transaction.update(mDb.collection("users").document(userId), "walletBalance", balance - totalCost);
            transaction.update(mDb.collection("slots").document(slotId), "occupied", true);
            transaction.update(mDb.collection("slots").document(slotId), "expiryTime", endTime);

            // Step 4: Create the official booking record.
            Booking booking = new Booking();
            booking.setBookingId(bookingId);
            booking.setUserId(userId);
            booking.setSlotId(slotId);
            booking.setVehicleNumber("MH-43-CY-2077"); // Placeholder vehicle number.
            booking.setStartTime(new Date(startTime));
            booking.setEndTime(new Date(endTime));
            booking.setTotalCost(totalCost);
            booking.setStatus("ACTIVE");

            // Save the new booking document.
            transaction.set(mDb.collection("bookings").document(bookingId), booking);

            // The booking object is the success payload for this transaction.
            return booking;

                }).addOnSuccessListener(booking -> callback.onSuccess(booking))
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    /**
     * Cancels a booking and frees up the associated parking slot.
     * Transaction ensures the booking status and slot state are updated together.
     */
    public void cancelBooking(String bookingId, String slotId, FirestoreCallback<Void> callback) {
        mDb.runTransaction(transaction -> {
                    // Mark the booking as cancelled.
                    transaction.update(mDb.collection("bookings").document(bookingId), "status", "CANCELLED");

                    // Free up the slot for others.
                    transaction.update(mDb.collection("slots").document(slotId), "occupied", false);
                    transaction.update(mDb.collection("slots").document(slotId), "expiryTime", 0);

                    return null; // No return value needed for this operation.
        }).addOnSuccessListener(result -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Extends a booking by one hour.
     * Transaction ensures the booking time, slot expiry, and cost are updated atomically.
     */
    public void extendSpecificBooking(String bookingId, String slotId, int extraCost, FirestoreCallback<String> callback) {
        mDb.runTransaction(transaction -> {
            DocumentSnapshot bookingSnap = transaction.get(mDb.collection("bookings").document(bookingId));
            Date currentEnd = bookingSnap.getDate("endTime");
            if (currentEnd == null) currentEnd = new Date(); // Sanity check for old/bad data.

            // Business logic: extensions are in 1-hour increments.
            long newEndMillis = currentEnd.getTime() + 3600000;
            Date newEndTime = new Date(newEndMillis);

            // Update the booking payload.
            transaction.update(mDb.collection("bookings").document(bookingId), "endTime", newEndTime);
            transaction.update(mDb.collection("bookings").document(bookingId), "totalCost", bookingSnap.getLong("totalCost") + extraCost);

            // Sync the slot's expiry time with the new booking end time.
            transaction.update(mDb.collection("slots").document(slotId), "expiryTime", newEndMillis);

            return "Extended until " + newEndTime;
        }).addOnSuccessListener(callback::onSuccess).addOnFailureListener(callback::onFailure);
    }

    // ----------------------------------------------------------------
    //  WALLET & TRANSACTIONS
    // ----------------------------------------------------------------

    public void addMoneyToWallet(String userId, double amount, FirestoreCallback<Void> callback) {
        mDb.runTransaction(transaction -> {
            // 1. Get User
            com.google.firebase.firestore.DocumentReference userRef = mDb.collection("users").document(userId);
            DocumentSnapshot userSnap = transaction.get(userRef);
            Long currentBalance = userSnap.getLong("walletBalance");
            if (currentBalance == null) currentBalance = 0L;

            // 2. Add Money
            double newBalance = currentBalance + amount;
            transaction.update(userRef, "walletBalance", newBalance);

            // 3. Create Transaction Record
            String txId = "tx_" + System.currentTimeMillis();
            com.example.parkeasy.model.Transaction tx = new com.example.parkeasy.model.Transaction(
                    txId, userId, amount, "CREDIT", "Wallet Top-up", new Date()
            );
            transaction.set(mDb.collection("transactions").document(txId), tx);

            return null;
        }).addOnSuccessListener(result -> callback.onSuccess(null))
          .addOnFailureListener(callback::onFailure);
    }

    public void fetchTransactions(String userId, FirestoreCallback<List<com.example.parkeasy.model.Transaction>> callback) {
        mDb.collection("transactions")
                .whereEqualTo("userId", userId)
                // .orderBy("timestamp", Query.Direction.DESCENDING) // Needs Index
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<com.example.parkeasy.model.Transaction> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        list.add(doc.toObject(com.example.parkeasy.model.Transaction.class));
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }
    // REPLACES your existing "bookSlot" method
    public void bookSlot(Slot slot, String userId, int durationHours, double totalPrice, FirestoreCallback<String> callback) {
        mDb.runTransaction(transaction -> {
                    // 1. Check User Wallet Balance
                    DocumentSnapshot userSnap = transaction.get(mDb.collection("users").document(userId));
                    Double currentBalance = userSnap.getDouble("walletBalance");
                    if (currentBalance == null) currentBalance = 0.0;

                    if (currentBalance < totalPrice) {
                        throw new FirebaseFirestoreException("Insufficient Funds!", FirebaseFirestoreException.Code.ABORTED);
                    }

                    // 2. Generate IDs
                    String bookingId = mDb.collection("bookings").document().getId();
                    Date startTime = new Date();
                    // Calculate End Time (Start + Duration)
                    long endTimeMillis = startTime.getTime() + (durationHours * 3600000L);

                    // 3. Create Booking Data
                    Map<String, Object> bookingData = new HashMap<>();
                    bookingData.put("bookingId", bookingId);
                    // ⚠️ CHECK: Does your Slot model use getId() or getSlotId()? Use the correct one!
                    bookingData.put("slotId", slot.getSlotId());
                    bookingData.put("slotName", slot.getName());
                    bookingData.put("userId", userId);
                    bookingData.put("startTime", startTime);
                    bookingData.put("endTime", new Date(endTimeMillis)); // Store End Time
                    bookingData.put("durationHours", durationHours);
                    bookingData.put("totalCost", totalPrice);
                    bookingData.put("status", "CONFIRMED");
                    bookingData.put("locationName", "Quantum Plaza"); // Or pass this in if available

                    // 4. WRITE EVERYTHING AT ONCE
                    // Deduct Money
                    transaction.update(mDb.collection("users").document(userId), "walletBalance", currentBalance - totalPrice);
                    // Create Booking
                    transaction.set(mDb.collection("bookings").document(bookingId), bookingData);
                    // Mark Slot Occupied
                    transaction.update(mDb.collection("slots").document(slot.getSlotId()), "occupied", true);
                    transaction.update(mDb.collection("slots").document(slot.getSlotId()), "expiryTime", endTimeMillis);

                    return bookingId; // Success! Return the ID.

                }).addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onFailure);
    }
    // ----------------------------------------------------------------
    //  HISTORY FETCHING
    // ----------------------------------------------------------------

    public void getUserBookings(FirestoreCallback<List<Booking>> callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        mDb.collection("bookings")
                .whereEqualTo("userId", uid) // Filter by current user
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Booking> list = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                        Booking b = doc.toObject(Booking.class);
                        b.setBookingId(doc.getId()); // Ensure ID is captured
                        list.add(b);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }
}

