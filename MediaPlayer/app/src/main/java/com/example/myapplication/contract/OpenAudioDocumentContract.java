package com.example.myapplication.contract;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Launches {@link Intent#ACTION_OPEN_DOCUMENT} restricted to a MIME type (e.g. {@code audio/*}).
 */
public final class OpenAudioDocumentContract extends ActivityResultContract<String, Uri> {

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, @NonNull String mimeType) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        return intent;
    }

    @Nullable
    @Override
    public Uri parseResult(int resultCode, @Nullable Intent intent) {
        if (resultCode != Activity.RESULT_OK || intent == null) {
            return null;
        }
        return intent.getData();
    }
}
