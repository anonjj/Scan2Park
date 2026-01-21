package com.example.parkeasy.data;

import com.example.parkeasy.model.Booking;
import com.example.parkeasy.model.Owner;
import com.example.parkeasy.model.ParkingLocation;
import com.example.parkeasy.model.Slot;
import com.example.parkeasy.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

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
    private static final long LOCATION_CACHE_TTL_MS = 60_000;
    private static final long SLOT_CACHE_TTL_MS = 30_000;
    private List<ParkingLocation> cachedLocations = new ArrayList<>();
    private long cachedLocationsAt = 0;
    private final Map<String, SlotCacheEntry> slotCache = new HashMap<>();

    private static class SlotCacheEntry {
        private final List<Slot> slots;
        private final long cachedAt;

        private SlotCacheEntry(List<Slot> slots, long cachedAt) {
            this.slots = slots;
            this.cachedAt = cachedAt;
        }
    }



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
    //  AUTH & OWNER
    // ----------------------------------------------------------------

    public void createOwner(String email, String password, Owner owner, FirestoreCallback<Void> callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        owner.setOwnerId(uid);
                        owner.setEmail(email);
                        owner.setCreatedAt(System.currentTimeMillis());
                        mDb.collection("owners").document(uid)
                                .set(owner)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                                .addOnFailureListener(callback::onFailure);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void loginOwner(String email, String password, FirestoreCallback<Owner> callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();
                        mDb.collection("owners").document(uid).get()
                                .addOnSuccessListener(snapshot -> {
                                    if (snapshot.exists()) {
                                        callback.onSuccess(snapshot.toObject(Owner.class));
                                    } else {
                                        mAuth.signOut();
                                        callback.onFailure(new Exception("No owner profile found."));
                                    }
                                })
                                .addOnFailureListener(callback::onFailure);
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    // ----------------------------------------------------------------
    //  LOCATIONS & SLOTS
    // ----------------------------------------------------------------

    public void fetchParkingLocations(FirestoreCallback<List<ParkingLocation>> callback) {
        List<ParkingLocation> cached = getCachedLocations();
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }

        mDb.collection("parking_locations").get()
                .addOnSuccessListener(snapshots -> {
                    List<ParkingLocation> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        ParkingLocation loc = doc.toObject(ParkingLocation.class);
                        // The document ID is the locationId, set it manually.
                        loc.setLocationId(doc.getId());
                        Boolean active = doc.getBoolean("active");
                        boolean isActive = active == null || Boolean.TRUE.equals(active);
                        loc.setActive(isActive);
                        if (isActive) {
                            list.add(loc);
                        }
                    }
                    updateLocationCache(list);
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void fetchOwnerLocations(String ownerId, FirestoreCallback<List<ParkingLocation>> callback) {
        mDb.collection("parking_locations")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<ParkingLocation> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        ParkingLocation loc = doc.toObject(ParkingLocation.class);
                        loc.setLocationId(doc.getId());
                        Boolean active = doc.getBoolean("active");
                        loc.setActive(active == null || Boolean.TRUE.equals(active));
                        list.add(loc);
                    }
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void updateOwnerLocation(String locationId, String name, String address, int ratePerHour,
                                    int newTotalSlots, int currentTotalSlots, FirestoreCallback<Void> callback) {
        WriteBatch batch = mDb.batch();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("address", address);
        updates.put("ratePerHour", ratePerHour);
        updates.put("totalSlots", newTotalSlots);
        batch.update(mDb.collection("parking_locations").document(locationId), updates);

        if (newTotalSlots > currentTotalSlots) {
            for (int i = currentTotalSlots + 1; i <= newTotalSlots; i++) {
                String slotName = "S" + i;
                String slotId = locationId + "_" + slotName;
                Slot slot = new Slot(slotId, slotName, locationId, false, 0);
                batch.set(mDb.collection("slots").document(slotId), slot);
            }
        } else if (newTotalSlots < currentTotalSlots) {
            for (int i = newTotalSlots + 1; i <= currentTotalSlots; i++) {
                String slotId = locationId + "_S" + i;
                batch.update(mDb.collection("slots").document(slotId), "active", false);
            }
            for (int i = 1; i <= newTotalSlots; i++) {
                String slotId = locationId + "_S" + i;
                batch.update(mDb.collection("slots").document(slotId), "active", true);
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    invalidateLocationCache();
                    invalidateSlotCache(locationId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void updateLocationActive(String locationId, boolean isActive, FirestoreCallback<Void> callback) {
        mDb.collection("parking_locations").document(locationId)
                .update("active", isActive)
                .addOnSuccessListener(aVoid -> {
                    invalidateLocationCache();
                    callback.onSuccess(null);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void addParkingLocationForOwner(String ownerId, String name, String address, int ratePerHour, int totalSlots,
                                           FirestoreCallback<String> callback) {
        if (ownerId == null || ownerId.isEmpty()) {
            callback.onFailure(new Exception("Owner not logged in."));
            return;
        }

        String locationId = mDb.collection("parking_locations").document().getId();
        ParkingLocation location = new ParkingLocation();
        location.setLocationId(locationId);
        location.setName(name);
        location.setAddress(address);
        location.setRatePerHour(ratePerHour);
        location.setTotalSlots(totalSlots);
        location.setOwnerId(ownerId);
        location.setActive(true);

        WriteBatch batch = mDb.batch();
        batch.set(mDb.collection("parking_locations").document(locationId), location);

        for (int i = 1; i <= totalSlots; i++) {
            String slotName = "S" + i;
            String slotId = locationId + "_" + slotName;
            Slot slot = new Slot(slotId, slotName, locationId, false, 0);
            batch.set(mDb.collection("slots").document(slotId), slot);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    invalidateLocationCache();
                    callback.onSuccess(locationId);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public synchronized List<ParkingLocation> getCachedLocations() {
        if (cachedLocationsAt == 0) {
            return null;
        }
        long age = System.currentTimeMillis() - cachedLocationsAt;
        if (age > LOCATION_CACHE_TTL_MS) {
            return null;
        }
        return new ArrayList<>(cachedLocations);
    }

    public synchronized List<Slot> getCachedSlots(String locationId) {
        SlotCacheEntry entry = slotCache.get(locationId);
        if (entry == null) {
            return null;
        }
        long age = System.currentTimeMillis() - entry.cachedAt;
        if (age > SLOT_CACHE_TTL_MS) {
            slotCache.remove(locationId);
            return null;
        }
        return new ArrayList<>(entry.slots);
    }

    public synchronized void updateSlotCache(String locationId, List<Slot> slots) {
        slotCache.put(locationId, new SlotCacheEntry(new ArrayList<>(slots), System.currentTimeMillis()));
    }

    private synchronized void updateLocationCache(List<ParkingLocation> locations) {
        cachedLocations = new ArrayList<>(locations);
        cachedLocationsAt = System.currentTimeMillis();
    }

    private synchronized void invalidateLocationCache() {
        cachedLocationsAt = 0;
        cachedLocations.clear();
    }

    private synchronized void invalidateSlotCache(String locationId) {
        slotCache.remove(locationId);
    }

    // ----------------------------------------------------------------
    //  BOOKING ENGINE (TRANSACTIONS)
    // ----------------------------------------------------------------

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
    public void bookSlot(Slot slot, String userId, String locationName, String vehicleNumber, long startTimeMillis,
                         int durationHours, double totalPrice,
                         FirestoreCallback<String> callback) {
        mDb.runTransaction(transaction -> {
                    if (slot == null || slot.getSlotId() == null) {
                        throw new FirebaseFirestoreException("Slot not found.", FirebaseFirestoreException.Code.ABORTED);
                    }

                    // 1. Check User Wallet Balance
                    DocumentSnapshot userSnap = transaction.get(mDb.collection("users").document(userId));
                    Object balanceObj = userSnap.get("walletBalance");
                    double currentBalance = 0.0;
                    if (balanceObj instanceof Number) {
                        currentBalance = ((Number) balanceObj).doubleValue();
                    }

                    if (currentBalance < totalPrice) {
                        throw new FirebaseFirestoreException("Insufficient Funds!", FirebaseFirestoreException.Code.ABORTED);
                    }

                    // 2. Check Slot State
                    DocumentSnapshot slotSnap = transaction.get(mDb.collection("slots").document(slot.getSlotId()));
                    Boolean isActive = slotSnap.getBoolean("active");
                    if (isActive != null && !isActive) {
                        throw new FirebaseFirestoreException("Slot is unavailable!", FirebaseFirestoreException.Code.ABORTED);
                    }
                    boolean isOccupied = Boolean.TRUE.equals(slotSnap.getBoolean("occupied"));
                    Long expiryTime = slotSnap.getLong("expiryTime");
                    long slotExpiry = (expiryTime != null) ? expiryTime : 0;
                    if (isOccupied && slotExpiry > System.currentTimeMillis()) {
                        throw new FirebaseFirestoreException("Slot is currently in use!", FirebaseFirestoreException.Code.ABORTED);
                    }

                    // 3. Generate IDs
                    String bookingId = mDb.collection("bookings").document().getId();
                    Date startTime = new Date(startTimeMillis);
                    long endTimeMillis = startTimeMillis + (durationHours * 3600000L);

                    // 4. Create Booking Data
                    Map<String, Object> bookingData = new HashMap<>();
                    bookingData.put("bookingId", bookingId);
                    bookingData.put("slotId", slot.getSlotId());
                    bookingData.put("slotName", slot.getName());
                    bookingData.put("locationId", slot.getLocationId());
                    bookingData.put("locationName", locationName != null ? locationName : "");
                    bookingData.put("userId", userId);
                    bookingData.put("vehicleNumber", vehicleNumber != null ? vehicleNumber : "");
                    bookingData.put("startTime", startTime);
                    bookingData.put("endTime", new Date(endTimeMillis));
                    bookingData.put("durationHours", durationHours);
                    bookingData.put("totalCost", totalPrice);
                    bookingData.put("status", "ACTIVE");

                    // 5. WRITE EVERYTHING AT ONCE
                    transaction.update(mDb.collection("users").document(userId), "walletBalance", currentBalance - totalPrice);
                    transaction.set(mDb.collection("bookings").document(bookingId), bookingData);
                    transaction.update(mDb.collection("slots").document(slot.getSlotId()), "occupied", true);
                    transaction.update(mDb.collection("slots").document(slot.getSlotId()), "expiryTime", endTimeMillis);

                    return bookingId;

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
