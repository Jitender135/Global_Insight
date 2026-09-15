package com.example.global_insights;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

public class ContactActivity extends AppCompatActivity {

    private TextInputEditText subjectInput, messageInput;
    private Button sendButton;
    private MaterialCardView cardEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        subjectInput = findViewById(R.id.subjectInput);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        cardEmail = findViewById(R.id.cardEmail);

        cardEmail.setOnClickListener(v -> sendEmail("General Inquiry", ""));

        sendButton.setOnClickListener(v -> {
            String subject = subjectInput.getText().toString().trim();
            String message = messageInput.getText().toString().trim();

            if (TextUtils.isEmpty(subject)) {
                subjectInput.setError("Please enter a subject");
                return;
            }

            if (TextUtils.isEmpty(message)) {
                messageInput.setError("Please enter your message");
                return;
            }

            sendEmail(subject, message);
        });
    }

    private void sendEmail(String subject, String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:support@globalinsights.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, message);

        try {
            startActivity(Intent.createChooser(intent, "Send Email via..."));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show();
        }
    }
}
