package com.example.parkeasy.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.parkeasy.BookingHistoryActivity;
import com.example.parkeasy.R;

public class NotificationWorker extends Worker {

    private static final String TAG = "NotificationWorker";
    private static final String CHANNEL_ID = "PARKING_ALERTS";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String title = getInputData().getString("TITLE");
        String message = getInputData().getString("MESSAGE");

        Log.d(TAG, "Worker started. Title: " + title);

        if (title != null && message != null) {
            triggerNotification(title, message);
        }

        return Result.success();
    }

    private void triggerNotification(String title, String message) {
        Context context = getApplicationContext();

        // 1. Create Channel
        createNotificationChannel(context);

        // 2. Build Intent for Tap Action
        Intent intent = new Intent(context, BookingHistoryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                (int) System.currentTimeMillis(), 
                intent, 
                PendingIntent.FLAG_IMMUTABLE
        );

        // 3. Build Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // 4. Show Notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        
        // Permission check for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, 
                    android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Notification permission not granted!");
                return;
            }
        }

        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            Log.d(TAG, "Notification delivered successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send notification: " + e.getMessage());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Parking Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for parking timers and expiry");
            
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
