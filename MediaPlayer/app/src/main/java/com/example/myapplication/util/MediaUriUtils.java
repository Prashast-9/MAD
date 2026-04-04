package com.example.myapplication.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;

/**
 * Resolves a human-readable file name for content or file URIs.
 */
public final class MediaUriUtils {

    private MediaUriUtils() {
    }

    @NonNull
    public static String displayName(@NonNull Context context, @NonNull Uri uri) {
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null,
                    null,
                    null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        String name = cursor.getString(index);
                        if (name != null && !name.isEmpty()) {
                            return name;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        String path = uri.getPath();
        if (path != null) {
            int slash = path.lastIndexOf('/');
            if (slash >= 0 && slash < path.length() - 1) {
                return path.substring(slash + 1);
            }
            if (!path.isEmpty()) {
                return path;
            }
        }

        return uri.toString();
    }
}
