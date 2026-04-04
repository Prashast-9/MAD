package com.example.myapplication.util;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Basic validation for HTTP(S) stream URLs.
 */
public final class VideoUrlValidator {

    public enum Result {
        OK,
        EMPTY,
        INVALID_SCHEME
    }

    private VideoUrlValidator() {
    }

    @NonNull
    public static Result validate(@Nullable String raw) {
        if (raw == null) {
            return Result.EMPTY;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Result.EMPTY;
        }
        Uri uri = Uri.parse(trimmed);
        String scheme = uri.getScheme();
        if (scheme == null
                || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            return Result.INVALID_SCHEME;
        }
        return Result.OK;
    }

    @NonNull
    public static String normalizedUrl(@NonNull String raw) {
        return raw.trim();
    }
}
