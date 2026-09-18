package com.example.global_insights;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.global_insights.model.SavedLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SavedLocationManager {

    private static final String PREF_NAME = "saved_locations_pref";
    private static final String KEY_LOCATIONS_LIST = "key_locations_list";
    private static final String KEY_ACTIVE_MODE_IS_LIVE = "key_active_mode_is_live";
    private static final String KEY_ACTIVE_SAVED_ID = "key_active_saved_id";

    private static final Gson gson = new Gson();

    public static List<SavedLocation> getSavedLocations(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_LOCATIONS_LIST, null);
        if (json == null || json.trim().isEmpty()) {
            List<SavedLocation> defaultList = new ArrayList<>();
            defaultList.add(new SavedLocation(
                    "Home",
                    "🏠",
                    "Palam Vihar, Gurugram, Haryana (122017)",
                    "Palam Vihar",
                    "Gurugram",
                    "122017",
                    28.5116,
                    77.0365
            ));
            defaultList.add(new SavedLocation(
                    "College",
                    "🎓",
                    "BML Munjal University, Kapriwas (122413)",
                    "Kapriwas",
                    "Gurugram",
                    "122413",
                    28.2435,
                    76.8152
            ));
            persistList(context, defaultList);
            return defaultList;
        }
        Type type = new TypeToken<List<SavedLocation>>() {}.getType();
        try {
            List<SavedLocation> list = gson.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }


    public static void saveLocation(Context context, SavedLocation location) {
        if (location == null) return;
        List<SavedLocation> currentList = getSavedLocations(context);

        // Remove existing item with same ID if updating
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).getId().equals(location.getId())) {
                currentList.remove(i);
                break;
            }
        }

        currentList.add(0, location); // Add to top
        persistList(context, currentList);

        // Sync with Firebase if logged in
        syncToFirebase(location);
    }

    public static void deleteLocation(Context context, String locationId) {
        if (locationId == null) return;
        List<SavedLocation> currentList = getSavedLocations(context);
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).getId().equals(locationId)) {
                currentList.remove(i);
                break;
            }
        }
        persistList(context, currentList);

        // If currently active location was deleted, revert to Live GPS
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String activeId = prefs.getString(KEY_ACTIVE_SAVED_ID, "");
        if (locationId.equals(activeId)) {
            setActiveToLiveGps(context);
        }

        // Delete from Firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("saved_locations")
                    .child(locationId);
            ref.removeValue();
        }
    }

    // Session-based active location (defaults to Live GPS on every app launch)
    private static SavedLocation sessionActiveSavedLocation = null;
    private static boolean sessionIsLiveGps = true;

    public static boolean isLiveGpsActive(Context context) {
        return sessionIsLiveGps && sessionActiveSavedLocation == null;
    }

    public static void setActiveToLiveGps(Context context) {
        sessionIsLiveGps = true;
        sessionActiveSavedLocation = null;
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putBoolean(KEY_ACTIVE_MODE_IS_LIVE, true)
                    .putString(KEY_ACTIVE_SAVED_ID, "")
                    .apply();
        }
    }

    public static void setActiveSavedLocation(Context context, SavedLocation location) {
        if (location == null) {
            setActiveToLiveGps(context);
            return;
        }
        sessionIsLiveGps = false;
        sessionActiveSavedLocation = location;
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putBoolean(KEY_ACTIVE_MODE_IS_LIVE, false)
                    .putString(KEY_ACTIVE_SAVED_ID, location.getId())
                    .apply();
        }
    }

    public static SavedLocation getActiveSavedLocation(Context context) {
        if (sessionIsLiveGps || sessionActiveSavedLocation == null) {
            return null;
        }
        return sessionActiveSavedLocation;
    }

    public static void updateDistancesFromCurrent(Context context, double currentLat, double currentLon) {
        List<SavedLocation> list = getSavedLocations(context);
        boolean changed = false;
        for (SavedLocation loc : list) {
            double distance = LocationHelper.calculateDistanceKm(currentLat, currentLon, loc.getLatitude(), loc.getLongitude());
            loc.setDistanceFromCurrentKm(distance);
            changed = true;
        }
        if (changed) {
            persistList(context, list);
        }
    }

    private static void persistList(Context context, List<SavedLocation> list) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(list);
        prefs.edit().putString(KEY_LOCATIONS_LIST, json).apply();
    }

    private static void syncToFirebase(SavedLocation location) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && location != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("saved_locations")
                    .child(location.getId());
            ref.setValue(location);
        }
    }

    public static void loadFromFirebaseIfAvailable(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("saved_locations");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<SavedLocation> cloudList = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        SavedLocation loc = child.getValue(SavedLocation.class);
                        if (loc != null) {
                            cloudList.add(loc);
                        }
                    }
                    if (!cloudList.isEmpty()) {
                        persistList(context, cloudList);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}
