package com.example.gallaryapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class GalleryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        MaterialButton openDetailsButton = findViewById(R.id.btnOpenSampleDetails);
        openDetailsButton.setOnClickListener(
                v -> startActivity(new Intent(GalleryActivity.this, ImageDetailsActivity.class))
        );
    }
}
