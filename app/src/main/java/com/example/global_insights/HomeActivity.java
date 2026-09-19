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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Build;
import android.speech.tts.TextToSpeech;
import androidx.recyclerview.widget.PagerSnapHelper;
import java.util.Locale;

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
import com.example.global_insights.Adapter.CategoryAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.pm.PackageManager;
import com.example.global_insights.model.SavedLocation;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import org.json.JSONObject;
import android.widget.ProgressBar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefreshLayout;

    private List<String> getCategories() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return Arrays.asList("All News", "Trending", "National", "International", "Local News", "Around Me (10km)", "Categories");
        } else {
            return Arrays.asList("My Feed", "All News", "Trending", "National", "International", "Local News", "Around Me (10km)", "Categories");
        }
    }

    private LinearLayout radiusLocationBanner;
    private ImageView ivRadiusBannerPin;
    private TextView tvRadiusBannerIcon;
    private TextView tvRadiusBannerMode;
    private TextView tvRadiusBannerLocation;
    private TextView btnSaveRadiusLocation;
    private TextView btnSwitchRadiusPlaces;
    private ImageView btnRefreshRadiusGps;
    private View layoutPostNoticeBanner;
    private TextView btnPostCommunityNotice;

    // Current GPS cache for quick saving
    private double currentGpsLat = 0.0;
    private double currentGpsLon = 0.0;
    private String currentGpsAddress = "";
    private String currentGpsSubLocality = "";
    private String currentGpsTehsil = "";
    private String currentGpsLocality = "";
    private String currentGpsDistrict = "";
    private String currentGpsState = "";
    private String currentGpsPostalCode = "";


    private TabLayout tabLayout;
    private RecyclerView newsRecyclerView;
    private RecyclerView categoryRecyclerView;
    private NewsAdapter newsAdapter;
    private List<Article> newsList = new ArrayList<>();
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView settingButton;

    private final String API_KEY = "316356d807a14581b154282e64386d25"; // 🔑 User's NewsAPI Key

    private final Map<String, List<Article>> tabArticleCache = new HashMap<>();
    private final Map<String, Long> tabCacheTime = new HashMap<>();
    private static final long CACHE_DURATION_MS = 15 * 60 * 1000; // 15 minutes cache

    private boolean isCacheValid(String cacheKey) {
        if (!tabArticleCache.containsKey(cacheKey) || !tabCacheTime.containsKey(cacheKey)) {
            return false;
        }
        long age = System.currentTimeMillis() - tabCacheTime.get(cacheKey);
        return age < CACHE_DURATION_MS && tabArticleCache.get(cacheKey) != null && !tabArticleCache.get(cacheKey).isEmpty();
    }

    private void saveToCache(String cacheKey, List<Article> articles) {
        if (articles != null && !articles.isEmpty()) {
            tabArticleCache.put(cacheKey, new ArrayList<>(articles));
            tabCacheTime.put(cacheKey, System.currentTimeMillis());
        }
    }

    private TextToSpeech textToSpeech;
    private boolean isTtsInitialized = false;
    private PagerSnapHelper snapHelper = new PagerSnapHelper();
    private boolean isSnapHelperAttached = false;
    private String currentlySpeakingTitle = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tabLayout = findViewById(R.id.tabLayout);
        newsRecyclerView = findViewById(R.id.newsRecyclerView);
        categoryRecyclerView = findViewById(R.id.categoryRecyclerView);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        settingButton = findViewById(R.id.settings_button);

        newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        newsAdapter = new NewsAdapter(this, newsList);
        SharedPreferences userPrefs = getSharedPreferences("user_preferences", MODE_PRIVATE);
        boolean isDarkMode = userPrefs.getBoolean("dark_mode", false);
        newsAdapter.setDarkMode(isDarkMode);
        newsAdapter.setOnAudioClickListener(article -> toggleAudioSpeech(article));
        newsRecyclerView.setAdapter(newsAdapter);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                tabArticleCache.clear();
                tabCacheTime.clear();
                int selectedTabPos = tabLayout.getSelectedTabPosition();
                loadHomeTabContent(selectedTabPos >= 0 ? selectedTabPos : 0);
                fetchBreakingNewsTicker();
            });
        }

        // Initialize Android TextToSpeech engine
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
                isTtsInitialized = true;
            }
        });

        // Initialize top Breaking News ticker
        fetchBreakingNewsTicker();

        // Initialize Radius News Location Banner & Actions
        radiusLocationBanner = findViewById(R.id.radiusLocationBanner);
        ivRadiusBannerPin = findViewById(R.id.ivRadiusBannerPin);
        tvRadiusBannerIcon = findViewById(R.id.tvRadiusBannerIcon);
        tvRadiusBannerMode = findViewById(R.id.tvRadiusBannerMode);
        tvRadiusBannerLocation = findViewById(R.id.tvRadiusBannerLocation);
        btnSaveRadiusLocation = findViewById(R.id.btnSaveRadiusLocation);
        btnSwitchRadiusPlaces = findViewById(R.id.btnSwitchRadiusPlaces);
        btnRefreshRadiusGps = findViewById(R.id.btnRefreshRadiusGps);

        if (btnSaveRadiusLocation != null) {
            btnSaveRadiusLocation.setOnClickListener(v -> showSaveLocationDialog());
        }
        if (btnSwitchRadiusPlaces != null) {
            btnSwitchRadiusPlaces.setOnClickListener(v -> showSavedLocationsBottomSheet());
        }
        if (btnRefreshRadiusGps != null) {
            btnRefreshRadiusGps.setOnClickListener(v -> {
                Toast.makeText(this, "Refreshing GPS location...", Toast.LENGTH_SHORT).show();
                SavedLocationManager.setActiveToLiveGps(this);
                loadRadiusNewsTab(true);
            });
        }

        layoutPostNoticeBanner = findViewById(R.id.layoutPostNoticeBanner);
        btnPostCommunityNotice = findViewById(R.id.btnPostCommunityNotice);
        View.OnClickListener openPostNoticeListener = v -> showPostCommunityNoticeDialog();
        if (layoutPostNoticeBanner != null) layoutPostNoticeBanner.setOnClickListener(openPostNoticeListener);
        if (btnPostCommunityNotice != null) btnPostCommunityNotice.setOnClickListener(openPostNoticeListener);

        SavedLocationManager.setActiveToLiveGps(this);
        updatePlacesButtonBadge();
        SavedLocationManager.loadFromFirebaseIfAvailable(this);

        // Setup Category Grid & Back Header
        LinearLayout categoryHeaderBar = findViewById(R.id.categoryHeaderBar);

        ImageView btnCategoryBack = findViewById(R.id.btnCategoryBack);
        TextView btnBackToGrid = findViewById(R.id.btnBackToGrid);
        TextView tvCategoryHeaderTitle = findViewById(R.id.tvCategoryHeaderTitle);

        categoryRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        List<CategoryAdapter.CategoryItem> categoryItems = Arrays.asList(
                new CategoryAdapter.CategoryItem("Politics", "politics", R.drawable.politics),
                new CategoryAdapter.CategoryItem("Business", "business", R.drawable.buissness),
                new CategoryAdapter.CategoryItem("Technology", "technology", R.drawable.technology),
                new CategoryAdapter.CategoryItem("Sports", "sports", R.drawable.sports),
                new CategoryAdapter.CategoryItem("Health", "health", R.drawable.healthy),
                new CategoryAdapter.CategoryItem("Entertainment", "entertainment", R.drawable.entairment),
                new CategoryAdapter.CategoryItem("Nature", "science", R.drawable.nature),
                new CategoryAdapter.CategoryItem("Culture", "general", R.drawable.culture)
        );
        TextView emptyStateText = findViewById(R.id.emptyStateText);
        CategoryAdapter categoryAdapter = new CategoryAdapter(this, categoryItems, item -> {
            categoryRecyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.GONE);
            categoryHeaderBar.setVisibility(View.VISIBLE);
            tvCategoryHeaderTitle.setText("Category: " + item.name);
            newsRecyclerView.setVisibility(View.VISIBLE);
            fetchIndianNews(item.apiKey);
        });
        categoryRecyclerView.setAdapter(categoryAdapter);

        View.OnClickListener returnToGridListener = v -> {
            categoryHeaderBar.setVisibility(View.GONE);
            newsRecyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.GONE);
            categoryRecyclerView.setVisibility(View.VISIBLE);
        };
        btnCategoryBack.setOnClickListener(returnToGridListener);
        btnBackToGrid.setOnClickListener(returnToGridListener);

        settingButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(this::handleNavigationItemSelected);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        LinearLayout searchBarContainer = findViewById(R.id.searchBarContainer);
        EditText searchEditText = findViewById(R.id.searchEditText);
        ImageView searchSubmitButton = findViewById(R.id.searchSubmitButton);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            categoryHeaderBar.setVisibility(View.GONE);
            if (id == R.id.nav_home) {
                searchBarContainer.setVisibility(View.GONE);
                tabLayout.setVisibility(View.VISIBLE);
                int selectedTabPos = tabLayout.getSelectedTabPosition();
                loadHomeTabContent(selectedTabPos >= 0 ? selectedTabPos : 0);
                return true;
            } else if (id == R.id.nav_search) {
                searchBarContainer.setVisibility(View.VISIBLE);
                tabLayout.setVisibility(View.GONE);
                categoryRecyclerView.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.GONE);
                newsRecyclerView.setVisibility(View.VISIBLE);
                searchEditText.requestFocus();
                String currentQuery = searchEditText.getText().toString().trim();
                if (!currentQuery.isEmpty()) {
                    performSearch(currentQuery);
                } else {
                    newsList.clear();
                    newsAdapter.notifyDataSetChanged();
                }
                return true;
            } else if (id == R.id.nav_bookmark) {
                searchBarContainer.setVisibility(View.GONE);
                tabLayout.setVisibility(View.GONE);
                categoryRecyclerView.setVisibility(View.GONE);
                newsRecyclerView.setVisibility(View.VISIBLE);
                loadBookmarkedNews();
                return true;
            }
            return false;
        });

        searchSubmitButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            } else {
                Toast.makeText(HomeActivity.this, "Please enter a search keyword", Toast.LENGTH_SHORT).show();
            }
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                }
                return true;
            }
            return false;
        });

        List<String> categoriesList = getCategories();
        tabLayout.removeAllTabs();
        for (String category : categoriesList) {
            tabLayout.addTab(tabLayout.newTab().setText(category));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(@NonNull TabLayout.Tab tab) {
                loadHomeTabContent(tab.getPosition());
            }

            @Override
            public void onTabUnselected(@NonNull TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(@NonNull TabLayout.Tab tab) {
                loadHomeTabContent(tab.getPosition());
            }
        });

        loadHomeTabContent(0);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String fullName = snapshot.child("fullName").getValue(String.class);
                        View headerView = navigationView.getHeaderView(0);
                        if (headerView != null) {
                            TextView fullNameTextView = headerView.findViewById(R.id.sidebarFullName);
                            if (fullNameTextView != null) {
                                fullNameTextView.setText(fullName != null ? fullName : "User");
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Firebase", "Failed to read full name: " + error.getMessage());
                }
            });
        } else {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView fullNameTextView = headerView.findViewById(R.id.sidebarFullName);
                if (fullNameTextView != null) {
                    fullNameTextView.setText("Guest");
                }
            }
        }
    }

    private void loadHomeTabContent(int position) {
        TextView emptyStateText = findViewById(R.id.emptyStateText);
        LinearLayout categoryHeaderBar = findViewById(R.id.categoryHeaderBar);
        if (categoryHeaderBar != null) {
            categoryHeaderBar.setVisibility(View.GONE);
        }
        if (radiusLocationBanner != null) {
            radiusLocationBanner.setVisibility(View.GONE);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isGuest = (currentUser == null);

        if (isGuest) {
            enableSnapScrolling(false);
            switch (position) {
                case 0: // All News
                    categoryRecyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.GONE);
                    newsRecyclerView.setVisibility(View.VISIBLE);
                    newsAdapter.setInshortsStyle(false);
                    fetchIndianNews(null);
                    break;
                case 1: // Trending
                    categoryRecyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.GONE);
                    newsRecyclerView.setVisibility(View.VISIBLE);
                    newsAdapter.setInshortsStyle(false);
                    fetchTrendingNews();
                    break;
                case 2: // National
                    categoryRecyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.GONE);
                    newsRecyclerView.setVisibility(View.VISIBLE);
                    newsAdapter.setInshortsStyle(false);
                    fetchNationalNews();
                    break;
                case 3: // International
                    categoryRecyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.GONE);
                    newsRecyclerView.setVisibility(View.VISIBLE);
                    newsAdapter.setInshortsStyle(false);
                    fetchInternationalNews();
                    break;
                case 4: // Local News
                    categoryRecyclerView.setVisibility(View.GONE);
                    newsAdapter.setInshortsStyle(false);
                    SharedPreferences userPrefsLoc = getSharedPreferences("user_preferences", MODE_PRIVATE);
                    String savedState = userPrefsLoc.getString("selected_state", null);
                    String savedDistrict = userPrefsLoc.getString("selected_district", null);

                    if (savedState != null || savedDistrict != null) {
                        emptyStateText.setVisibility(View.GONE);
                        newsRecyclerView.setVisibility(View.VISIBLE);
                        fetchLocationNews(savedState, savedDistrict);
                    } else {
                        newsRecyclerView.setVisibility(View.GONE);
                        emptyStateText.setText("No location selected yet.\nTap here to select your State & District!");
                        emptyStateText.setVisibility(View.VISIBLE);
                        emptyStateText.setOnClickListener(v -> showLocationSelectionDialog());
                    }
                    break;
                case 5: // Around Me (10km Radius)
                    SavedLocationManager.setActiveToLiveGps(this);
                    loadRadiusNewsTab(true);
                    break;
                case 6: // Categories
                    newsRecyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.GONE);
                    categoryRecyclerView.setVisibility(View.VISIBLE);
                    break;
                default:
                    categoryRecyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.GONE);
                    newsRecyclerView.setVisibility(View.VISIBLE);
                    newsAdapter.setInshortsStyle(false);
                    fetchIndianNews(null);
                    break;
            }
        } else {
            switch (position) {
                case 0: // My Feed
                    categoryRecyclerView.setVisibility(View.GONE);
                    enableSnapScrolling(true);
                    fetchMyFeedNews();
                    break;
                case 1: // All News
                    categoryRecyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.GONE);
                    newsRecyclerView.setVisibility(View.VISIBLE);
                    newsAdapter.setInshortsStyle(false);
                    enableSnapScrolling(false);
                    fetchIndianNews(null);
                    break;
                case 2: // Trending
                    categoryRecyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.GONE);
                    newsRecyclerView.setVisibility(View.VISIBLE);
                    newsAdapter.setInshortsStyle(false);
                    enableSnapScrolling(false);
                    fetchTrendingNews();
                    break;
                case 3: // National
                    categoryRecyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.GONE);
                    newsRecyclerView.setVisibility(View.VISIBLE);
                    newsAdapter.setInshortsStyle(false);
                    enableSnapScrolling(false);
                    fetchNationalNews();
                    break;
                case 4: // International
                    categoryRecyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.GONE);
                    newsRecyclerView.setVisibility(View.VISIBLE);
                    newsAdapter.setInshortsStyle(false);
                    enableSnapScrolling(false);
                    fetchInternationalNews();
                    break;
                case 5: // Local News
                    categoryRecyclerView.setVisibility(View.GONE);
                    newsAdapter.setInshortsStyle(false);
                    enableSnapScrolling(false);
                    SharedPreferences userPrefsLoc = getSharedPreferences("user_preferences", MODE_PRIVATE);
                    String savedState = userPrefsLoc.getString("selected_state", null);
                    String savedDistrict = userPrefsLoc.getString("selected_district", null);

                    if (savedState != null || savedDistrict != null) {
                        emptyStateText.setVisibility(View.GONE);
                        newsRecyclerView.setVisibility(View.VISIBLE);
                        fetchLocationNews(savedState, savedDistrict);
                    } else {
                        newsRecyclerView.setVisibility(View.GONE);
                        emptyStateText.setText("No location selected yet.\nTap here to select your State & District!");
                        emptyStateText.setVisibility(View.VISIBLE);
                        emptyStateText.setOnClickListener(v -> showLocationSelectionDialog());
                    }
                    break;
                case 6: // Around Me (10km Radius)
                    SavedLocationManager.setActiveToLiveGps(this);
                    loadRadiusNewsTab(true);
                    break;
                case 7: // Categories
                    newsRecyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.GONE);
                    enableSnapScrolling(false);
                    categoryRecyclerView.setVisibility(View.VISIBLE);
                    break;
                default:
                    categoryRecyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.GONE);
                    newsRecyclerView.setVisibility(View.VISIBLE);
                    newsAdapter.setInshortsStyle(false);
                    enableSnapScrolling(false);
                    fetchIndianNews(null);
                    break;
            }
        }
    }


    private String getDefaultSearchQueryForLanguage(String langCode, String category) {
        if (category != null && !category.isEmpty()) {
            return category;
        }
        if (langCode == null) return "India";

        switch (langCode.toLowerCase()) {
            case "hi": return "भारत";
            case "bn": return "খবর";
            case "fr": return "actualités";
            case "ru": return "новости";
            case "es": return "noticias";
            case "de": return "nachrichten";
            case "it": return "notizie";
            case "ar": return "أخبار";
            case "pt": return "notícias";
            case "ja": return "ニュース";
            case "zh": return "新闻";
            default: return "India";
        }
    }

    private void setSanitizedArticles(List<Article> rawArticles) {
        newsList.clear();
        if (rawArticles != null) {
            for (Article a : rawArticles) {
                if (isValidArticle(a)) {
                    newsList.add(a);
                }
            }
        }
        newsAdapter.notifyDataSetChanged();
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tabLayout != null) {
            int selectedTabPos = tabLayout.getSelectedTabPosition();
            loadHomeTabContent(selectedTabPos >= 0 ? selectedTabPos : 0);
        }
        fetchBreakingNewsTicker();
    }

    private boolean isValidArticle(Article a) {
        if (a == null) return false;
        String title = a.getTitle();
        if (title == null || title.trim().isEmpty()) return false;
        
        String lowerTitle = title.trim().toLowerCase();
        if (lowerTitle.equals("news") || lowerTitle.equals("comments") || lowerTitle.contains("[removed]") || lowerTitle.length() < 5) {
            return false;
        }

        String desc = a.getDescription();
        if (desc != null) {
            String lowerDesc = desc.trim().toLowerCase();
            if (lowerDesc.equals("comments") || lowerDesc.contains("[removed]")) {
                a.setDescription(null);
            }
        }
        return true;
    }

    private void fetchMyFeedNews() {
        TextView emptyStateText = findViewById(R.id.emptyStateText);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            newsList.clear();
            newsAdapter.notifyDataSetChanged();
            newsRecyclerView.setVisibility(View.GONE);
            emptyStateText.setText("🔒 My Feed is available for logged-in users only.\n\nPlease Sign In or Register to view your personalized news feed!");
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateText.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, SignInActivity.class);
                startActivity(intent);
            });
            return;
        }

        emptyStateText.setVisibility(View.GONE);
        newsRecyclerView.setVisibility(View.VISIBLE);

        String uid = currentUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> interests = new ArrayList<>();
                if (snapshot.exists() && snapshot.hasChild("interests")) {
                    for (DataSnapshot child : snapshot.child("interests").getChildren()) {
                        String interest = child.getValue(String.class);
                        if (interest != null && !interest.trim().isEmpty()) {
                            interests.add(interest.trim());
                        }
                    }
                }

                String query;
                if (!interests.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < interests.size(); i++) {
                        if (i > 0) sb.append(" OR ");
                        sb.append(interests.get(i));
                    }
                    query = sb.toString();
                } else {
                    query = "India news trending";
                }

                SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
                String languageCode = preferences.getString("selected_language", "en");

                NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
                Call<NewsResponse> call = apiService.getEverything(query, API_KEY, languageCode);

                call.enqueue(new Callback<NewsResponse>() {
                    @Override
                    public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null && !response.body().getArticles().isEmpty()) {
                            setSanitizedArticles(response.body().getArticles());
                            newsAdapter.setInshortsStyle(true);
                            newsRecyclerView.setVisibility(View.VISIBLE);
                            emptyStateText.setVisibility(View.GONE);
                        } else {
                            fetchEverythingIndia("breaking news");
                        }
                    }

                    @Override
                    public void onFailure(Call<NewsResponse> call, Throwable t) {
                        fetchEverythingIndia("breaking news");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                fetchEverythingIndia("breaking news");
            }
        });
    }

    private void performSearch(String query) {
        TextView emptyStateText = findViewById(R.id.emptyStateText);
        emptyStateText.setVisibility(View.GONE);

        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        String languageCode = preferences.getString("selected_language", "en");

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getEverything(query, API_KEY, languageCode);

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null && !response.body().getArticles().isEmpty()) {
                    setSanitizedArticles(response.body().getArticles());
                    newsRecyclerView.setVisibility(View.VISIBLE);
                    emptyStateText.setVisibility(View.GONE);
                } else {
                    newsList.clear();
                    newsAdapter.notifyDataSetChanged();
                    emptyStateText.setText("No news found for \"" + query + "\"");
                    emptyStateText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBookmarkedNews() {
        TextView emptyStateText = findViewById(R.id.emptyStateText);
        List<Article> bookmarks = BookmarkManager.getBookmarks(this);
        setSanitizedArticles(bookmarks);

        if (newsList.isEmpty()) {
            emptyStateText.setText("No bookmarked articles yet.\nTap the bookmark icon on any news card to save it for later!");
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            emptyStateText.setVisibility(View.GONE);
        }
    }

    private void fetchIndianNews(String category) {
        String cacheKey = "indian_" + (category != null ? category : "all");
        if (isCacheValid(cacheKey)) {
            setSanitizedArticles(tabArticleCache.get(cacheKey));
            return;
        }

        TextView emptyStateText = findViewById(R.id.emptyStateText);
        emptyStateText.setVisibility(View.GONE);

        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        String languageCode = preferences.getString("selected_language", "en");

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call;
        
        if (!"en".equalsIgnoreCase(languageCode)) {
            String query = getDefaultSearchQueryForLanguage(languageCode, category);
            call = apiService.getEverything(query, API_KEY, languageCode);
        } else if (category != null && !category.isEmpty()) {
            call = apiService.getTopHeadlinesByCategory("in", category, API_KEY);
        } else {
            call = apiService.getTopHeadlines("in", API_KEY);
        }

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null && !response.body().getArticles().isEmpty()) {
                    saveToCache(cacheKey, response.body().getArticles());
                    setSanitizedArticles(response.body().getArticles());
                    Log.d("NewsAPI", "News fetched: " + newsList.size() + " articles for lang: " + languageCode);
                } else {
                    fetchEverythingIndia(getDefaultSearchQueryForLanguage(languageCode, category));
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                fetchEverythingIndia(getDefaultSearchQueryForLanguage(languageCode, category));
            }
        });
    }

    private void fetchTrendingNews() {
        String cacheKey = "trending_world";
        if (isCacheValid(cacheKey)) {
            setSanitizedArticles(tabArticleCache.get(cacheKey));
            return;
        }

        TextView emptyStateText = findViewById(R.id.emptyStateText);
        if (emptyStateText != null) emptyStateText.setVisibility(View.GONE);
        newsRecyclerView.setVisibility(View.VISIBLE);

        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        String languageCode = preferences.getString("selected_language", "en");

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getEverything("trending OR viral OR breaking news", API_KEY, languageCode);

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null && !response.body().getArticles().isEmpty()) {
                    saveToCache(cacheKey, response.body().getArticles());
                    setSanitizedArticles(response.body().getArticles());
                } else {
                    fetchTrendingFallback();
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                fetchTrendingFallback();
            }
        });
    }

    private void fetchTrendingFallback() {
        String cacheKey = "trending_world_fallback";
        if (isCacheValid(cacheKey)) {
            setSanitizedArticles(tabArticleCache.get(cacheKey));
            return;
        }

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getTopHeadlines("us", API_KEY);

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null && !response.body().getArticles().isEmpty()) {
                    saveToCache(cacheKey, response.body().getArticles());
                    setSanitizedArticles(response.body().getArticles());
                } else {
                    fetchIndianNews(null);
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                fetchIndianNews(null);
            }
        });
    }

    private void fetchNationalNews() {
        String cacheKey = "national_india";
        if (isCacheValid(cacheKey)) {
            setSanitizedArticles(tabArticleCache.get(cacheKey));
            return;
        }

        TextView emptyStateText = findViewById(R.id.emptyStateText);
        if (emptyStateText != null) emptyStateText.setVisibility(View.GONE);
        newsRecyclerView.setVisibility(View.VISIBLE);

        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        String languageCode = preferences.getString("selected_language", "en");

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getEverything("India national news OR India government OR Lok Sabha OR Parliament", API_KEY, languageCode);

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null && !response.body().getArticles().isEmpty()) {
                    saveToCache(cacheKey, response.body().getArticles());
                    setSanitizedArticles(response.body().getArticles());
                } else {
                    fetchNationalFallback();
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                fetchNationalFallback();
            }
        });
    }

    private void fetchNationalFallback() {
        String cacheKey = "national_india_fallback";
        if (isCacheValid(cacheKey)) {
            setSanitizedArticles(tabArticleCache.get(cacheKey));
            return;
        }

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getTopHeadlines("in", API_KEY);

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null) {
                    saveToCache(cacheKey, response.body().getArticles());
                    setSanitizedArticles(response.body().getArticles());
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {}
        });
    }

    private void fetchInternationalNews() {
        String cacheKey = "international";
        if (isCacheValid(cacheKey)) {
            setSanitizedArticles(tabArticleCache.get(cacheKey));
            return;
        }

        TextView emptyStateText = findViewById(R.id.emptyStateText);
        emptyStateText.setVisibility(View.GONE);
        newsRecyclerView.setVisibility(View.VISIBLE);

        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        String languageCode = preferences.getString("selected_language", "en");

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getEverything("world", API_KEY, languageCode);

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null && !response.body().getArticles().isEmpty()) {
                    saveToCache(cacheKey, response.body().getArticles());
                    setSanitizedArticles(response.body().getArticles());
                } else {
                    fetchInternationalHeadlinesFallback();
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                fetchInternationalHeadlinesFallback();
            }
        });
    }

    private void fetchInternationalHeadlinesFallback() {
        TextView emptyStateText = findViewById(R.id.emptyStateText);
        emptyStateText.setVisibility(View.GONE);
        newsRecyclerView.setVisibility(View.VISIBLE);

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getTopHeadlines("us", API_KEY);

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null && !response.body().getArticles().isEmpty()) {
                    setSanitizedArticles(response.body().getArticles());
                } else {
                    fetchIndianNews(null);
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                fetchIndianNews(null);
            }
        });
    }

    private void fetchEverythingIndia(String query) {
        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        String languageCode = preferences.getString("selected_language", "en");

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getEverything("India news", API_KEY, languageCode);

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null && !response.body().getArticles().isEmpty()) {
                    setSanitizedArticles(response.body().getArticles());
                } else {
                    apiService.getTopHeadlines("in", API_KEY).enqueue(new Callback<NewsResponse>() {
                        @Override
                        public void onResponse(Call<NewsResponse> c, Response<NewsResponse> r) {
                            if (r.isSuccessful() && r.body() != null && r.body().getArticles() != null) {
                                setSanitizedArticles(r.body().getArticles());
                            }
                        }

                        @Override
                        public void onFailure(Call<NewsResponse> c, Throwable t) {}
                    });
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                apiService.getTopHeadlines("in", API_KEY).enqueue(new Callback<NewsResponse>() {
                    @Override
                    public void onResponse(Call<NewsResponse> c, Response<NewsResponse> r) {
                        if (r.isSuccessful() && r.body() != null && r.body().getArticles() != null) {
                            setSanitizedArticles(r.body().getArticles());
                        }
                    }

                    @Override
                    public void onFailure(Call<NewsResponse> c, Throwable t) {}
                });
            }
        });
    }

    private boolean handleNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_language) {
            showLanguageSelectionDialog();
        } else if (id == R.id.nav_location) {
            showLocationSelectionDialog();
        } else if (id == R.id.nav_notifications) {
            startActivity(new Intent(this, NotificationsActivity.class));
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (id == R.id.nav_contact) {
            startActivity(new Intent(this, ContactActivity.class));
        } else if (id == R.id.nav_feedback) {
            startActivity(new Intent(this, FeedbackActivity.class));
        } else if (id == R.id.nav_rate) {
            rateApp();
        } else if (id == R.id.nav_dark_theme) {
            toggleDarkMode();
        } else if (id == R.id.nav_signout) {
            signOutUser();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showLanguageSelectionDialog() {
        String[] displayLanguages = {
                "English",
                "Hindi (हिंदी)",
                "Bengali (বাংলা)",
                "French (Français)",
                "Russian (Русский)",
                "Spanish (Español)",
                "German (Deutsch)",
                "Italian (Italiano)",
                "Arabic (العربية)",
                "Portuguese (Português)",
                "Japanese (日本語)",
                "Chinese (中文)"
        };

        String[] languageCodes = {
                "en", "hi", "bn", "fr", "ru", "es", "de", "it", "ar", "pt", "ja", "zh"
        };

        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        String currentLang = preferences.getString("selected_language", "en");
        int checkedItem = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equalsIgnoreCase(currentLang)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Language / भाषा चुनें")
                .setSingleChoiceItems(displayLanguages, checkedItem, (dialog, which) -> {
                    String selectedCode = languageCodes[which];
                    String selectedName = displayLanguages[which];

                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("selected_language", selectedCode);
                    editor.apply();

                    LocaleHelper.setLocale(this, selectedCode);
                    Toast.makeText(this, "Language updated: " + selectedName, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Map<String, List<String>> getStateDistrictMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("Maharashtra", Arrays.asList("Mumbai", "Pune", "Nagpur", "Nashik", "Thane", "Chhatrapati Sambhajinagar", "Solapur", "Kolhapur", "Amravati", "Nanded", "Sangli", "Satara", "Jalgaon", "Ahmednagar"));
        map.put("Delhi (NCR)", Arrays.asList("New Delhi", "North Delhi", "South Delhi", "East Delhi", "West Delhi", "Central Delhi", "Gurugram", "Noida"));
        map.put("Uttar Pradesh", Arrays.asList("Lucknow", "Kanpur", "Noida", "Varanasi", "Agra", "Ghaziabad", "Prayagraj", "Meerut", "Bareilly", "Aligarh", "Gorakhpur", "Ayodhya"));
        map.put("Karnataka", Arrays.asList("Bengaluru", "Mysuru", "Hubballi-Dharwad", "Mangaluru", "Belagavi", "Davanagere", "Ballari", "Kalaburagi", "Shivamogga"));
        map.put("Tamil Nadu", Arrays.asList("Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem", "Tiruppur", "Erode", "Vellore", "Tirunelveli"));
        map.put("West Bengal", Arrays.asList("Kolkata", "Howrah", "Durgapur", "Asansol", "Siliguri", "Bardhaman", "Malda", "Kharagpur", "Darjeeling"));
        map.put("Gujarat", Arrays.asList("Ahmedabad", "Surat", "Vadodara", "Rajkot", "Bhavnagar", "Jamnagar", "Junagadh", "Gandhinagar", "Anand"));
        map.put("Rajasthan", Arrays.asList("Jaipur", "Jodhpur", "Udaipur", "Kota", "Ajmer", "Bikaner", "Bhilwara", "Alwar", "Sikar"));
        map.put("Punjab", Arrays.asList("Amritsar", "Ludhiana", "Jalandhar", "Patiala", "Bathinda", "Mohali", "Pathankot"));
        map.put("Haryana", Arrays.asList("Gurugram", "Faridabad", "Panipat", "Ambala", "Karnal", "Hisar", "Rohtak", "Panchkula"));
        map.put("Kerala", Arrays.asList("Thiruvananthapuram", "Kochi", "Kozhikode", "Thrissur", "Kollam", "Palakkad", "Kannur", "Alappuzha"));
        map.put("Bihar", Arrays.asList("Patna", "Gaya", "Bhagalpur", "Muzaffarpur", "Purnia", "Darbhanga", "Bihar Sharif"));
        map.put("Telangana", Arrays.asList("Hyderabad", "Warangal", "Nizamabad", "Karimnagar", "Khammam"));
        map.put("Andhra Pradesh", Arrays.asList("Visakhapatnam", "Vijayawada", "Guntur", "Nellore", "Kurnool", "Tirupati", "Rajahmundry"));
        map.put("Madhya Pradesh", Arrays.asList("Bhopal", "Indore", "Jabalpur", "Gwalior", "Ujjain", "Sagar", "Dewas"));
        map.put("Odisha", Arrays.asList("Bhubaneswar", "Cuttack", "Rourkela", "Puri", "Sambalpur", "Balasore"));
        map.put("Assam", Arrays.asList("Guwahati", "Silchar", "Dibrugarh", "Jorhat", "Nagaon", "Tinsukia"));
        map.put("Jharkhand", Arrays.asList("Ranchi", "Jamshedpur", "Dhanbad", "Bokaro", "Hazaribagh"));
        map.put("Goa", Arrays.asList("Panaji", "Margao", "Vasco da Gama", "Mapusa"));
        map.put("Himachal Pradesh", Arrays.asList("Shimla", "Dharamshala", "Manali", "Solan", "Mandi", "Kullu"));
        map.put("Uttarakhand", Arrays.asList("Dehradun", "Haridwar", "Roorkee", "Haldwani", "Nainital", "Rishikesh"));
        map.put("Chhattisgarh", Arrays.asList("Raipur", "Bhilai", "Bilaspur", "Korba", "Durg"));
        map.put("Jammu & Kashmir", Arrays.asList("Srinagar", "Jammu", "Anantnag", "Baramulla", "Udhampur"));
        return map;
    }

    private void showLocationSelectionDialog() {
        Map<String, List<String>> stateMap = getStateDistrictMap();
        List<String> statesList = new ArrayList<>(stateMap.keySet());
        statesList.add(0, "📍 Clear Location Filter (All India)");

        String[] statesArray = statesList.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Select State / राज्य चुनें")
                .setItems(statesArray, (dialog, which) -> {
                    if (which == 0) {
                        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
                        preferences.edit()
                                .remove("selected_state")
                                .remove("selected_district")
                                .apply();
                        Toast.makeText(this, "Location filter cleared. Showing All News.", Toast.LENGTH_SHORT).show();
                        int selectedTabPos = tabLayout.getSelectedTabPosition();
                        loadHomeTabContent(selectedTabPos >= 0 ? selectedTabPos : 0);
                    } else {
                        String selectedState = statesArray[which];
                        showDistrictSelectionDialog(selectedState, stateMap.get(selectedState));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDistrictSelectionDialog(String state, List<String> districts) {
        if (districts == null) districts = new ArrayList<>();
        List<String> districtList = new ArrayList<>(districts);
        districtList.add(0, "Entire State (" + state + ")");

        String[] districtsArray = districtList.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Select District in " + state)
                .setItems(districtsArray, (dialog, which) -> {
                    String selectedDistrict = (which == 0) ? "" : districtsArray[which];
                    SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
                    preferences.edit()
                            .putString("selected_state", state)
                            .putString("selected_district", selectedDistrict)
                            .apply();

                    String locationLabel = selectedDistrict.isEmpty() ? state : (selectedDistrict + ", " + state);
                    Toast.makeText(this, "Location set to: " + locationLabel, Toast.LENGTH_SHORT).show();

                    boolean isGuestUser = (FirebaseAuth.getInstance().getCurrentUser() == null);
                    int localTabIdx = isGuestUser ? 4 : 5;
                    TabLayout.Tab localTab = tabLayout.getTabAt(localTabIdx);
                    if (localTab != null) {
                        localTab.select();
                    }
                    fetchLocationNews(state, selectedDistrict);
                })
                .setNegativeButton("Back", (dialog, which) -> showLocationSelectionDialog())
                .show();
    }

    private void fetchLocationNews(String state, String district) {
        String cacheKey = "location_" + (state != null ? state : "") + "_" + (district != null ? district : "");
        if (isCacheValid(cacheKey)) {
            setSanitizedArticles(tabArticleCache.get(cacheKey));
            return;
        }

        TextView emptyStateText = findViewById(R.id.emptyStateText);
        emptyStateText.setVisibility(View.GONE);
        newsRecyclerView.setVisibility(View.VISIBLE);

        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        String languageCode = preferences.getString("selected_language", "en");

        String query;
        if (district != null && !district.isEmpty()) {
            query = district + " news";
        } else if (state != null && !state.isEmpty()) {
            query = state + " news";
        } else {
            fetchIndianNews(null);
            return;
        }

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getEverything(query.trim(), API_KEY, languageCode);

        String locationName = (district != null && !district.isEmpty()) ? (district + ", " + state) : state;

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null && !response.body().getArticles().isEmpty()) {
                    saveToCache(cacheKey, response.body().getArticles());
                    setSanitizedArticles(response.body().getArticles());
                } else {
                    fetchLocationNewsFallback(locationName, (district != null && !district.isEmpty()) ? (district + " India") : (state + " India"));
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                fetchLocationNewsFallback(locationName, (district != null && !district.isEmpty()) ? (district + " India") : (state + " India"));
            }
        });
    }

    private void fetchLocationNewsFallback(String locationName, String fallbackQuery) {
        TextView emptyStateText = findViewById(R.id.emptyStateText);
        emptyStateText.setVisibility(View.GONE);
        newsRecyclerView.setVisibility(View.VISIBLE);

        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        String languageCode = preferences.getString("selected_language", "en");

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getEverything(fallbackQuery, API_KEY, languageCode);

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null && !response.body().getArticles().isEmpty()) {
                    setSanitizedArticles(response.body().getArticles());
                } else {
                    fetchIndianNews(null);
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                fetchIndianNews(null);
            }
        });
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
        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        boolean isDarkMode = preferences.getBoolean("dark_mode", false);
        boolean newDarkMode = !isDarkMode;

        preferences.edit().putBoolean("dark_mode", newDarkMode).apply();

        if (newDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Toast.makeText(this, "Dark Theme Enabled", Toast.LENGTH_SHORT).show();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(this, "Light Theme Enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void enableSnapScrolling(boolean enable) {
        if (enable) {
            if (!isSnapHelperAttached) {
                try {
                    snapHelper.attachToRecyclerView(newsRecyclerView);
                    isSnapHelperAttached = true;
                } catch (Exception ignored) {}
            }
        } else {
            if (isSnapHelperAttached) {
                try {
                    snapHelper.attachToRecyclerView(null);
                    isSnapHelperAttached = false;
                } catch (Exception ignored) {}
            }
        }
    }

    private void toggleAudioSpeech(Article article) {
        if (article == null) return;
        if (!isTtsInitialized || textToSpeech == null) {
            Toast.makeText(this, "Audio reader initializing...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (textToSpeech.isSpeaking() && article.getTitle().equals(currentlySpeakingTitle)) {
            textToSpeech.stop();
            currentlySpeakingTitle = "";
            Toast.makeText(this, "Audio playback stopped", Toast.LENGTH_SHORT).show();
        } else {
            textToSpeech.stop();
            currentlySpeakingTitle = article.getTitle();

            StringBuilder textToSpeak = new StringBuilder(article.getTitle());
            if (article.getDescription() != null && !article.getDescription().isEmpty()) {
                textToSpeak.append(". ").append(article.getDescription());
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(textToSpeak.toString(), TextToSpeech.QUEUE_FLUSH, null, "news_tts_id");
            } else {
                textToSpeech.speak(textToSpeak.toString(), TextToSpeech.QUEUE_FLUSH, null);
            }
            Toast.makeText(this, "Playing Audio News 🔊", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchBreakingNewsTicker() {
        LinearLayout breakingContainer = findViewById(R.id.breakingNewsContainer);
        TextView breakingText = findViewById(R.id.breakingNewsText);
        if (breakingContainer == null || breakingText == null) return;

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getTopHeadlines("in", API_KEY);

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null && !response.body().getArticles().isEmpty()) {
                    List<Article> articles = response.body().getArticles();
                    StringBuilder sb = new StringBuilder();
                    for (Article a : articles) {
                        if (a.getTitle() != null && !a.getTitle().contains("[Removed]")) {
                            if (sb.length() > 0) sb.append("  ✦  ");
                            sb.append(a.getTitle());
                        }
                    }
                    if (sb.length() > 0) {
                        breakingText.setText(sb.toString());
                        breakingText.setSelected(true); // Triggers Marquee animation
                        breakingContainer.setVisibility(View.GONE);

                        breakingContainer.setOnClickListener(v -> {
                            Article firstArticle = articles.get(0);
                            if (firstArticle.getUrl() != null && !firstArticle.getUrl().isEmpty()) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(firstArticle.getUrl()));
                                startActivity(intent);
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {}
        });
    }

    // ==========================================
    // 📍 HYPER-LOCAL RADIUS (10KM) NEWS FEATURE
    // ==========================================

    private void loadRadiusNewsTab() {
        loadRadiusNewsTab(false);
    }

    private void loadRadiusNewsTab(boolean forceLiveGpsRefresh) {
        if (radiusLocationBanner != null) {
            radiusLocationBanner.setVisibility(View.VISIBLE);
        }
        if (categoryRecyclerView != null) categoryRecyclerView.setVisibility(View.GONE);
        TextView emptyStateText = findViewById(R.id.emptyStateText);
        if (emptyStateText != null) emptyStateText.setVisibility(View.GONE);
        newsRecyclerView.setVisibility(View.VISIBLE);
        newsAdapter.setInshortsStyle(false);
        enableSnapScrolling(false);

        updatePlacesButtonBadge();

        if (forceLiveGpsRefresh) {
            SavedLocationManager.setActiveToLiveGps(this);
        }

        SavedLocation activeSaved = SavedLocationManager.getActiveSavedLocation(this);
        if (activeSaved != null && !forceLiveGpsRefresh) {
            // Viewing a saved location (Home, College, etc.)
            if (ivRadiusBannerPin != null) ivRadiusBannerPin.setVisibility(View.GONE);
            if (tvRadiusBannerIcon != null) {
                tvRadiusBannerIcon.setVisibility(View.VISIBLE);
                tvRadiusBannerIcon.setText(activeSaved.getTagIcon());
            }
            if (tvRadiusBannerMode != null) tvRadiusBannerMode.setText(activeSaved.getLabel().toUpperCase(Locale.getDefault()) + " • 10 KM RADIUS");
            if (tvRadiusBannerLocation != null) tvRadiusBannerLocation.setText(activeSaved.getAddressLine());
            if (btnSaveRadiusLocation != null) btnSaveRadiusLocation.setVisibility(View.GONE);

            fetchRadiusNews(
                    activeSaved.getLatitude(),
                    activeSaved.getLongitude(),
                    activeSaved.getSubLocality(),
                    "",
                    activeSaved.getLocality(),
                    "",
                    activeSaved.getPostalCode(),
                    activeSaved.getLabel()
            );
        } else {
            // Live GPS mode: Always fetch fresh GPS coordinates every time Around Me is selected
            if (tvRadiusBannerIcon != null) tvRadiusBannerIcon.setVisibility(View.GONE);
            if (ivRadiusBannerPin != null) ivRadiusBannerPin.setVisibility(View.VISIBLE);
            if (tvRadiusBannerMode != null) tvRadiusBannerMode.setText("LIVE GPS • 10 KM RADIUS");
            if (btnSaveRadiusLocation != null) btnSaveRadiusLocation.setVisibility(View.VISIBLE);

            if (!LocationHelper.hasLocationPermission(this)) {
                if (tvRadiusBannerLocation != null) {
                    tvRadiusBannerLocation.setText("Location permission required. Tap here to grant.");
                    tvRadiusBannerLocation.setOnClickListener(v -> LocationHelper.requestLocationPermission(this));
                }
                LocationHelper.requestLocationPermission(this);
            } else {
                if (tvRadiusBannerLocation != null) {
                    tvRadiusBannerLocation.setText("Detecting your exact GPS location...");
                    tvRadiusBannerLocation.setOnClickListener(null);
                }
                fetchCurrentGpsAndLoadRadiusNews();
            }
        }
    }

    private void updatePlacesButtonBadge() {
        if (btnSwitchRadiusPlaces == null) return;
        List<SavedLocation> list = SavedLocationManager.getSavedLocations(this);
        if (list.isEmpty()) {
            btnSwitchRadiusPlaces.setText("📍 Places");
        } else {
            btnSwitchRadiusPlaces.setText("📍 Places (" + list.size() + ")");
        }
    }

    private void fetchCurrentGpsAndLoadRadiusNews() {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        // Invalidate old radius cache to guarantee fresh location news
        if (tabArticleCache != null) {
            for (String key : new ArrayList<>(tabArticleCache.keySet())) {
                if (key.startsWith("radius_")) {
                    tabArticleCache.remove(key);
                    tabCacheTime.remove(key);
                }
            }
        }

        LocationHelper.getCurrentCoordinates(this, new LocationHelper.OnLocationResultListener() {
            @Override
            public void onLocationReceived(double latitude, double longitude) {
                currentGpsLat = latitude;
                currentGpsLon = longitude;

                // Update distances for all saved locations
                SavedLocationManager.updateDistancesFromCurrent(HomeActivity.this, latitude, longitude);
                updatePlacesButtonBadge();

                // Reverse geocode to get neighborhood / campus / pincode
                LocationHelper.reverseGeocode(HomeActivity.this, latitude, longitude, new LocationHelper.OnGeocodeResultListener() {
                    @Override
                    public void onGeocodeSuccess(String addressLine, String villageOrColony, String tehsilOrBlock, String district, String state, String postalCode) {
                        currentGpsAddress = addressLine;
                        currentGpsSubLocality = villageOrColony;
                        currentGpsTehsil = tehsilOrBlock;
                        currentGpsLocality = district;
                        currentGpsDistrict = district;
                        currentGpsState = state;
                        currentGpsPostalCode = postalCode;

                        if (tvRadiusBannerLocation != null) {
                            tvRadiusBannerLocation.setText(addressLine);
                        }

                        fetchRadiusNews(latitude, longitude, villageOrColony, tehsilOrBlock, district, state, postalCode, "Live GPS");
                    }

                    @Override
                    public void onGeocodeFailed(String errorMessage) {
                        currentGpsAddress = String.format(Locale.getDefault(), "Lat: %.3f, Lon: %.3f", latitude, longitude);
                        if (tvRadiusBannerLocation != null) {
                            tvRadiusBannerLocation.setText(currentGpsAddress);
                        }
                        fetchRadiusNews(latitude, longitude, "", "", "India", "", "", "Live GPS");
                    }
                });
            }

            @Override
            public void onLocationFailed(String errorMessage) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (tvRadiusBannerLocation != null) {
                    tvRadiusBannerLocation.setText(errorMessage);
                }
                Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchRadiusNews(double lat, double lon, String village, String tehsil, String district, String state, String postalCode, String placeLabel) {
        String cacheKey = "radius_" + (village != null ? village : "") + "_" + (tehsil != null ? tehsil : "") + "_" + (district != null ? district : "") + "_" + (postalCode != null ? postalCode : "");
        if (isCacheValid(cacheKey)) {
            setSanitizedArticles(tabArticleCache.get(cacheKey));
            return;
        }

        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        executor.execute(() -> {
            boolean backendSuccess = false;
            try {
                SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
                String targetLang = preferences.getString("selected_language", "en");

                String backendBase = "http://10.0.2.2:8085";
                StringBuilder urlBuilder = new StringBuilder(backendBase + "/api/radius-news?");
                if (lat != 0.0) urlBuilder.append("lat=").append(lat).append("&");
                if (lon != 0.0) urlBuilder.append("lon=").append(lon).append("&");
                if (village != null && !village.isEmpty()) {
                    urlBuilder.append("village=").append(URLEncoder.encode(village, "UTF-8")).append("&");
                }
                if (tehsil != null && !tehsil.isEmpty()) {
                    urlBuilder.append("tehsil=").append(URLEncoder.encode(tehsil, "UTF-8")).append("&");
                }
                if (district != null && !district.isEmpty()) {
                    urlBuilder.append("district=").append(URLEncoder.encode(district, "UTF-8")).append("&");
                }
                if (state != null && !state.isEmpty()) {
                    urlBuilder.append("state=").append(URLEncoder.encode(state, "UTF-8")).append("&");
                }
                if (postalCode != null && !postalCode.isEmpty()) {
                    urlBuilder.append("postal_code=").append(URLEncoder.encode(postalCode, "UTF-8")).append("&");
                }
                urlBuilder.append("radius=10&target_lang=").append(URLEncoder.encode(targetLang, "UTF-8"));

                URL url = new URL(urlBuilder.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(18000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    Gson gson = new Gson();
                    NewsResponse response = gson.fromJson(sb.toString(), NewsResponse.class);
                    if (response != null && response.getArticles() != null && !response.getArticles().isEmpty()) {
                        backendSuccess = true;
                        final boolean isExpanded = response.isExpanded();
                        final int effectiveRadius = response.getRadiusKm() > 0 ? response.getRadiusKm() : 10;
                        final String geoLevel = response.getGeoLevel();
                        final String geoLabel = response.getGeoLabel();
                        final String area = response.getArea();
                        final String expansionReason = response.getExpansionReason();

                        mainHandler.post(() -> {
                            saveToCache(cacheKey, response.getArticles());
                            setSanitizedArticles(response.getArticles());

                            if (tvRadiusBannerMode != null) {
                                String baseMode = (placeLabel != null && !placeLabel.equals("Live GPS"))
                                        ? placeLabel.toUpperCase(Locale.getDefault())
                                        : "LIVE GPS";
                                if ("tehsil".equalsIgnoreCase(geoLevel) && geoLabel != null && !geoLabel.isEmpty()) {
                                    tvRadiusBannerMode.setText(baseMode + " • TEHSIL " + geoLabel.toUpperCase(Locale.getDefault()) + " (10 KM)");
                                } else if ("district".equalsIgnoreCase(geoLevel) && geoLabel != null && !geoLabel.isEmpty()) {
                                    tvRadiusBannerMode.setText(baseMode + " • " + geoLabel.toUpperCase(Locale.getDefault()) + " DISTRICT (20 KM)");
                                } else if (isExpanded) {
                                    tvRadiusBannerMode.setText(baseMode + " • 20 KM RADIUS (EXPANDED)");
                                } else {
                                    tvRadiusBannerMode.setText(baseMode + " • " + effectiveRadius + " KM RADIUS");
                                }
                            }
                            if (tvRadiusBannerLocation != null && area != null && !area.isEmpty() && !"Nearby".equals(area)) {
                                tvRadiusBannerLocation.setText(area);
                            }
                            if (expansionReason != null && !expansionReason.isEmpty()) {
                                Toast.makeText(HomeActivity.this, expansionReason, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.w("HomeActivity", "Backend radius news fallback: " + e.getMessage());
            }

            if (!backendSuccess) {
                mainHandler.post(() -> fetchRadiusNewsFallback(cacheKey, village, tehsil, district, placeLabel));
            }
        });
    }

    private void fetchRadiusNewsFallback(String cacheKey, String village, String tehsil, String district, String placeLabel) {
        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        String languageCode = preferences.getString("selected_language", "en");

        String query;
        if (village != null && !village.isEmpty()) {
            query = (village + " " + (district != null ? district : "")).trim();
        } else if (tehsil != null && !tehsil.isEmpty()) {
            query = (tehsil + " " + (district != null ? district : "") + " news").trim();
        } else if (district != null && !district.isEmpty()) {
            query = district + " news";
        } else {
            query = "India local news";
        }

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getEverything(query, API_KEY, languageCode);

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null && !response.body().getArticles().isEmpty()) {
                    List<Article> articles = response.body().getArticles();
                    if (articles.size() < 5 && tehsil != null && !tehsil.isEmpty() && !tehsil.equalsIgnoreCase(village)) {
                        fetchTehsilOrDistrictFallback(cacheKey, articles, tehsil, district, placeLabel);
                    } else if (articles.size() < 5 && district != null && !district.isEmpty()) {
                        fetchDistrictFallback(cacheKey, articles, district, placeLabel);
                    } else {
                        saveToCache(cacheKey, articles);
                        setSanitizedArticles(articles);
                        if (tvRadiusBannerMode != null) {
                            String baseMode = (placeLabel != null && !placeLabel.equals("Live GPS"))
                                    ? placeLabel.toUpperCase(Locale.getDefault())
                                    : "LIVE GPS";
                            tvRadiusBannerMode.setText(baseMode + " • 10 KM RADIUS");
                        }
                    }
                } else {
                    fetchLocationNewsFallback(district, (district != null && !district.isEmpty() ? district : "India") + " news");
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                fetchLocationNewsFallback(district, (district != null && !district.isEmpty() ? district : "India") + " news");
            }
        });
    }

    private void fetchTehsilOrDistrictFallback(String cacheKey, List<Article> initialArticles, String tehsil, String district, String placeLabel) {
        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        String languageCode = preferences.getString("selected_language", "en");

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getEverything(tehsil + " news", API_KEY, languageCode);

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                List<Article> merged = new ArrayList<>(initialArticles);
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null) {
                    Set<String> titles = new HashSet<>();
                    for (Article a : initialArticles) {
                        if (a.getTitle() != null) titles.add(a.getTitle().toLowerCase(Locale.getDefault()));
                    }
                    for (Article a : response.body().getArticles()) {
                        if (a.getTitle() != null && !titles.contains(a.getTitle().toLowerCase(Locale.getDefault()))) {
                            merged.add(a);
                            titles.add(a.getTitle().toLowerCase(Locale.getDefault()));
                        }
                    }
                }

                if (merged.size() < 5 && district != null && !district.isEmpty()) {
                    fetchDistrictFallback(cacheKey, merged, district, placeLabel);
                } else {
                    saveToCache(cacheKey, merged);
                    setSanitizedArticles(merged);
                    if (tvRadiusBannerMode != null) {
                        String baseMode = (placeLabel != null && !placeLabel.equals("Live GPS"))
                                ? placeLabel.toUpperCase(Locale.getDefault())
                                : "LIVE GPS";
                        tvRadiusBannerMode.setText(baseMode + " • TEHSIL " + tehsil.toUpperCase(Locale.getDefault()) + " (10 KM)");
                    }
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                if (district != null && !district.isEmpty()) {
                    fetchDistrictFallback(cacheKey, initialArticles, district, placeLabel);
                } else {
                    saveToCache(cacheKey, initialArticles);
                    setSanitizedArticles(initialArticles);
                }
            }
        });
    }

    private void fetchDistrictFallback(String cacheKey, List<Article> initialArticles, String district, String placeLabel) {
        SharedPreferences preferences = getSharedPreferences("user_preferences", MODE_PRIVATE);
        String languageCode = preferences.getString("selected_language", "en");

        NewsApiService apiService = ApiClient.getClient().create(NewsApiService.class);
        Call<NewsResponse> call = apiService.getEverything(district + " news", API_KEY, languageCode);

        call.enqueue(new Callback<NewsResponse>() {
            @Override
            public void onResponse(Call<NewsResponse> call, Response<NewsResponse> response) {
                List<Article> merged = new ArrayList<>(initialArticles);
                if (response.isSuccessful() && response.body() != null && response.body().getArticles() != null) {
                    Set<String> titles = new HashSet<>();
                    for (Article a : initialArticles) {
                        if (a.getTitle() != null) titles.add(a.getTitle().toLowerCase(Locale.getDefault()));
                    }
                    for (Article a : response.body().getArticles()) {
                        if (a.getTitle() != null && !titles.contains(a.getTitle().toLowerCase(Locale.getDefault()))) {
                            merged.add(a);
                            titles.add(a.getTitle().toLowerCase(Locale.getDefault()));
                        }
                    }
                }
                saveToCache(cacheKey, merged);
                setSanitizedArticles(merged);
                if (tvRadiusBannerMode != null) {
                    String baseMode = (placeLabel != null && !placeLabel.equals("Live GPS"))
                            ? placeLabel.toUpperCase(Locale.getDefault())
                            : "LIVE GPS";
                    tvRadiusBannerMode.setText(baseMode + " • " + district.toUpperCase(Locale.getDefault()) + " DISTRICT (20 KM)");
                }
            }

            @Override
            public void onFailure(Call<NewsResponse> call, Throwable t) {
                saveToCache(cacheKey, initialArticles);
                setSanitizedArticles(initialArticles);
            }
        });
    }

    private void showSaveLocationDialog() {
        if (currentGpsLat == 0.0 && currentGpsLon == 0.0) {
            Toast.makeText(this, "Detecting GPS location... please wait a moment.", Toast.LENGTH_SHORT).show();
            fetchCurrentGpsAndLoadRadiusNews();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_save_location, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvAddressPreview = dialogView.findViewById(R.id.tvDialogAddressPreview);
        EditText etLabel = dialogView.findViewById(R.id.etLocationLabel);
        TextView chipHome = dialogView.findViewById(R.id.chipHome);
        TextView chipCollege = dialogView.findViewById(R.id.chipCollege);
        TextView chipWork = dialogView.findViewById(R.id.chipWork);
        TextView chipOther = dialogView.findViewById(R.id.chipOther);
        View btnCancel = dialogView.findViewById(R.id.btnCancelSaveLocation);
        View btnConfirm = dialogView.findViewById(R.id.btnConfirmSaveLocation);

        final String[] selectedTag = {"🏠"};
        etLabel.setText("Home");

        Runnable updateChipStyles = () -> {
            boolean isHome = "🏠".equals(selectedTag[0]);
            boolean isCollege = "🎓".equals(selectedTag[0]);
            boolean isWork = "💼".equals(selectedTag[0]);
            boolean isOther = "📍".equals(selectedTag[0]);

            int activeColor = ContextCompat.getColor(HomeActivity.this, R.color.pill_blue_text);
            int inactiveColor = ContextCompat.getColor(HomeActivity.this, R.color.pill_outline_text);

            chipHome.setBackgroundResource(isHome ? R.drawable.bg_radius_pill_blue : R.drawable.bg_radius_pill_outline);
            chipHome.setTextColor(isHome ? activeColor : inactiveColor);

            chipCollege.setBackgroundResource(isCollege ? R.drawable.bg_radius_pill_blue : R.drawable.bg_radius_pill_outline);
            chipCollege.setTextColor(isCollege ? activeColor : inactiveColor);

            chipWork.setBackgroundResource(isWork ? R.drawable.bg_radius_pill_blue : R.drawable.bg_radius_pill_outline);
            chipWork.setTextColor(isWork ? activeColor : inactiveColor);

            chipOther.setBackgroundResource(isOther ? R.drawable.bg_radius_pill_blue : R.drawable.bg_radius_pill_outline);
            chipOther.setTextColor(isOther ? activeColor : inactiveColor);
        };
        updateChipStyles.run();

        String displayAddr = !currentGpsAddress.isEmpty() ? currentGpsAddress : String.format(Locale.getDefault(), "Lat: %.3f, Lon: %.3f", currentGpsLat, currentGpsLon);
        tvAddressPreview.setText(displayAddr);

        chipHome.setOnClickListener(v -> {
            selectedTag[0] = "🏠";
            etLabel.setText("Home");
            updateChipStyles.run();
        });
        chipCollege.setOnClickListener(v -> {
            selectedTag[0] = "🎓";
            etLabel.setText("College");
            updateChipStyles.run();
        });
        chipWork.setOnClickListener(v -> {
            selectedTag[0] = "💼";
            etLabel.setText("Work");
            updateChipStyles.run();
        });
        chipOther.setOnClickListener(v -> {
            selectedTag[0] = "📍";
            etLabel.setText(!currentGpsSubLocality.isEmpty() ? currentGpsSubLocality : "My Spot");
            updateChipStyles.run();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String label = etLabel.getText().toString().trim();
            if (label.isEmpty()) {
                label = "Saved Place";
            }

            SavedLocation savedLocation = new SavedLocation(
                    label,
                    selectedTag[0],
                    displayAddr,
                    currentGpsSubLocality,
                    currentGpsLocality,
                    currentGpsPostalCode,
                    currentGpsLat,
                    currentGpsLon
            );

            SavedLocationManager.saveLocation(HomeActivity.this, savedLocation);
            Toast.makeText(HomeActivity.this, "Saved " + label + " successfully! 📍", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            updatePlacesButtonBadge();
            loadRadiusNewsTab();
        });

        dialog.show();
    }

    private void showSavedLocationsBottomSheet() {
        SavedLocationsBottomSheetDialog bottomSheet = SavedLocationsBottomSheetDialog.newInstance(new SavedLocationsBottomSheetDialog.OnLocationChangeListener() {
            @Override
            public void onLocationChanged(SavedLocation location) {
                if (location != null) {
                    SavedLocationManager.setActiveSavedLocation(HomeActivity.this, location);
                    loadRadiusNewsTab(false);
                } else {
                    SavedLocationManager.setActiveToLiveGps(HomeActivity.this);
                    loadRadiusNewsTab(true);
                }
            }

            @Override
            public void onSaveCurrentSpotRequested() {
                showSaveLocationDialog();
            }

            @Override
            public void onSearchCustomAreaRequested() {
                showSearchAreaDialog();
            }
        });
        bottomSheet.show(getSupportFragmentManager(), "SavedLocationsBottomSheet");
    }

    private void showPostCommunityNoticeDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_post_community_notice, null);
        dialog.setContentView(sheetView);

        TextView tvPostNoticeLocation = sheetView.findViewById(R.id.tvPostNoticeLocation);
        EditText etNoticeTitle = sheetView.findViewById(R.id.etNoticeTitle);
        EditText etNoticeContent = sheetView.findViewById(R.id.etNoticeContent);
        TextView tvWordCount = sheetView.findViewById(R.id.tvWordCount);
        TextView tvWordLimitWarning = sheetView.findViewById(R.id.tvWordLimitWarning);
        TextView btnSubmitNotice = sheetView.findViewById(R.id.btnSubmitNotice);
        ProgressBar pbSubmitNotice = sheetView.findViewById(R.id.pbSubmitNotice);
        LinearLayout layoutRejectionNotice = sheetView.findViewById(R.id.layoutRejectionNotice);
        TextView tvRejectionReason = sheetView.findViewById(R.id.tvRejectionReason);

        com.google.android.material.chip.Chip chipRoleResident = sheetView.findViewById(R.id.chipRoleResident);
        com.google.android.material.chip.Chip chipRolePanchayat = sheetView.findViewById(R.id.chipRolePanchayat);
        com.google.android.material.chip.Chip chipRoleStudent = sheetView.findViewById(R.id.chipRoleStudent);
        com.google.android.material.chip.Chip chipRoleDoctor = sheetView.findViewById(R.id.chipRoleDoctor);
        com.google.android.material.chip.Chip chipRoleMerchant = sheetView.findViewById(R.id.chipRoleMerchant);

        TextView templateHealthCamp = sheetView.findViewById(R.id.templateHealthCamp);
        TextView templateRoadRepair = sheetView.findViewById(R.id.templateRoadRepair);
        TextView templatePowerCut = sheetView.findViewById(R.id.templatePowerCut);
        TextView templateMandi = sheetView.findViewById(R.id.templateMandi);

        // Location label
        String locDisplay = currentGpsAddress;
        if (locDisplay == null || locDisplay.isEmpty()) {
            locDisplay = tvRadiusBannerLocation != null ? tvRadiusBannerLocation.getText().toString() : "Current Location";
        }
        if (tvPostNoticeLocation != null) {
            tvPostNoticeLocation.setText(locDisplay);
        }

        // Helper to update word count & colors dynamically
        Runnable updateWordCounter = () -> {
            String text = etNoticeContent.getText().toString().trim();
            int words = text.isEmpty() ? 0 : text.split("\\s+").length;
            if (tvWordCount != null) {
                tvWordCount.setText(words + " / 60 words");
                if (words == 0) {
                    tvWordCount.setTextColor(android.graphics.Color.parseColor("#64748B"));
                    if (tvWordLimitWarning != null) tvWordLimitWarning.setVisibility(View.GONE);
                    btnSubmitNotice.setEnabled(true);
                    btnSubmitNotice.setAlpha(1.0f);
                } else if (words <= 45) {
                    tvWordCount.setTextColor(android.graphics.Color.parseColor("#2563EB")); // Global Insight Brand Blue
                    if (tvWordLimitWarning != null) tvWordLimitWarning.setVisibility(View.GONE);
                    btnSubmitNotice.setEnabled(true);
                    btnSubmitNotice.setAlpha(1.0f);
                } else if (words <= 60) {
                    tvWordCount.setTextColor(android.graphics.Color.parseColor("#D97706")); // Amber warning
                    if (tvWordLimitWarning != null) tvWordLimitWarning.setVisibility(View.GONE);
                    btnSubmitNotice.setEnabled(true);
                    btnSubmitNotice.setAlpha(1.0f);
                } else {
                    tvWordCount.setTextColor(android.graphics.Color.parseColor("#DC2626")); // Red error
                    if (tvWordLimitWarning != null) tvWordLimitWarning.setVisibility(View.VISIBLE);
                    btnSubmitNotice.setEnabled(false);
                    btnSubmitNotice.setAlpha(0.6f);
                }
            }
        };

        // Quick template clicks
        if (templateHealthCamp != null) {
            templateHealthCamp.setOnClickListener(v -> {
                etNoticeTitle.setText("Free Health & Eye Checkup Camp at Local Dispensary");
                etNoticeContent.setText("A free health and eye checkup camp is organized this Sunday from 9 AM to 2 PM at the local dispensary. Doctors will provide free consultations and basic medicine. All residents are welcome.");
                if (chipRoleDoctor != null) chipRoleDoctor.setChecked(true);
                updateWordCounter.run();
            });
        }
        if (templateRoadRepair != null) {
            templateRoadRepair.setOnClickListener(v -> {
                etNoticeTitle.setText("Road Repair & Service Lane Diversion on Main Sector Road");
                etNoticeContent.setText("Emergency road repair underway on the main sector road for the next 48 hours. Heavy vehicles and traffic diverted via the internal service lane. Commuters please plan ahead.");
                if (chipRoleResident != null) chipRoleResident.setChecked(true);
                updateWordCounter.run();
            });
        }
        if (templatePowerCut != null) {
            templatePowerCut.setOnClickListener(v -> {
                etNoticeTitle.setText("Scheduled Power Outage for Feeder Maintenance on Saturday");
                etNoticeContent.setText("Electricity department announces scheduled maintenance outage this Saturday from 10 AM to 3 PM for transformer upgradation across the sector. Inconvenience is regretted.");
                if (chipRoleResident != null) chipRoleResident.setChecked(true);
                updateWordCounter.run();
            });
        }
        if (templateMandi != null) {
            templateMandi.setOnClickListener(v -> {
                etNoticeTitle.setText("Grain Mandi Government MSP Procurement Starts Monday");
                etNoticeContent.setText("Local Anaaj Mandi will commence government MSP procurement from Monday 8 AM. Farmers are requested to bring registration token slips and bank passbooks.");
                if (chipRolePanchayat != null) chipRolePanchayat.setChecked(true);
                updateWordCounter.run();
            });
        }

        // Word Counter TextWatcher (strictly <= 60 words)
        etNoticeContent.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateWordCounter.run();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnSubmitNotice.setOnClickListener(v -> {
            String title = etNoticeTitle.getText().toString().trim();
            String content = etNoticeContent.getText().toString().trim();

            if (title.isEmpty()) {
                etNoticeTitle.setError("Please enter a notice headline");
                etNoticeTitle.requestFocus();
                return;
            }
            if (content.isEmpty()) {
                etNoticeContent.setError("Please enter notice details");
                etNoticeContent.requestFocus();
                return;
            }

            int wordCount = content.split("\\s+").length;
            if (wordCount > 60) {
                Toast.makeText(HomeActivity.this, "Notice must be 60 words or fewer for instant mobile reading", Toast.LENGTH_SHORT).show();
                return;
            }

            String authorRole = "Verified Resident";
            if (chipRolePanchayat != null && chipRolePanchayat.isChecked()) {
                authorRole = "Panchayat Member";
            } else if (chipRoleStudent != null && chipRoleStudent.isChecked()) {
                authorRole = "Student";
            } else if (chipRoleDoctor != null && chipRoleDoctor.isChecked()) {
                authorRole = "Doctor / PHC";
            } else if (chipRoleMerchant != null && chipRoleMerchant.isChecked()) {
                authorRole = "Local Merchant";
            }

            // Show loading state
            btnSubmitNotice.setText("");
            btnSubmitNotice.setEnabled(false);
            if (pbSubmitNotice != null) pbSubmitNotice.setVisibility(View.VISIBLE);
            if (layoutRejectionNotice != null) layoutRejectionNotice.setVisibility(View.GONE);

            final String finalRole = authorRole;
            final double finalLat = currentGpsLat;
            final double finalLon = currentGpsLon;
            final String finalVillage = currentGpsSubLocality;
            final String finalTehsil = currentGpsTehsil;
            final String finalDistrict = currentGpsDistrict;
            final String finalState = currentGpsState;

            Executors.newSingleThreadExecutor().execute(() -> {
                boolean isApproved = false;
                String responseMessage = "";
                String rejectionReason = "";

                try {
                    URL url = new URL("http://10.0.2.2:8085/api/community/post");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(18000);

                    JSONObject payload = new JSONObject();
                    payload.put("title", title);
                    payload.put("content", content);
                    payload.put("author_name", "Community Member");
                    payload.put("author_role", finalRole);
                    payload.put("village", finalVillage != null ? finalVillage : "");
                    payload.put("tehsil", finalTehsil != null ? finalTehsil : "");
                    payload.put("district", finalDistrict != null ? finalDistrict : "");
                    payload.put("state", finalState != null ? finalState : "");
                    payload.put("lat", finalLat);
                    payload.put("lon", finalLon);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                    }

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();

                        JSONObject resJson = new JSONObject(sb.toString());
                        isApproved = resJson.optBoolean("approved", false);
                        responseMessage = resJson.optString("message", "Notice broadcasted successfully!");
                        rejectionReason = resJson.optString("reason", "Notice violated community safety guidelines.");
                    } else {
                        rejectionReason = "Server error (" + code + "). Please try again.";
                    }
                } catch (Exception e) {
                    rejectionReason = "AI verification error: " + e.getMessage();
                }

                final boolean success = isApproved;
                final String msg = responseMessage;
                final String reason = rejectionReason;

                runOnUiThread(() -> {
                    btnSubmitNotice.setText("Verify & Publish Notice");
                    btnSubmitNotice.setEnabled(true);
                    if (pbSubmitNotice != null) pbSubmitNotice.setVisibility(View.GONE);

                    if (success) {
                        dialog.dismiss();
                        new AlertDialog.Builder(HomeActivity.this)
                                .setTitle("Notice Verified & Published")
                                .setMessage(msg + "\n\nYour spotlight is now live for all residents in your 10 km circle.")
                                .setPositiveButton("View in Feed", (d, w) -> {
                                    tabArticleCache.clear();
                                    tabCacheTime.clear();
                                    loadRadiusNewsTab(false);
                                })
                                .show();

                        tabArticleCache.clear();
                        tabCacheTime.clear();
                        loadRadiusNewsTab(false);
                    } else {
                        if (layoutRejectionNotice != null) layoutRejectionNotice.setVisibility(View.VISIBLE);
                        if (tvRejectionReason != null) tvRejectionReason.setText(reason);
                    }
                });
            });
        });

        dialog.show();
    }

    private void showSearchAreaDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔍 Set Area / Coordinates");
        builder.setMessage("Enter your area, neighborhood, or college:\n(e.g., Palam Vihar Gurgaon 122017 or Kapriwas BMU)");

        final EditText input = new EditText(this);
        input.setHint("e.g. Palam Vihar Gurgaon 122017");
        input.setText("Palam Vihar Gurgaon 122017");
        input.setSelection(input.getText().length());
        input.setPadding(36, 24, 36, 24);
        builder.setView(input);

        builder.setPositiveButton("Set Area & Fetch News", (dialog, which) -> {
            String query = input.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "Please enter an area name", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Detecting coordinates for " + query + "...", Toast.LENGTH_SHORT).show();
            LocationHelper.geocodeAreaNameToCoordinates(this, query, query, "📍", new LocationHelper.OnGeocodeAreaResultListener() {
                @Override
                public void onAreaGeocoded(SavedLocation savedLocation) {
                    SavedLocationManager.saveLocation(HomeActivity.this, savedLocation);
                    SavedLocationManager.setActiveSavedLocation(HomeActivity.this, savedLocation);
                    Toast.makeText(HomeActivity.this, "Set to: " + savedLocation.getAddressLine(), Toast.LENGTH_SHORT).show();
                    loadRadiusNewsTab();
                }

                @Override
                public void onGeocodeFailed(String errorMessage) {
                    Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted! Fetching 10 km radius news...", Toast.LENGTH_SHORT).show();
                loadRadiusNewsTab();
            } else {
                Toast.makeText(this, "Location permission is required to detect news within 10 km radius.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Method to remove the reported news from the list
    public void removeReportedNews(Article reportedArticle) {

        newsList.remove(reportedArticle);
        newsAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
