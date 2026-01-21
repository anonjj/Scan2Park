package com.example.parkeasy;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.parkeasy.databinding.ActivityBookingSummaryBinding;
import com.example.parkeasy.model.Booking;
import com.example.parkeasy.service.NotificationWorker;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

public class BookingSummaryActivity extends AppCompatActivity {

    private static final String TAG = "BookingSummary";
    private ActivityBookingSummaryBinding binding;
    private String bookingId;
    private static final int QR_CODE_SIZE_PX = 600;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingSummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bookingId = getIntent().getStringExtra("BOOKING_ID");

        if (bookingId != null && !bookingId.isEmpty()) {
            loadBookingFromFirestore();
        } else {
            loadBookingFromIntent();
        }

        // Home Button Logic
        binding.btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupShareButton(String location, String slot, String time, String vehicle, String bId) {
        if (binding.btnShare == null) return;
        binding.btnShare.setOnClickListener(v -> {
            String safeId = (bId != null) ? bId : "PENDING";
            String vehicleLine = (vehicle != null && !vehicle.isEmpty() && !"NOT_SET".equals(vehicle))
                    ? "🚘 Vehicle: " + vehicle + "\n"
                    : "";
            String shareMessage = "🚗 *Parking Booking Confirmed!*\n\n" +
                    "📍 Location: " + location + "\n" +
                    "🅿️ Slot: " + slot + "\n" +
                    "🕒 Time: " + time + "\n" +
                    vehicleLine +
                    "🆔 Booking ID: " + safeId + "\n\n" +
                    "Navigate via: http://maps.google.com/?q=" + location.replace(" ", "+") + "\n" +
                    "- Shared via ParkEasy App";

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Parking Receipt");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "Share Receipt via"));
        });
    }

    private void loadBookingFromIntent() {
        String slotName = getIntent().getStringExtra("SLOT_NAME");
        String location = getIntent().getStringExtra("LOCATION_NAME");
        double totalCost = getIntent().getDoubleExtra("TOTAL_COST", 0.0);
        long startTimeMillis = getIntent().getLongExtra("START_TIME", System.currentTimeMillis());
        int durationHours = getIntent().getIntExtra("DURATION", 1);
        String vehicleNumber = getIntent().getStringExtra("VEHICLE_NUMBER");

        String resolvedBookingId = resolveBookingId(null, startTimeMillis);
        displayDetails(resolvedBookingId, location, slotName, vehicleNumber, totalCost, startTimeMillis, durationHours);
        scheduleReminders(startTimeMillis, durationHours);
    }

    private void loadBookingFromFirestore() {
        if (binding.progressBar != null) binding.progressBar.setVisibility(View.VISIBLE);
        if (binding.contentLayout != null) binding.contentLayout.setVisibility(View.GONE);

        FirebaseFirestore.getInstance().collection("bookings")
                .document(bookingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Booking booking = documentSnapshot.toObject(Booking.class);
                        if (booking != null) {
                            long startTimeMillis = booking.getStartTime() != null
                                    ? booking.getStartTime().getTime()
                                    : System.currentTimeMillis();
                            int durationHours = booking.getDurationHours() > 0 ? booking.getDurationHours() : 1;
                            String resolvedBookingId = resolveBookingId(booking.getBookingId(), startTimeMillis);

                            displayDetails(
                                    resolvedBookingId,
                                    booking.getLocationName(),
                                    booking.getSlotName(),
                                    booking.getVehicleNumber(),
                                    booking.getTotalCost(),
                                    startTimeMillis,
                                    durationHours
                            );
                            scheduleReminders(startTimeMillis, durationHours);
                        }
                    }
                    if (binding.progressBar != null) binding.progressBar.setVisibility(View.GONE);
                    if (binding.contentLayout != null) binding.contentLayout.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading booking", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayDetails(String resolvedBookingId, String location, String slot, String vehicle,
                                double cost, long start, int duration) {
        bookingId = resolvedBookingId;
        long end = start + ((long) duration * 3600000L);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeRange = timeFormat.format(new Date(start)) + " - " + timeFormat.format(new Date(end));

        binding.tvReceiptLocation.setText(location);
        binding.tvReceiptSlot.setText(slot);
        binding.tvReceiptAmount.setText("₹" + (int) cost);
        binding.tvReceiptDateTime.setText(timeRange);
        if (binding.tvReceiptVehicle != null) {
            String vehicleText = (vehicle != null && !vehicle.isEmpty() && !"NOT_SET".equals(vehicle))
                    ? vehicle
                    : "Not set";
            binding.tvReceiptVehicle.setText(vehicleText);
        }

        setupShareButton(location, slot, timeRange, vehicle, resolvedBookingId);
        startTimer(start, end);
        renderQrCode(resolvedBookingId, location, slot, vehicle, start, end, duration, cost);
    }

    private String resolveBookingId(String fallbackId, long startTimeMillis) {
        if (bookingId != null && !bookingId.isEmpty()) return bookingId;
        if (fallbackId != null && !fallbackId.isEmpty()) return fallbackId;
        return "temp_" + startTimeMillis;
    }

    private void renderQrCode(String resolvedBookingId, String location, String slot, String vehicle,
                              long startMillis, long endMillis, int durationHours, double totalCost) {
        if (binding.ivQrCode == null) return;

        String qrPayload;
        try {
            JSONObject payload = new JSONObject();
            payload.put("v", 1);
            payload.put("bookingId", resolvedBookingId);
            payload.put("location", location != null ? location : "");
            payload.put("slot", slot != null ? slot : "");
            payload.put("vehicle", vehicle != null ? vehicle : "");
            payload.put("startTime", startMillis);
            payload.put("endTime", endMillis);
            payload.put("durationHours", durationHours);
            payload.put("totalCost", totalCost);
            qrPayload = payload.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build QR payload", e);
            return;
        }

        try {
            Bitmap bitmap = createQrBitmap(qrPayload, QR_CODE_SIZE_PX);
            binding.ivQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Log.e(TAG, "Failed to generate QR code", e);
        }

        if (binding.tvQrId != null) {
            binding.tvQrId.setText("ID: " + resolvedBookingId);
        }
    }

    private Bitmap createQrBitmap(String content, int sizePx) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx);
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private void startTimer(long startMillis, long endMillis) {
        if (binding.tvTimerValue == null) return;
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (now >= endMillis) {
                    binding.tvTimerValue.setText("Expired");
                    return;
                }

                long remaining;
                String prefix;
                if (now < startMillis) {
                    prefix = "Starts in ";
                    remaining = startMillis - now;
                } else {
                    prefix = "Time left ";
                    remaining = endMillis - now;
                }

                binding.tvTimerValue.setText(prefix + formatDuration(remaining));
                timerHandler.postDelayed(this, 1000L);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void scheduleReminders(long startTimeMillis, int durationHours) {
        long endTimeMillis = startTimeMillis + (durationHours * 3600000L);
        long currentTime = System.currentTimeMillis();

        Log.d(TAG, "Scheduling reminders. EndTime: " + new Date(endTimeMillis));

        // --- TEST/CONFIRMATION: Immediate (5 seconds delay) ---
        Data confirmData = new Data.Builder()
                .putString("TITLE", "✅ Booking Confirmed")
                .putString("MESSAGE", "Your spot " + binding.tvReceiptSlot.getText() + " is ready for use.")
                .build();
        
        OneTimeWorkRequest confirmRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInitialDelay(2, TimeUnit.SECONDS)
                .setInputData(confirmData)
                .addTag("booking_confirmation")
                .build();
        WorkManager.getInstance(this).enqueue(confirmRequest);

        // --- REMINDER 1: 15 Minutes Before ---
        long warningTime = endTimeMillis - (15 * 60 * 1000);
        long delayWarning = warningTime - currentTime;

        if (delayWarning > 0) {
            Data data = new Data.Builder()
                    .putString("TITLE", "⏳ Time is running out!")
                    .putString("MESSAGE", "You have 15 minutes left on your parking slot.")
                    .build();

            OneTimeWorkRequest warningRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                    .setInitialDelay(delayWarning, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag("parking_reminder")
                    .build();

            WorkManager.getInstance(this).enqueue(warningRequest);
            Log.d(TAG, "Warning reminder enqueued with delay: " + (delayWarning / 1000) + "s");
        }

        // --- REMINDER 2: Overtime Alert ---
        long delayOvertime = endTimeMillis - currentTime;

        if (delayOvertime > 0) {
            Data overtimeData = new Data.Builder()
                    .putString("TITLE", "⚠️ OVERTIME ZONE ENTERED")
                    .putString("MESSAGE", "Your parking has expired! Extend now to avoid fines.")
                    .build();

            OneTimeWorkRequest overtimeRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                    .setInitialDelay(delayOvertime, TimeUnit.MILLISECONDS)
                    .setInputData(overtimeData)
                    .addTag("parking_overtime")
                    .build();

            WorkManager.getInstance(this).enqueue(overtimeRequest);
            Log.d(TAG, "Overtime reminder enqueued with delay: " + (delayOvertime / 1000) + "s");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerRunnable != null) timerHandler.removeCallbacks(timerRunnable);
    }
}
