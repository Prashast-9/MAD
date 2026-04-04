package com.example.myapplication.video;

import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Streams video into a {@link VideoView}. Does not manage audio.
 */
public final class VideoStreamViewController {

    public interface Listener {
        void onVideoPrepared();

        void onVideoError();

        /** True while network buffering (including initial load). */
        void onVideoBufferingChanged(boolean buffering);
    }

    private final VideoView videoView;
    private final View placeholder;
    private final ProgressBar loading;
    private final Listener listener;

    private boolean prepared;
    @Nullable
    private Uri currentUri;

    public VideoStreamViewController(
            VideoView videoView,
            View placeholder,
            ProgressBar loading,
            Listener listener) {
        this.videoView = videoView;
        this.placeholder = placeholder;
        this.loading = loading;
        this.listener = listener;
        this.videoView.setOnPreparedListener(mp -> {
            prepared = true;
            loading.setVisibility(View.GONE);
            placeholder.setVisibility(View.GONE);
            listener.onVideoBufferingChanged(false);
            listener.onVideoPrepared();
        });
        this.videoView.setOnErrorListener((mp, what, extra) -> {
            prepared = false;
            currentUri = null;
            loading.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
            listener.onVideoBufferingChanged(false);
            listener.onVideoError();
            return true;
        });
        this.videoView.setOnInfoListener((mp, what, extra) -> {
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                loading.setVisibility(View.VISIBLE);
                listener.onVideoBufferingChanged(true);
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                if (prepared) {
                    loading.setVisibility(View.GONE);
                }
                listener.onVideoBufferingChanged(false);
            }
            return false;
        });
    }

    public boolean isPrepared() {
        return prepared;
    }

    public boolean isPlaying() {
        return prepared && videoView.isPlaying();
    }

    public int getCurrentPositionMs() {
        if (!prepared) {
            return 0;
        }
        return videoView.getCurrentPosition();
    }

    /** Whether the loading spinner is visible (initial load or buffering). */
    public boolean isLoadingVisible() {
        return loading.getVisibility() == View.VISIBLE;
    }

    @Nullable
    public Uri getCurrentUri() {
        return currentUri;
    }

    public void load(@NonNull Uri uri) {
        prepared = false;
        currentUri = uri;
        loading.setVisibility(View.VISIBLE);
        placeholder.setVisibility(View.VISIBLE);
        listener.onVideoBufferingChanged(true);
        videoView.setVideoURI(uri);
        videoView.requestFocus();
    }

    public void play() {
        if (!prepared) {
            return;
        }
        videoView.start();
    }

    public void pause() {
        if (!prepared) {
            return;
        }
        if (videoView.canPause()) {
            videoView.pause();
        }
    }

    public void pauseIfPlaying() {
        if (prepared && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    /**
     * Pauses and seeks to the start without reloading (used when audio takes over playback).
     */
    public void pauseAndSeekToStart() {
        if (!prepared) {
            return;
        }
        videoView.pause();
        videoView.seekTo(0);
    }

    /**
     * Stops {@link VideoView} playback and reloads the same URI so the next Play starts clean.
     */
    public void stopFullyAndReload() {
        if (currentUri == null) {
            return;
        }
        Uri uri = currentUri;
        videoView.stopPlayback();
        prepared = false;
        loading.setVisibility(View.VISIBLE);
        placeholder.setVisibility(View.VISIBLE);
        listener.onVideoBufferingChanged(true);
        videoView.setVideoURI(uri);
        videoView.requestFocus();
    }

    public void restartFromBeginning() {
        if (!prepared) {
            return;
        }
        videoView.seekTo(0);
        videoView.start();
    }

    public void pauseForLifecycle() {
        pauseIfPlaying();
    }

    public void release() {
        videoView.stopPlayback();
        prepared = false;
        currentUri = null;
        loading.setVisibility(View.GONE);
        placeholder.setVisibility(View.VISIBLE);
        listener.onVideoBufferingChanged(false);
    }
}
