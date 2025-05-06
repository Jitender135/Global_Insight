package com.example.global_insights;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterestActivity extends AppCompatActivity {

    private Button btnSaveInterests;
    private final Map<String, CheckBox> interestMap = new HashMap<>();
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interests); // Make sure this XML exists

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        btnSaveInterests = findViewById(R.id.btnSaveInterests);

        // Bind all checkboxes to their category
        interestMap.put("Politics", findViewById(R.id.politicsCheckbox));
        interestMap.put("Business", findViewById(R.id.businessCheckbox));
        interestMap.put("Culture", findViewById(R.id.cultureCheckbox));
        interestMap.put("Health", findViewById(R.id.healthCheckbox));
        interestMap.put("Sports", findViewById(R.id.sportsCheckbox));
        interestMap.put("Technology", findViewById(R.id.technologyCheckbox));
        interestMap.put("Nature", findViewById(R.id.scienceCheckbox)); // ID in XML might be scienceCheckbox
        interestMap.put("Entertainment", findViewById(R.id.entertainmentCheckbox));

        // Toggle checkbox state when parent card is clicked
        for (Map.Entry<String, CheckBox> entry : interestMap.entrySet()) {
            CheckBox checkBox = entry.getValue();
            View parent = (View) checkBox.getParent().getParent(); // Adjust if needed
            parent.setOnClickListener(v -> checkBox.setChecked(!checkBox.isChecked()));
        }

        btnSaveInterests.setOnClickListener(v -> {
            List<String> selectedInterests = new ArrayList<>();
            for (Map.Entry<String, CheckBox> entry : interestMap.entrySet()) {
                if (entry.getValue().isChecked()) {
                    selectedInterests.add(entry.getKey());
                }
            }

            if (selectedInterests.isEmpty()) {
                Toast.makeText(this, "Please select at least one interest.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                mDatabase.child("users").child(userId).child("interests")
                        .setValue(selectedInterests)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Interests saved!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this, "Failed to save interests", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
