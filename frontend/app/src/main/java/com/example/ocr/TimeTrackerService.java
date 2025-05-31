package com.example.ocr;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.Calendar;

public class TimeTrackerService extends Service {
    private static final String TAG = "MedReminder";
    private static final long UPDATE_INTERVAL = 60000; // 1 minute in milliseconds

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TimeTrackerService started");
        startTrackingTime();
    }

    private void startTrackingTime() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(UPDATE_INTERVAL);
                    logCurrentTime();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Time tracking interrupted", e);
                    break;
                }
            }
        }).start();
    }

    private void logCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        String time = String.format("%02d:%02d:%02d", 
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND)
        );
        
        Log.d(TAG, "Current phone time: " + time);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "TimeTrackerService stopped");
    }
}
