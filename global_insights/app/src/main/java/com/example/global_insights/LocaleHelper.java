package com.example.global_insights;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS_NAME = "LanguagePrefs";
    private static final String LANGUAGE_KEY = "language";

    public static void setLocale(Context context, String languageCode) {
        // Store the selected language in SharedPreferences
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(LANGUAGE_KEY, languageCode);
        editor.apply();

        // Set the new locale
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(LANGUAGE_KEY, "en"); // Default to English
    }
}
