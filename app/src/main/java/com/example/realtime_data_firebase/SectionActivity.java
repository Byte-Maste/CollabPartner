package com.example.realtime_data_firebase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

public class SectionActivity extends AppCompatActivity {

    TabLayout tabLayout;
    ViewPager2 viewPager;
    SectionPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_section);

        MaterialToolbar toolbar = findViewById(R.id.topBar);
        setSupportActionBar(toolbar);

        ImageView btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        adapter = new SectionPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {

            View custom = getLayoutInflater().inflate(R.layout.custom_tab, null);
            TextView tabText = custom.findViewById(R.id.tabText);

            switch (position) {
                case 0:
                    tabText.setText("Groups");
                    break;
                case 1:
                    tabText.setText("Project");
                    break;
                case 2:
                    tabText.setText("Jobs");
                    break;
                case 3:
                    tabText.setText("Collab");
                    break;
                case 4:
                    tabText.setText("Events");
                    break;
                case 5:
                    tabText.setText("Profile");
                    break;
                case 6:
                    tabText.setText("Learn");
                    break;
            }

            tab.setCustomView(custom);
        }).attach();

        setupStreak();

        // Refresh profile counts when app starts
        ProfileCountManager.getInstance().refreshAllCounts();
    }

    private void setupStreak() {
        TextView streakText = findViewById(R.id.txtStreak);
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("Users").document(uid)
                    .addSnapshotListener((value, error) -> {
                        if (error != null)
                            return;
                        if (value != null && value.contains("streakCount")) {
                            long streak = value.getLong("streakCount");
                            streakText.setText(streak + " 🔥");
                        } else {
                            streakText.setText("1 🔥"); // Default logic fallback
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        StreakManager.getInstance().startTracking(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        StreakManager.getInstance().stopTracking(this);
    }
}
