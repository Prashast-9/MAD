package com.example.gallaryapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "photo_manager_prefs";
    private static final String KEY_LAST_IMAGE_URI = "last_image_uri";
    private static final String KEY_LAST_IMAGE_PATH = "last_image_path";

    private Uri pendingImageUri;
    private String pendingImagePath;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCameraCapture();
                } else {
                    Toast.makeText(this, R.string.permission_camera_denied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && pendingImageUri != null) {
                    saveLastCapturedImage(pendingImageUri, pendingImagePath);
                    Toast.makeText(this, R.string.photo_saved_successfully, Toast.LENGTH_SHORT).show();
                } else {
                    deletePendingFileIfExists();
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCameraCapture();
            return;
        }
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void launchCameraCapture() {
        File imageFile = createImageFile();
        if (imageFile == null) {
            Toast.makeText(this, R.string.file_creation_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri imageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile
            );
            pendingImageUri = imageUri;
            pendingImagePath = imageFile.getAbsolutePath();
            takePictureLauncher.launch(imageUri);
        } catch (IllegalArgumentException exception) {
            deleteFileSafely(imageFile);
            Toast.makeText(this, R.string.file_provider_error, Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() {
        File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (picturesDir == null) {
            return null;
        }

        File targetDir = new File(picturesDir, "PhotoManager");
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            return null;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File imageFile = new File(targetDir, "IMG_" + timestamp + ".jpg");
        try {
            if (imageFile.createNewFile()) {
                return imageFile;
            }
        } catch (IOException exception) {
            return null;
        }
        return null;
    }

    private void saveLastCapturedImage(Uri uri, String path) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit()
                .putString(KEY_LAST_IMAGE_URI, uri.toString())
                .putString(KEY_LAST_IMAGE_PATH, path)
                .apply();
    }

    private void deletePendingFileIfExists() {
        if (pendingImagePath == null) {
            return;
        }
        deleteFileSafely(new File(pendingImagePath));
    }

    private void deleteFileSafely(File file) {
        if (file.exists()) {
            file.delete();
        }
    }
}