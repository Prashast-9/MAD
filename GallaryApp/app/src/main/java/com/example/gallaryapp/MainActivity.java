package com.example.gallaryapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "photo_manager_prefs";
    private static final String KEY_LAST_IMAGE_URI = "last_image_uri";
    private static final String PHOTO_MANAGER_RELATIVE_PATH = Environment.DIRECTORY_PICTURES + "/PhotoManager/";

    private Uri pendingImageUri;

    private final ActivityResultLauncher<String[]> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean cameraGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                boolean writeGranted = true;
                if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
                    writeGranted = Boolean.TRUE.equals(result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE));
                }
                if (cameraGranted && writeGranted) {
                    launchCameraCapture();
                } else {
                    Toast.makeText(this, R.string.permission_capture_denied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && pendingImageUri != null) {
                    saveLastCapturedImage(pendingImageUri);
                    Log.d(TAG, "Saved image URI: " + pendingImageUri);
                    Toast.makeText(this, R.string.photo_saved_successfully, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.camera_capture_cancelled, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialButton takePhotoButton = findViewById(R.id.btnTakePhoto);
        MaterialButton openGalleryButton = findViewById(R.id.btnOpenGallery);

        takePhotoButton.setOnClickListener(v -> openCameraWithPermissionCheck());

        openGalleryButton.setOnClickListener(
                v -> startActivity(new Intent(MainActivity.this, GalleryActivity.class))
        );
    }

    private void openCameraWithPermissionCheck() {
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasWritePermission = true;
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        if (hasCameraPermission && hasWritePermission) {
            launchCameraCapture();
            return;
        }

        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            cameraPermissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            });
        } else {
            cameraPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
        }
    }

    private void launchCameraCapture() {
        Uri imageUri = createImageUriInPictures();
        if (imageUri == null) {
            Toast.makeText(this, R.string.file_creation_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        pendingImageUri = imageUri;
        takePictureLauncher.launch(imageUri);
    }

    private Uri createImageUriInPictures() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "IMG_" + timestamp + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, PHOTO_MANAGER_RELATIVE_PATH);
        } else {
            java.io.File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            java.io.File targetDir = new java.io.File(picturesDir, "PhotoManager");
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory: " + targetDir.getAbsolutePath());
                return null;
            }
            values.put(MediaStore.Images.Media.DATA, new java.io.File(targetDir, fileName).getAbsolutePath());
        }

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            Log.d(TAG, "Created image URI for capture: " + uri);
        } else {
            Log.e(TAG, "Failed to create image URI in MediaStore");
        }
        return uri;
    }

    private void saveLastCapturedImage(Uri uri) {
        if (uri == null) {
            return;
        }
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit()
                .putString(KEY_LAST_IMAGE_URI, uri.toString())
                .apply();
    }
}