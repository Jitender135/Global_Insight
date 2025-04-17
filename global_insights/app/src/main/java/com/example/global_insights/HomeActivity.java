package com.example.global_insights;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.global_insights.Adapter.NewsAdapter;
import com.example.global_insights.model.Article;
import com.example.global_insights.model.NewsResponse;
import com.example.global_insights.Network.ApiClient;
import com.example.global_insights.Network.NewsApiService;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private static final List<String> categories = Arrays.asList(
            "My Feed", "All News", "Trending", "Unread", "Categories"
    );

    private TabLayout tabLayout;
    private RecyclerView newsRecyclerView;
    private NewsAdapter newsAdapter;
    private List<Article> newsList = new ArrayList<>();
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView settingButton;

    private final String API_KEY = "8cc9b4a1207543338dbc3f4a0da633d6"; // 🔑 Replace with your NewsAPI key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tabLayout = findViewById(R.id.tabLayout);
        newsRecyclerView = findViewById(R.id.newsRecyclerView);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        settingButton = findViewById(R.id.settings_button); // assuming this is your settings icon

        newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        newsAdapter = new NewsAdapter(this, newsList);
        newsRecyclerView.setAdapter(newsAdapter);

        // Open sidebar when setting icon is clicked
        settingButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Set up NavigationView item click listener
        navigationView.setNavigationItemSelectedListener(this::handleNavigationItemSelected);

        // Add tabs
        for (String category : categories) {
            tabLayout.addTab(tabLayout.newTab().setText(category));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(@NonNull TabLayout.Tab tab) {
                String selectedTab = tab.getText().toString();

                switch (selectedTab) {
                    case "My Feed":
                        fetchNews("technology");
                        break;
                    case "All News":
                        fetchNews("latest");
                        break;
                    case "Trending":
                        fetchNews("trending");
                        break;
                    case "Unread":
                        fetchNews("science");
                        break;
                    case "Categories":
                        fetchNews("sports");
                        break;
                    default:
                        fetchNews("latest");
                        break;
                }
            }

            @Override
            public void onTabUnselected(@NonNull TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(@NonNull TabLayout.Tab tab) {
            }
        });

        // Load default
        fetchNews("technology");

        // Fetch and display user's full name in sidebar
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String fullName = snapshot.child("fullName").getValue(String.class);

                        // Get header view and set full name
                        View headerView = navigationView.getHeaderView(0);
                        TextView fullNameTextView = headerView.findViewById(R.id.sidebarFullName);
                        fullNameTextView.setText(fullName);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Firebase", "Failed to read full name: " + error.getMessage());
                }
            });
        }
    }

    private void fetchNews(String query) {
        // Get the selected language from SharedPreferences
        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        String languageCode = preferences.getString("selected_language", "en"); // Default to "en" (English)

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getEverything(query, API_KEY, languageCode); // Pass language code

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    newsList.clear();
                    newsList.addAll(response.body().getArticles());
                    newsAdapter.notifyDataSetChanged();
                    Log.d("NewsAPI", "Articles: " + newsList.toString());
                } else {
                    Log.e("NewsAPI", "Code: " + response.code() + ", Error: " + response.message());
                    Toast.makeText(HomeActivity.this, "Failed to fetch news. Code: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("NewsAPI", "Failure", t);
            }
        });
    }

    private boolean handleNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_language) {
            // Language selection logic
            showLanguageSelectionDialog();  // Show language selection dialog
        } else if (id == R.id.nav_location) {
            // Location selection logic
            showLocationSelectionDialog();  // Show location selection dialog
        } else if (id == R.id.nav_notifications) {
            Toast.makeText(this, "Notifications settings", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_about) {
            Toast.makeText(this, "About Us", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_contact) {
            Toast.makeText(this, "Contact Us", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_feedback) {
            Toast.makeText(this, "Feedback", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_rate) {
            rateApp();
        } else if (id == R.id.nav_dark_theme) {
            // Dark theme toggle logic
            toggleDarkMode();
        } else if (id == R.id.nav_signout) {
            signOutUser();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showLanguageSelectionDialog() {
        String[] languages = {"English", "Spanish", "French"};
        new AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setItems(languages, (dialog, which) -> {
                    String languageCode = "en"; // Default is English
                    if (which == 1) languageCode = "es"; // Spanish
                    else if (which == 2) languageCode = "fr"; // French

                    // Save the selected language in SharedPreferences
                    SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("selected_language", languageCode);
                    editor.apply();

                    // Apply the selected language and recreate the activity
                    LocaleHelper.setLocale(this, languageCode);
                    recreate(); // Recreate activity to apply the new language
                })
                .show();
    }

    private void showLocationSelectionDialog() {
        String[] locations = {"New York", "London", "Paris"};
        new AlertDialog.Builder(this)
                .setTitle("Select Location")
                .setItems(locations, (dialog, which) -> {
                    String selectedLocation = locations[which];
                    // Show a Toast with the selected location
                    Toast.makeText(this, "Selected Location: " + selectedLocation, Toast.LENGTH_SHORT).show();
                    // You can also store the selected location in SharedPreferences if needed.
                })
                .show();
    }

    private void rateApp() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void signOutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void toggleDarkMode() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // Set to Light Mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            // Set to Dark Mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }
}
