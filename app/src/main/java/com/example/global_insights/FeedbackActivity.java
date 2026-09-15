package com.example.global_insights;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private TextInputEditText commentsInput;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ratingBar = findViewById(R.id.ratingBar);
        commentsInput = findViewById(R.id.feedbackCommentsInput);
        submitButton = findViewById(R.id.submitFeedbackButton);

        submitButton.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
        float rating = ratingBar.getRating();
        String comments = commentsInput.getText().toString().trim();

        if (TextUtils.isEmpty(comments)) {
            commentsInput.setError("Please write a few words of feedback.");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userEmail = currentUser != null ? currentUser.getEmail() : "Anonymous";
        String userId = currentUser != null ? currentUser.getUid() : "Anonymous";

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("feedback");
        String feedbackId = ref.push().getKey();

        Map<String, Object> feedbackMap = new HashMap<>();
        feedbackMap.put("userId", userId);
        feedbackMap.put("userEmail", userEmail);
        feedbackMap.put("rating", rating);
        feedbackMap.put("comments", comments);
        feedbackMap.put("timestamp", System.currentTimeMillis());

        if (feedbackId != null) {
            ref.child(feedbackId).setValue(feedbackMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
