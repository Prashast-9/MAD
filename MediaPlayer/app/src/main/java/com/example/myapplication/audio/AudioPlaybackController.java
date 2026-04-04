package com.example.myapplication.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.example.myapplication.R;

import java.io.IOException;

/**
 * Wraps {@link MediaPlayer} for local audio URIs. Call {@link #release()} from {@code onDestroy()}.
 */
public final class AudioPlaybackController {

    public interface Listener {
        void onPrepared();

        void onPlaybackComplete();

        void onError(String message);
    }

    private final Context appContext;
    private final Listener listener;

    @Nullable
    private MediaPlayer mediaPlayer;
    @Nullable
    private Uri currentUri;
    private boolean prepared;
    private boolean awaitingSeekAfterStop;

    public AudioPlaybackController(Context context, Listener listener) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;
    }

    @Nullable
    public Uri getCurrentUri() {
        return currentUri;
    }

    public boolean isPrepared() {
        return prepared && mediaPlayer != null;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    /**
     * Releases any existing player and loads a new URI (prepare is asynchronous).
     */
    public void load(Uri uri) {
        releasePlayerInternal();
        currentUri = uri;
        prepared = false;
        awaitingSeekAfterStop = false;

        MediaPlayer mp = new MediaPlayer();
        mediaPlayer = mp;
        try {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            mp.setAudioAttributes(attrs);
            mp.setDataSource(appContext, uri);
            mp.setOnPreparedListener(m -> {
                prepared = true;
                if (awaitingSeekAfterStop) {
                    awaitingSeekAfterStop = false;
                    try {
                        m.seekTo(0);
                    } catch (IllegalStateException ignored) {
                    }
                }
                listener.onPrepared();
            });
            mp.setOnCompletionListener(m -> {
                try {
                    m.seekTo(0);
                } catch (IllegalStateException ignored) {
                }
                listener.onPlaybackComplete();
            });
            mp.setOnErrorListener((m, what, extra) -> {
                prepared = false;
                currentUri = null;
                listener.onError(appContext.getString(R.string.audio_error_playback));
                releasePlayerInternal();
                return true;
            });
            mp.prepareAsync();
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            prepared = false;
            currentUri = null;
            releasePlayerInternal();
            listener.onError(appContext.getString(R.string.audio_error_load));
        }
    }

    public void play() {
        if (!isPrepared()) {
            return;
        }
        try {
            mediaPlayer.start();
        } catch (IllegalStateException e) {
            listener.onError(appContext.getString(R.string.audio_error_playback));
        }
    }

    public void pause() {
        if (!isPrepared()) {
            return;
        }
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        } catch (IllegalStateException e) {
            listener.onError(appContext.getString(R.string.audio_error_playback));
        }
    }

    /**
     * Stops playback and returns to the start after preparation completes.
     */
    public void stop() {
        if (mediaPlayer == null || !prepared) {
            return;
        }
        try {
            mediaPlayer.stop();
            prepared = false;
            awaitingSeekAfterStop = true;
            mediaPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            prepared = false;
            currentUri = null;
            listener.onError(appContext.getString(R.string.audio_error_playback));
            releasePlayerInternal();
        }
    }

    public void restartFromBeginning() {
        if (!isPrepared()) {
            return;
        }
        try {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        } catch (IllegalStateException e) {
            listener.onError(appContext.getString(R.string.audio_error_playback));
        }
    }

    public void release() {
        currentUri = null;
        prepared = false;
        awaitingSeekAfterStop = false;
        releasePlayerInternal();
    }

    private void releasePlayerInternal() {
        if (mediaPlayer == null) {
            return;
        }
        try {
            mediaPlayer.reset();
        } catch (IllegalStateException ignored) {
        }
        mediaPlayer.release();
        mediaPlayer = null;
        prepared = false;
    }
}
