package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.audio.AudioPlaybackController;
import com.example.myapplication.contract.OpenAudioDocumentContract;
import com.example.myapplication.util.MediaUriUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class MainActivity extends AppCompatActivity implements AudioPlaybackController.Listener {

    private AudioPlaybackController audioController;
    private TextView audioFileName;
    private MaterialButton btnOpenAudio;
    private MaterialButton btnPlay;
    private MaterialButton btnPause;
    private MaterialButton btnStop;
    private MaterialButton btnRestart;

    private ActivityResultLauncher<String> pickAudioLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        audioController = new AudioPlaybackController(this, this);

        pickAudioLauncher = registerForActivityResult(
                new OpenAudioDocumentContract(),
                this::onAudioDocumentPicked);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        audioFileName = findViewById(R.id.audio_file_name);
        btnOpenAudio = findViewById(R.id.btn_open_audio);
        btnPlay = findViewById(R.id.btn_play);
        btnPause = findViewById(R.id.btn_pause);
        btnStop = findViewById(R.id.btn_stop);
        btnRestart = findViewById(R.id.btn_restart);

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

        btnOpenAudio.setOnClickListener(v -> pickAudioLauncher.launch("audio/*"));

        btnPlay.setOnClickListener(v -> {
            if (!audioController.isPrepared()) {
                Toast.makeText(this, R.string.audio_msg_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            audioController.play();
        });

        btnPause.setOnClickListener(v -> {
            if (!audioController.isPrepared()) {
                Toast.makeText(this, R.string.audio_msg_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            audioController.pause();
        });

        btnStop.setOnClickListener(v -> {
            if (!audioController.isPrepared()) {
                Toast.makeText(this, R.string.audio_msg_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            audioController.stop();
            updateTransportEnabled();
        });

        btnRestart.setOnClickListener(v -> {
            if (!audioController.isPrepared()) {
                Toast.makeText(this, R.string.audio_msg_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            audioController.restartFromBeginning();
        });

        updateTransportEnabled();
    }

    private void onAudioDocumentPicked(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
        }

        audioFileName.setText(MediaUriUtils.displayName(this, uri));
        audioController.load(uri);
        updateTransportEnabled();
    }

    @Override
    public void onPrepared() {
        updateTransportEnabled();
    }

    @Override
    public void onPlaybackComplete() {
        // Controller seeks to start; controls stay enabled.
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        audioFileName.setText(R.string.audio_file_placeholder);
        updateTransportEnabled();
    }

    private void updateTransportEnabled() {
        boolean ready = audioController.isPrepared();
        btnPlay.setEnabled(ready);
        btnPause.setEnabled(ready);
        btnStop.setEnabled(ready);
        btnRestart.setEnabled(ready);
    }

    @Override
    protected void onDestroy() {
        audioController.release();
        super.onDestroy();
    }
}
