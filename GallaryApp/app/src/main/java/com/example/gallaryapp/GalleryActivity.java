package com.example.gallaryapp;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    private static final String TAG = "GalleryActivity";
    private static final String PHOTO_MANAGER_RELATIVE_PATH = Environment.DIRECTORY_PICTURES + "/PhotoManager/";

    private final List<Uri> imageUris = new ArrayList<>();
    private GalleryAdapter galleryAdapter;
    private TextView emptyStateText;

    private final ActivityResultLauncher<String> readPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    loadImagesFromPhotoManager();
                } else {
                    Toast.makeText(this, R.string.permission_gallery_denied, Toast.LENGTH_SHORT).show();
                    imageUris.clear();
                    galleryAdapter.notifyDataSetChanged();
                    updateEmptyState();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        MaterialButton selectFolderButton = findViewById(R.id.btnSelectFolder);
        RecyclerView galleryRecyclerView = findViewById(R.id.galleryRecyclerView);
        emptyStateText = findViewById(R.id.txtEmptyState);

        galleryAdapter = new GalleryAdapter(imageUris, uri -> {
            Intent detailsIntent = new Intent(GalleryActivity.this, ImageDetailsActivity.class);
            detailsIntent.putExtra(ImageDetailsActivity.EXTRA_IMAGE_URI, uri.toString());
            startActivity(detailsIntent);
        });

        galleryRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        galleryRecyclerView.setAdapter(galleryAdapter);

        selectFolderButton.setText(R.string.refresh_gallery);
        selectFolderButton.setOnClickListener(v -> loadImagesWithPermissionCheck());
        loadImagesWithPermissionCheck();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadImagesWithPermissionCheck();
    }

    private void loadImagesWithPermissionCheck() {
        String permission;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadImagesFromPhotoManager();
        } else {
            readPermissionLauncher.launch(permission);
        }
    }

    private void loadImagesFromPhotoManager() {
        imageUris.clear();
        String[] projection = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.MIME_TYPE};
        String selection;
        String[] selectionArgs;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            selection = MediaStore.Images.Media.RELATIVE_PATH + "=?";
            selectionArgs = new String[]{PHOTO_MANAGER_RELATIVE_PATH};
        } else {
            selection = MediaStore.Images.Media.DATA + " LIKE ?";
            selectionArgs = new String[]{"%/Pictures/PhotoManager/%"};
        }

        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE);
                while (cursor.moveToNext()) {
                    String mimeType = cursor.getString(mimeColumn);
                    if (!isSupportedImageMime(mimeType)) {
                        continue;
                    }
                    long id = cursor.getLong(idColumn);
                    Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    imageUris.add(imageUri);
                    Log.d(TAG, "Loaded image URI: " + imageUri);
                }
            }
            Log.d(TAG, "Loaded images count: " + imageUris.size());
        } catch (Exception exception) {
            Log.e(TAG, "Failed loading images", exception);
            Toast.makeText(this, R.string.gallery_load_failed, Toast.LENGTH_SHORT).show();
        }

        galleryAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private boolean isSupportedImageMime(String mimeType) {
        return "image/jpeg".equalsIgnoreCase(mimeType)
                || "image/jpg".equalsIgnoreCase(mimeType)
                || "image/png".equalsIgnoreCase(mimeType);
    }

    private void updateEmptyState() {
        emptyStateText.setText(R.string.no_images_available);
        emptyStateText.setVisibility(imageUris.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
