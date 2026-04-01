package com.example.realtime_data_firebase;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StreakManager {

    private static StreakManager instance;
    private long startTime;
    private boolean isTracking = false;
    private Handler handler;
    private Runnable runnable;
    private static final long STREAK_THRESHOLD_MS = 30 * 60 * 1000; // 30 minutes
    // private static final long STREAK_THRESHOLD_MS = 10 * 1000; // 10 seconds for
    // testing

    public static synchronized StreakManager getInstance() {
        if (instance == null) {
            instance = new StreakManager();
        }
        return instance;
    }

    private StreakManager() {
        handler = new Handler(Looper.getMainLooper());
    }

    public void startTracking(Context context) {
        if (isTracking)
            return;
        isTracking = true;
        startTime = System.currentTimeMillis();

        runnable = new Runnable() {
            @Override
            public void run() {
                if (!isTracking)
                    return;
                updateActiveTime(context);
                handler.postDelayed(this, 60 * 1000); // Check every minute
            }
        };
        handler.post(runnable);
    }

    public void stopTracking(Context context) {
        if (!isTracking)
            return;
        isTracking = false;
        handler.removeCallbacks(runnable);
        updateActiveTime(context);
    }

    private void updateActiveTime(Context context) {
        if (context == null)
            return;
        SharedPreferences prefs = context.getSharedPreferences("StreakPrefs", Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        long lastSavedTime = prefs.getLong("lastSavedTime", System.currentTimeMillis());
        long currentTime = System.currentTimeMillis();
        long sessionDuration = currentTime - lastSavedTime;

        // Fix: Use startTime for session tracking if just started, but to be robust:
        // We accumulate time.
        // Better logic: add (currentTime - startTime) to total, then reset startTime to
        // currentTime.
        long durationToAdd = currentTime - startTime;
        startTime = currentTime; // Reset start time for next tick

        if (durationToAdd < 0)
            durationToAdd = 0;

        long totalTimeToday = prefs.getLong("time_" + today, 0) + durationToAdd;

        // Save
        prefs.edit()
                .putLong("time_" + today, totalTimeToday)
                .putLong("lastSavedTime", currentTime)
                .apply();

        checkStreak(context, totalTimeToday, today);
    }

    private void checkStreak(Context context, long totalTimeToday, String today) {
        SharedPreferences prefs = context.getSharedPreferences("StreakPrefs", Context.MODE_PRIVATE);
        boolean streakUpdatedToday = prefs.getBoolean("updated_" + today, false);

        if (!streakUpdatedToday && totalTimeToday >= STREAK_THRESHOLD_MS) {
            // Increment Streak in Firestore
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("Users").document(uid)
                        .update("streakCount", FieldValue.increment(1))
                        .addOnSuccessListener(aVoid -> {
                            prefs.edit().putBoolean("updated_" + today, true).apply();
                        });
            }
        }
    }

    public long getActiveTimeToday(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("StreakPrefs", Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return prefs.getLong("time_" + today, 0);
    }
}
