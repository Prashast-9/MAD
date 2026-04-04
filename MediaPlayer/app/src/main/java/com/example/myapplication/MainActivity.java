package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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
import com.example.myapplication.util.VideoUrlValidator;
import com.example.myapplication.video.VideoStreamViewController;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity
        implements AudioPlaybackController.Listener, VideoStreamViewController.Listener {

    private AudioPlaybackController audioController;
    private VideoStreamViewController videoController;
    private MaterialButtonToggleGroup modeToggle;

    private TextView audioFileName;
    private MaterialButton btnOpenAudio;
    private MaterialButton btnPlay;
    private MaterialButton btnPause;
    private MaterialButton btnStop;
    private MaterialButton btnRestart;

    private TextInputLayout layoutVideoUrl;
    private TextInputEditText inputVideoUrl;
    private MaterialButton btnLoadVideoUrl;

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

        layoutVideoUrl = findViewById(R.id.layout_video_url);
        inputVideoUrl = findViewById(R.id.input_video_url);
        btnLoadVideoUrl = findViewById(R.id.btn_load_video_url);

        VideoView videoView = findViewById(R.id.video_view);
        View videoPlaceholder = findViewById(R.id.video_placeholder);
        ProgressBar videoLoading = findViewById(R.id.video_loading);
        videoController = new VideoStreamViewController(videoView, videoPlaceholder, videoLoading, this);

        View audioSection = findViewById(R.id.include_audio);
        View videoSection = findViewById(R.id.include_video);
        modeToggle = findViewById(R.id.mode_toggle_group);

        modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.mode_audio) {
                audioSection.setVisibility(View.VISIBLE);
                videoSection.setVisibility(View.GONE);
                videoController.pauseIfPlaying();
            } else if (checkedId == R.id.mode_video) {
                audioSection.setVisibility(View.GONE);
                videoSection.setVisibility(View.VISIBLE);
                audioController.pause();
            }
            updateTransportEnabled();
        });

        btnOpenAudio.setOnClickListener(v -> pickAudioLauncher.launch("audio/*"));

        btnLoadVideoUrl.setOnClickListener(v -> onLoadVideoClicked());

        btnPlay.setOnClickListener(v -> onPlayClicked());
        btnPause.setOnClickListener(v -> onPauseClicked());
        btnStop.setOnClickListener(v -> onStopClicked());
        btnRestart.setOnClickListener(v -> onRestartClicked());

        updateTransportEnabled();
    }

    private boolean isVideoMode() {
        return modeToggle.getCheckedButtonId() == R.id.mode_video;
    }

    private void onLoadVideoClicked() {
        layoutVideoUrl.setError(null);
        String raw = String.valueOf(inputVideoUrl.getText());
        VideoUrlValidator.Result result = VideoUrlValidator.validate(raw);
        if (result == VideoUrlValidator.Result.EMPTY) {
            layoutVideoUrl.setError(getString(R.string.video_error_empty));
            return;
        }
        if (result == VideoUrlValidator.Result.INVALID_SCHEME) {
            layoutVideoUrl.setError(getString(R.string.video_error_invalid_url));
            return;
        }

        String normalized = VideoUrlValidator.normalizedUrl(raw);
        audioController.pause();
        videoController.load(Uri.parse(normalized));
        updateTransportEnabled();
    }

    private void onPlayClicked() {
        if (isVideoMode()) {
            if (!videoController.isPrepared()) {
                Toast.makeText(this, R.string.video_msg_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            audioController.pause();
            videoController.play();
        } else {
            if (!audioController.isPrepared()) {
                Toast.makeText(this, R.string.audio_msg_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            videoController.pauseIfPlaying();
            audioController.play();
        }
    }

    private void onPauseClicked() {
        if (isVideoMode()) {
            if (!videoController.isPrepared()) {
                Toast.makeText(this, R.string.video_msg_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            videoController.pause();
        } else {
            if (!audioController.isPrepared()) {
                Toast.makeText(this, R.string.audio_msg_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            audioController.pause();
        }
    }

    private void onStopClicked() {
        if (isVideoMode()) {
            if (!videoController.isPrepared()) {
                Toast.makeText(this, R.string.video_msg_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            videoController.stopAndResetPosition();
        } else {
            if (!audioController.isPrepared()) {
                Toast.makeText(this, R.string.audio_msg_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            audioController.stop();
        }
        updateTransportEnabled();
    }

    private void onRestartClicked() {
        if (isVideoMode()) {
            if (!videoController.isPrepared()) {
                Toast.makeText(this, R.string.video_msg_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            audioController.pause();
            videoController.restartFromBeginning();
        } else {
            if (!audioController.isPrepared()) {
                Toast.makeText(this, R.string.audio_msg_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }
            videoController.pauseIfPlaying();
            audioController.restartFromBeginning();
        }
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

        videoController.pauseIfPlaying();
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

    @Override
    public void onVideoPrepared() {
        updateTransportEnabled();
    }

    @Override
    public void onVideoError() {
        Toast.makeText(this, R.string.video_error_playback, Toast.LENGTH_LONG).show();
        updateTransportEnabled();
    }

    private void updateTransportEnabled() {
        boolean ready = audioController.isPrepared() || videoController.isPrepared();
        btnPlay.setEnabled(ready);
        btnPause.setEnabled(ready);
        btnStop.setEnabled(ready);
        btnRestart.setEnabled(ready);
    }

    @Override
    protected void onPause() {
        videoController.pauseForLifecycle();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        videoController.release();
        audioController.release();
        super.onDestroy();
    }
}
