package com.example.global_insights;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class NotificationsActivity extends AppCompatActivity {

    private SwitchMaterial switchBreakingNews, switchDailyDigest, switchInterests;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        preferences = getSharedPreferences("notification_settings", MODE_PRIVATE);

        switchBreakingNews = findViewById(R.id.switchBreakingNews);
        switchDailyDigest = findViewById(R.id.switchDailyDigest);
        switchInterests = findViewById(R.id.switchInterests);

        // Load saved preferences
        switchBreakingNews.setChecked(preferences.getBoolean("breaking_news", true));
        switchDailyDigest.setChecked(preferences.getBoolean("daily_digest", true));
        switchInterests.setChecked(preferences.getBoolean("topic_interests", true));

        // Save on change
        switchBreakingNews.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("breaking_news", isChecked).apply();
            Toast.makeText(this, isChecked ? "Breaking news alerts enabled" : "Breaking news alerts disabled", Toast.LENGTH_SHORT).show();
        });

        switchDailyDigest.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("daily_digest", isChecked).apply();
            Toast.makeText(this, isChecked ? "Daily digest enabled" : "Daily digest disabled", Toast.LENGTH_SHORT).show();
        });

        switchInterests.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("topic_interests", isChecked).apply();
            Toast.makeText(this, isChecked ? "Topic recommendations enabled" : "Topic recommendations disabled", Toast.LENGTH_SHORT).show();
        });
    }
}
