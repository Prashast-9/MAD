package com.example.currencyconvertor;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Persists and applies light/dark theme using SharedPreferences and {@link AppCompatDelegate}.
 */
public final class ThemeManager {

    private static final String PREFS_NAME = "currency_converter_prefs";
    private static final String KEY_THEME = "theme";

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private ThemeManager() {
    }

    public static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Applies the stored theme. Call from {@link android.app.Application#onCreate()} before any UI.
     */
    public static void applyPersistedTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode(nightModeFromStoredTheme(context));
    }

    /**
     * @return {@code true} if the user has chosen dark theme in settings.
     */
    public static boolean isDarkTheme(Context context) {
        return THEME_DARK.equals(prefs(context).getString(KEY_THEME, THEME_LIGHT));
    }

    /**
     * Persists choice and applies it app-wide (activities may recreate).
     */
    public static void setDarkTheme(Context context, boolean dark) {
        prefs(context).edit()
                .putString(KEY_THEME, dark ? THEME_DARK : THEME_LIGHT)
                .apply();
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private static int nightModeFromStoredTheme(Context context) {
        return isDarkTheme(context)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;
    }
}
