package com.example.ocr;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "medication_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Log all available extras
        Log.d("MedReminder", "Received intent with extras:");
        for (String key : intent.getExtras().keySet()) {
            Object value = intent.getExtras().get(key);
            Log.d("MedReminder", "Key: " + key + ", Value: " + value);
        }

        int reminderId = intent.getIntExtra("reminderId", -1);
        String medicationName = intent.getStringExtra("medicationName");
        String dosage = intent.getStringExtra("dosage");
        String time = intent.getStringExtra("time");

        // Log the extracted values
        Log.d("MedReminder", "Reminder ID: " + reminderId + ", Medication: " + medicationName + ", Dosage: " + dosage + ", Time: " + time);

        // Create notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Medication Reminder")
            .setContentText("Time to take " + medicationName + " (" + dosage + ")")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true);

        // Show notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(intent.getIntExtra("reminderId", 0), builder.build());
    }
} 