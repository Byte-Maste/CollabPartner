package com.example.realtime_data_firebase;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import java.util.HashMap;

public class MyApplication extends Application implements DefaultLifecycleObserver {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Enable Offline Persistence explicitly (enabled by default in newer SDKs,
        // but good to be sure)
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);

        // 2. Register Lifecycle Observer for App-Wide Online/Offline Status
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        // App comes to foreground
        updateStatus("Online");
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // App goes to background or closed
        updateStatus("Offline");
    }

    private void updateStatus(String status) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            HashMap<String, Object> map = new HashMap<>();
            map.put("status", status);
            if ("Offline".equals(status)) {
                map.put("lastSeen", System.currentTimeMillis());
            }

            FirebaseFirestore.getInstance().collection("Users").document(uid).update(map);
        }
    }
}
