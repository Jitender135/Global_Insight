package com.example.global_insights;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.example.global_insights.model.Report;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ReportNewsActivity extends AppCompatActivity {

    private RadioGroup reasonGroup;
    private EditText fullExplanationInput, otherReasonInput;
    private Button submitReportButton;
    private String articleId;
    private String articleTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_news);

        // Initialize views
        reasonGroup = findViewById(R.id.reasonGroup);
        fullExplanationInput = findViewById(R.id.fullExplanationInput);
        otherReasonInput = findViewById(R.id.otherReasonInput);
        submitReportButton = findViewById(R.id.submitReportButton);

        // Get article details passed from the previous activity
        articleId = getIntent().getStringExtra("article_id");
        articleTitle = getIntent().getStringExtra("article_title");

        // Submit button logic
        submitReportButton.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        String reason = getSelectedReason();
        String explanation = fullExplanationInput.getText().toString().trim();

        if (explanation.isEmpty()) {
            showPopup("Please provide a detailed explanation.");
            return;
        }

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        // First get user's full name from Firebase
        userRef.child("fullName").get().addOnSuccessListener(snapshot -> {
            String fullName = snapshot.getValue(String.class);

            // Now save report with email, full name, reason, and explanation
            Report report = new Report(email, fullName, reason, explanation);

            DatabaseReference reportRef = FirebaseDatabase.getInstance().getReference("reports")
                    .child(articleId)
                    .child(email.replace(".", "_")); // Firebase-safe

            reportRef.setValue(report).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    showPopup("Report submitted successfully.");
                } else {
                    showPopup("Failed to submit report. Please try again.");
                }
            });
        }).addOnFailureListener(e -> showPopup("Could not fetch user info."));
    }


    private String getSelectedReason() {
        int selectedId = reasonGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.fakeNewsRadio) {
            return "Fake News";
        } else if (selectedId == R.id.clickbaitRadio) {
            return "Clickbait";
        } else if (selectedId == R.id.hateSpeechRadio) {
            return "Hate Speech";
        } else if (selectedId == R.id.misinformationRadio) {
            return "Misinformation";
        } else {
            return otherReasonInput.getText().toString();
        }
    }

    private void showPopup(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Report Status")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    if (message.equals("Report submitted successfully.")) {
                        finish(); // Go back to previous screen (e.g., home page)
                    }
                })
                .show();
    }
}
