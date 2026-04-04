package com.example.myapplication;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View audioSection = findViewById(R.id.include_audio);
        View videoSection = findViewById(R.id.include_video);
        MaterialButtonToggleGroup modeToggle = findViewById(R.id.mode_toggle_group);

        modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.mode_audio) {
                audioSection.setVisibility(View.VISIBLE);
                videoSection.setVisibility(View.GONE);
            } else if (checkedId == R.id.mode_video) {
                audioSection.setVisibility(View.GONE);
                videoSection.setVisibility(View.VISIBLE);
            }
        });
    }
}
