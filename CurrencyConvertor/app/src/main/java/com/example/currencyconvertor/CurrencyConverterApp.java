package com.example.currencyconvertor;

import android.app.Application;

/**
 * Applies persisted night mode before any activity is shown.
 */
public class CurrencyConverterApp extends Application {

    @Override
    public void onCreate() {
        ThemeManager.applyPersistedTheme(this);
        super.onCreate();
    }
}
