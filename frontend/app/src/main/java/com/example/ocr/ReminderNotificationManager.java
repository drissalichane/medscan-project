package com.example.ocr;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

public class ReminderNotificationManager {
    private static final String CHANNEL_ID = "medication_reminders";
    private static final String CHANNEL_NAME = "Medication Reminders";
    private static final String CHANNEL_DESCRIPTION = "Notifications for medication reminders";

    private final Context context;
    private final AlarmManager alarmManager;
    private final NotificationManager notificationManager;

    public ReminderNotificationManager(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void scheduleReminder(int reminderId, String medicationName, String dosage, List<String> reminderTimes, String frequency) {
        Log.d("MedReminder", "Scheduling reminder: ID=" + reminderId + ", Medication=" + medicationName + ", Times=" + reminderTimes + ", Frequency=" + frequency);
        // Cancel any existing alarms for this reminder
        cancelReminder(reminderId);

        // Schedule new alarms for each time
        for (String time : reminderTimes) {
            String[] timeParts = time.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            // Create intent for the alarm
            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra("reminderId", reminderId);
            intent.putExtra("medicationName", medicationName);
            intent.putExtra("dosage", dosage);
            intent.putExtra("time", time);

            // Create a unique request code based on reminder ID and time
            int requestCode = (reminderId * 100000) + (hour * 100) + minute;
            
            // Create the PendingIntent
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Set the alarm
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // Get current day (1=Sunday, 2=Monday, etc.)
            int currentDay = calendar.get(Calendar.DAY_OF_WEEK);
            
            // Find the next occurrence for each day in the frequency
            String[] daysOfWeek = frequency.split(", ");
            boolean foundDay = false;
            
            for (String day : daysOfWeek) {
                int targetDay = 0;
                switch (day.toLowerCase()) {
                    case "sunday":
                        targetDay = Calendar.SUNDAY;
                        break;
                    case "monday":
                        targetDay = Calendar.MONDAY;
                        break;
                    case "tuesday":
                        targetDay = Calendar.TUESDAY;
                        break;
                    case "wednesday":
                        targetDay = Calendar.WEDNESDAY;
                        break;
                    case "thursday":
                        targetDay = Calendar.THURSDAY;
                        break;
                    case "friday":
                        targetDay = Calendar.FRIDAY;
                        break;
                    case "saturday":
                        targetDay = Calendar.SATURDAY;
                        break;
                }
                
                // Set the target day
                calendar.set(Calendar.DAY_OF_WEEK, targetDay);
                
                // If the time has already passed today, move to next occurrence
                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    // If it's the same day, add 7 days
                    if (currentDay == targetDay) {
                        calendar.add(Calendar.DAY_OF_YEAR, 7);
                    } else {
                        // If it's a future day this week, keep it
                        if (targetDay > currentDay) {
                            // Do nothing, it's already set for this week
                        } else {
                            // If it's a past day, add 7 days
                            calendar.add(Calendar.DAY_OF_YEAR, 7);
                        }
                    }
                }
                
                // Log detailed timing information
                
                // Calculate time until next alarm
                long currentTime = System.currentTimeMillis();
                long nextAlarmTime = calendar.getTimeInMillis();
                long timeUntilAlarm = nextAlarmTime - currentTime;
                long minutesUntilAlarm = timeUntilAlarm / (1000 * 60);

                Log.d("MedReminder", "Scheduled alarm for reminder ID=" + reminderId + " at " + time + 
                    ", next occurrence: " + calendar.getTime() + 
                    ", minutes until alarm: " + minutesUntilAlarm + 
                    ", for day: " + day);

                // Set the alarm
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );

                // Schedule the next occurrence for next week
                Calendar nextWeek = (Calendar) calendar.clone();
                nextWeek.add(Calendar.DAY_OF_YEAR, 7);
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    nextWeek.getTimeInMillis(),
                    pendingIntent
                );

                foundDay = true;
                break; // Only need to find one valid day
            }
            
            if (!foundDay) {
                Log.e("MedReminder", "No valid day found in frequency: " + frequency);
                return;
            }

        }
    }

    public void cancelReminder(int reminderId) {
        // Cancel all alarms for this reminder
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }
} 