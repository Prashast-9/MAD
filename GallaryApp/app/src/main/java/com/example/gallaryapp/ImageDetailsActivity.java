package com.example.gallaryapp;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_details);

        ImageView imagePreview = findViewById(R.id.imgPreview);
        TextView imageNameText = findViewById(R.id.txtImageName);
        TextView imagePathText = findViewById(R.id.txtImagePath);
        TextView imageSizeText = findViewById(R.id.txtImageSize);
        TextView imageDateText = findViewById(R.id.txtImageDate);
        MaterialButton deleteButton = findViewById(R.id.btnDeleteImage);

        String imageUriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (imageUriString == null) {
            Toast.makeText(this, R.string.image_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        imageUri = Uri.parse(imageUriString);

        Glide.with(this)
                .load(imageUri)
                .fitCenter()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert)
                .into(imagePreview);

        ImageMetadata metadata = readImageMetadata(imageUri);
        imageNameText.setText(getString(R.string.image_name_format, metadata.name));
        imagePathText.setText(getString(R.string.image_uri_format, imageUri.toString()));
        imageSizeText.setText(getString(R.string.image_size_format, formatFileSize(metadata.sizeBytes)));
        imageDateText.setText(getString(R.string.image_date_format, formatLastModified(metadata.lastModified)));

        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void showDeleteConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.delete_confirmation_message)
                .setPositiveButton(R.string.yes, (DialogInterface dialog, int which) -> deleteCurrentImage())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void deleteCurrentImage() {
        if (imageUri == null) {
            Toast.makeText(this, R.string.image_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean deleted = false;
        try {
            DocumentFile file = DocumentFile.fromSingleUri(this, imageUri);
            if (file != null && file.exists()) {
                deleted = file.delete();
            }
            if (!deleted) {
                int rows = getContentResolver().delete(imageUri, null, null);
                deleted = rows > 0;
            }
        } catch (SecurityException exception) {
            Toast.makeText(this, R.string.delete_permission_denied, Toast.LENGTH_SHORT).show();
            return;
        } catch (Exception exception) {
            Toast.makeText(this, R.string.delete_image_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (deleted) {
            Toast.makeText(this, R.string.image_deleted_successfully, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.delete_image_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private ImageMetadata readImageMetadata(Uri uri) {
        String name = getString(R.string.unknown_value);
        long sizeBytes = -1L;
        long lastModified = 0L;

        try (android.database.Cursor cursor = getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex);
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    sizeBytes = cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception ignored) {
            // Keep fallback values to avoid crashing on inaccessible metadata.
        }

        DocumentFile file = DocumentFile.fromSingleUri(this, uri);
        if (file != null) {
            if (name.equals(getString(R.string.unknown_value)) && file.getName() != null) {
                name = file.getName();
            }
            if (sizeBytes < 0) {
                sizeBytes = file.length();
            }
            lastModified = file.lastModified();
        }

        return new ImageMetadata(name, sizeBytes, lastModified);
    }

    private String formatFileSize(long sizeBytes) {
        if (sizeBytes < 0) {
            return getString(R.string.unknown_value);
        }
        double sizeKb = sizeBytes / 1024.0;
        if (sizeKb < 1024.0) {
            return new DecimalFormat("0.00").format(sizeKb) + " KB";
        }
        double sizeMb = sizeKb / 1024.0;
        return new DecimalFormat("0.00").format(sizeMb) + " MB";
    }

    private String formatLastModified(long lastModified) {
        if (lastModified <= 0L) {
            return getString(R.string.unknown_value);
        }
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date(lastModified));
    }

    private static class ImageMetadata {
        final String name;
        final long sizeBytes;
        final long lastModified;

        ImageMetadata(String name, long sizeBytes, long lastModified) {
            this.name = name;
            this.sizeBytes = sizeBytes;
            this.lastModified = lastModified;
        }
    }
}
