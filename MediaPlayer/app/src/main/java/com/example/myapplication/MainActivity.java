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
import androidx.annotation.StringRes;
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

/**
 * Hosts audio (document URI) and video (HTTP stream) playback with shared transport controls.
 * Only one medium plays at a time; the active tab determines which source the buttons target.
 */
public class MainActivity extends AppCompatActivity
        implements AudioPlaybackController.Listener, VideoStreamViewController.Listener {

    private AudioPlaybackController audioController;
    private VideoStreamViewController videoController;
    private MaterialButtonToggleGroup modeToggle;
    private TextView modeActiveLabel;
    private TextView playbackStatus;

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

        playbackStatus = findViewById(R.id.playback_status);
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
        modeActiveLabel = findViewById(R.id.mode_active_label);

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
                if (audioController.isPlaying()) {
                    audioController.pause();
                }
            }
            updateModeIndicator();
            updateTransportState();
            refreshPlaybackStatus();
        });

        btnOpenAudio.setOnClickListener(v -> pickAudioLauncher.launch("audio/*"));

        btnLoadVideoUrl.setOnClickListener(v -> onLoadVideoClicked());

        btnPlay.setOnClickListener(v -> onPlayClicked());
        btnPause.setOnClickListener(v -> onPauseClicked());
        btnStop.setOnClickListener(v -> onStopClicked());
        btnRestart.setOnClickListener(v -> onRestartClicked());

        updateModeIndicator();
        updateTransportState();
        refreshPlaybackStatus();
    }

    private boolean isVideoMode() {
        return modeToggle.getCheckedButtonId() == R.id.mode_video;
    }

    private void updateModeIndicator() {
        modeActiveLabel.setText(
                isVideoMode() ? R.string.mode_active_video : R.string.mode_active_audio);
    }

    /**
     * Stops local audio so video (load or play) never overlaps with audio output.
     */
    private void stopAudioForVideoHandoff() {
        if (audioController.isPrepared()) {
            audioController.stop();
        }
    }

    /**
     * Stops video stream position when audio should own playback.
     */
    private void stopVideoForAudioHandoff() {
        if (videoController.isPrepared()) {
            videoController.pauseAndSeekToStart();
        }
    }

    private void onLoadVideoClicked() {
        layoutVideoUrl.setError(null);
        String raw = String.valueOf(inputVideoUrl.getText());
        VideoUrlValidator.Result result = VideoUrlValidator.validate(raw);
        if (result == VideoUrlValidator.Result.EMPTY) {
            layoutVideoUrl.setError(getString(R.string.video_error_empty));
            refreshPlaybackStatus();
            return;
        }
        if (result == VideoUrlValidator.Result.INVALID_SCHEME) {
            layoutVideoUrl.setError(getString(R.string.video_error_invalid_url));
            refreshPlaybackStatus();
            return;
        }

        String normalized = VideoUrlValidator.normalizedUrl(raw);
        stopAudioForVideoHandoff();
        videoController.load(Uri.parse(normalized));
        updateTransportState();
        refreshPlaybackStatus();
    }

    private void onPlayClicked() {
        if (isVideoMode()) {
            if (!videoController.isPrepared()) {
                return;
            }
            stopAudioForVideoHandoff();
            videoController.play();
        } else {
            if (!audioController.isPrepared()) {
                return;
            }
            stopVideoForAudioHandoff();
            audioController.play();
        }
        updateTransportState();
        refreshPlaybackStatus();
    }

    private void onPauseClicked() {
        if (isVideoMode()) {
            if (videoController.isPlaying()) {
                videoController.pause();
            }
        } else {
            if (audioController.isPlaying()) {
                audioController.pause();
            }
        }
        updateTransportState();
        refreshPlaybackStatus();
    }

    private void onStopClicked() {
        if (isVideoMode()) {
            if (!videoController.isPrepared()) {
                return;
            }
            videoController.stopFullyAndReload();
        } else {
            if (!audioController.isPrepared()) {
                return;
            }
            audioController.stop();
        }
        updateTransportState();
        refreshPlaybackStatus();
    }

    private void onRestartClicked() {
        if (isVideoMode()) {
            if (!videoController.isPrepared()) {
                return;
            }
            stopAudioForVideoHandoff();
            videoController.restartFromBeginning();
        } else {
            if (!audioController.isPrepared()) {
                return;
            }
            stopVideoForAudioHandoff();
            audioController.restartFromBeginning();
        }
        updateTransportState();
        refreshPlaybackStatus();
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

        stopVideoForAudioHandoff();
        audioFileName.setText(MediaUriUtils.displayName(this, uri));
        audioController.load(uri);
        updateTransportState();
        refreshPlaybackStatus();
    }

    /**
     * Enables transport for the current tab when that source is prepared; Pause only while playing.
     */
    private void updateTransportState() {
        boolean modeVideo = isVideoMode();
        boolean sourceReady = modeVideo ? videoController.isPrepared() : audioController.isPrepared();
        boolean canPause = modeVideo ? videoController.isPlaying() : audioController.isPlaying();

        btnPlay.setEnabled(sourceReady);
        btnStop.setEnabled(sourceReady);
        btnRestart.setEnabled(sourceReady);
        btnPause.setEnabled(sourceReady && canPause);
    }

    private void refreshPlaybackStatus() {
        if (playbackStatus == null) {
            return;
        }
        if (videoController.isLoadingVisible()) {
            setStatusText(R.string.status_buffering);
            return;
        }
        boolean playing = audioController.isPlaying() || videoController.isPlaying();
        if (playing) {
            setStatusText(R.string.status_playing);
            return;
        }
        if (isVideoMode()) {
            if (!videoController.isPrepared()) {
                setStatusText(R.string.status_hint_video);
                return;
            }
            setPausedOrStopped(videoController.getCurrentPositionMs());
            return;
        }
        if (!audioController.isPrepared()) {
            setStatusText(R.string.status_hint_audio);
            return;
        }
        setPausedOrStopped(audioController.getCurrentPositionMs());
    }

    private void setPausedOrStopped(int positionMs) {
        if (positionMs > 250) {
            setStatusText(R.string.status_paused);
        } else {
            setStatusText(R.string.status_stopped);
        }
    }

    private void setStatusText(@StringRes int resId) {
        playbackStatus.setText(resId);
    }

    @Override
    public void onPrepared() {
        updateTransportState();
        refreshPlaybackStatus();
    }

    @Override
    public void onPlaybackComplete() {
        updateTransportState();
        refreshPlaybackStatus();
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        audioFileName.setText(R.string.audio_file_placeholder);
        updateTransportState();
        refreshPlaybackStatus();
    }

    @Override
    public void onVideoPrepared() {
        updateTransportState();
        refreshPlaybackStatus();
    }

    @Override
    public void onVideoError() {
        Toast.makeText(this, R.string.video_error_playback, Toast.LENGTH_LONG).show();
        updateTransportState();
        refreshPlaybackStatus();
    }

    @Override
    public void onVideoBufferingChanged(boolean buffering) {
        refreshPlaybackStatus();
    }

    @Override
    protected void onPause() {
        if (audioController.isPlaying()) {
            audioController.pause();
        }
        videoController.pauseForLifecycle();
        updateTransportState();
        refreshPlaybackStatus();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        videoController.release();
        audioController.release();
        super.onDestroy();
    }
}
