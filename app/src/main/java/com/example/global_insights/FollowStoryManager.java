package com.example.global_insights;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.global_insights.model.Story;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class FollowStoryManager {

    private static final String PREF_NAME = "followed_stories_cache";

    public interface FollowStatusCallback {
        void onStatusChecked(boolean isFollowed);
    }

    public interface FollowToggleCallback {
        void onComplete(boolean isNowFollowed, String message);
        void onError(String errorMessage);
    }

    /**
     * Checks if current user is an authenticated, non-guest member.
     */
    public static boolean isUserRegistered(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            return false;
        }
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
            if (prefs.getBoolean("is_guest", false)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sanitize story key for Firebase Realtime Database path.
     */
    public static String getSafeStoryKey(String storyId, String title) {
        if (storyId != null && !storyId.trim().isEmpty()) {
            return storyId.replaceAll("[.#$\\[\\]/\\s]", "_");
        }
        if (title != null && !title.trim().isEmpty()) {
            return "story_" + Math.abs(title.hashCode());
        }
        return "story_" + System.currentTimeMillis();
    }

    /**
     * Get cached follow status from local SharedPreferences.
     */
    public static boolean isFollowedLocally(Context context, String storyKey) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME + "_" + user.getUid(), Context.MODE_PRIVATE);
        return prefs.getBoolean(storyKey, false);
    }

    private static void setFollowedLocally(Context context, String storyKey, boolean followed) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME + "_" + user.getUid(), Context.MODE_PRIVATE);
        prefs.edit().putBoolean(storyKey, followed).apply();
    }

    /**
     * Checks whether the current user is following this story in Firebase Database.
     */
    public static void checkFollowStatus(Context context, String storyKey, FollowStatusCallback callback) {
        if (!isUserRegistered(context)) {
            if (callback != null) callback.onStatusChecked(false);
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onStatusChecked(false);
            return;
        }

        // Return instant local cache value first
        boolean localStatus = isFollowedLocally(context, storyKey);
        if (callback != null) {
            callback.onStatusChecked(localStatus);
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("followed_stories")
                .child(storyKey);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean remoteFollowed = snapshot.exists();
                setFollowedLocally(context, storyKey, remoteFollowed);
                if (callback != null && remoteFollowed != localStatus) {
                    callback.onStatusChecked(remoteFollowed);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore failure, fallback to local state
            }
        });
    }

    /**
     * Toggles following status for a registered user.
     */
    public static void toggleFollow(Context context, Story story, FollowToggleCallback callback) {
        if (!isUserRegistered(context)) {
            if (callback != null) {
                callback.onError("User is not signed in.");
            }
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError("Authentication required.");
            return;
        }

        String storyKey = getSafeStoryKey(story.getStoryId(), story.getTitle());
        boolean currentlyFollowed = isFollowedLocally(context, storyKey);
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("followed_stories")
                .child(storyKey);

        if (currentlyFollowed) {
            // Unfollow
            ref.removeValue((error, databaseReference) -> {
                if (error == null) {
                    setFollowedLocally(context, storyKey, false);
                    if (callback != null) {
                        callback.onComplete(false, "Unfollowed story");
                    }
                } else {
                    if (callback != null) callback.onError(error.getMessage());
                }
            });
        } else {
            // Follow
            Map<String, Object> data = new HashMap<>();
            data.put("storyId", story.getStoryId() != null ? story.getStoryId() : storyKey);
            data.put("title", story.getTitle() != null ? story.getTitle() : "Untitled Story");
            data.put("source", story.getSource() != null ? story.getSource() : "Global Insight");
            data.put("sourceUrl", story.getSourceUrl() != null ? story.getSourceUrl() : "");
            data.put("category", story.getCategory() != null ? story.getCategory() : "General");
            data.put("followedAt", ServerValue.TIMESTAMP);
            data.put("lastUpdated", ServerValue.TIMESTAMP);

            ref.setValue(data, (error, databaseReference) -> {
                if (error == null) {
                    setFollowedLocally(context, storyKey, true);
                    if (callback != null) {
                        callback.onComplete(true, "✓ You are now following this story! We'll alert you as it develops.");
                    }
                } else {
                    if (callback != null) callback.onError(error.getMessage());
                }
            });
        }
    }

    /**
     * Shows a polite dialog explaining that follow & updates are for registered members only.
     */
    public static void showGuestPrompt(Context context) {
        if (context == null) return;
        new AlertDialog.Builder(context)
                .setTitle("Member Feature")
                .setMessage("Following stories and receiving live update alerts is exclusively available for registered Global Insight members.\n\nPlease sign in or create an account to follow this story.")
                .setPositiveButton("Sign In", (dialog, which) -> {
                    Intent intent = new Intent(context, SignInActivity.class);
                    context.startActivity(intent);
                })
                .setNegativeButton("Not Now", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
