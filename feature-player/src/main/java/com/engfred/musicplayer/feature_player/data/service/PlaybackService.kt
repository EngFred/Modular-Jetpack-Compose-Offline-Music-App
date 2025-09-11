package com.engfred.musicplayer.feature_player.data.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.engfred.musicplayer.core.data.source.SharedAudioDataSource;
import com.engfred.musicplayer.core.domain.repository.LibraryRepository;
import com.engfred.musicplayer.core.domain.repository.PlaybackController;  // Add this import
import dagger.hilt.android.AndroidEntryPoint;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.SupervisorJob;
import kotlinx.coroutines.cancel;
import kotlinx.coroutines.delay;
import kotlinx.coroutines.flow.first;
import kotlinx.coroutines.launch;
import java.lang.Exception;
import javax.inject.Inject;
import androidx.core.graphics.createBitmap;
import com.engfred.musicplayer.core.domain.repository.SettingsRepository;
import com.engfred.musicplayer.core.domain.model.FilterOption;
import com.engfred.musicplayer.core.domain.repository.RepeatMode

const val MUSIC_NOTIFICATION_CHANNEL_ID = "music_playback_channel";
const val MUSIC_NOTIFICATION_ID = 101;

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer;

    @Inject
    lateinit var musicNotificationProvider: MusicNotificationProvider;

    @Inject
    lateinit var playbackController: PlaybackController;

    @Inject
    lateinit var libRepo: LibraryRepository;

    @Inject
    lateinit var sharedAudioDataSource: SharedAudioDataSource;

    @Inject
    lateinit var settingsRepository: SettingsRepository; //we need to get the last sort order/filter

    private var mediaSession: MediaSession? = null;
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main);

    companion object {
        // NOTE: we use string actions that match your app module's PlayerWidgetProvider actions.
        // Keep these strings identical to the ones in the provider class in the app module.
        const val ACTION_WIDGET_PLAY_PAUSE = "com.engfred.musicplayer.ACTION_WIDGET_PLAY_PAUSE";
        const val ACTION_WIDGET_NEXT = "com.engfred.musicplayer.ACTION_WIDGET_NEXT";
        const val ACTION_WIDGET_PREV = "com.engfred.musicplayer.ACTION_WIDGET_PREV";
        // Internal: request widget refresh
        const val ACTION_REFRESH_WIDGET = "com.engfred.musicplayer.ACTION_REFRESH_WIDGET";
        const val ACTION_WIDGET_REPEAT = "com.engfred.musicplayer.ACTION_WIDGET_REPEAT";
        // Fully qualified class name of the widget provider (in the app module)
        const val WIDGET_PROVIDER_CLASS = "com.engfred.musicplayer.widget.PlayerWidgetProvider";
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate();
        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = NotificationCompat.Builder(this, MUSIC_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Music Player")
                .setContentText("Starting music service...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setSilent(true)
                .build();
            try {
                startForeground(MUSIC_NOTIFICATION_ID, notification);
            } catch (e: Exception) {
                stopSelf();
                return;
            }
        }

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();
            exoPlayer.setAudioAttributes(audioAttributes, true);
            exoPlayer.setHandleAudioBecomingNoisy(true);

            // Create PendingIntent to launch MainActivity when notification is clicked
            val intent = Intent().setClassName(this, "${packageName}.MainActivity");
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0;
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

            mediaSession = MediaSession.Builder(this, exoPlayer)
                .setSessionActivity(pendingIntent)
                .build();

            setMediaNotificationProvider(musicNotificationProvider);

            // Listen for player changes so we can refresh widget UI
            exoPlayer.addListener(object : Player.Listener {
                @RequiresApi(Build.VERSION_CODES.P)
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying);
                    updateWidget();
                }

                @RequiresApi(Build.VERSION_CODES.P)
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason);
                    updateWidget();
                }

                @RequiresApi(Build.VERSION_CODES.P)
                override fun onPositionDiscontinuity(reason: Int) {
                    super.onPositionDiscontinuity(reason);
                    updateWidget();
                }
            });

            // Start periodic update for duration
            serviceScope.launch {
                while (true) {
                    delay(1000);
                    if (exoPlayer.isPlaying) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            updateWidget()
                        };
                    }
                }
            };
        } catch (e: Exception) {
            stopSelf();
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_WIDGET_PLAY_PAUSE -> {
                    handlePlayPauseFromWidget();
                    updateWidget();
                }
                ACTION_WIDGET_NEXT -> {
                    try { exoPlayer.seekToNextMediaItem(); } catch (e: Exception) {}
                    updateWidget();
                }
                ACTION_WIDGET_PREV -> {
                    try { exoPlayer.seekToPreviousMediaItem(); } catch (e: Exception) {}
                    updateWidget();
                }
                ACTION_REFRESH_WIDGET -> updateWidget();
                ACTION_WIDGET_REPEAT -> {
                    handleRepeatToggle();
                    updateWidget();
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun handlePlayPauseFromWidget() {
        try {
            serviceScope.launch {
                if (exoPlayer.mediaItemCount == 0) {
                    // No media loaded; fetch all songs and start playback (non-shuffled, from first)
                    val deviceAudios = libRepo.getAllAudioFiles().first();
                    if (deviceAudios.isNotEmpty()) {
                        val filter = settingsRepository.getFilterOption().first();
                        val repeatMode = settingsRepository.getAppSettings().first().repeatMode
                        val shuffleMode = settingsRepository.getAppSettings().first().shuffleMode
                        val sortedAudios = when (filter) {
                            FilterOption.DATE_ADDED_ASC -> deviceAudios.sortedBy { it.dateAdded };
                            FilterOption.DATE_ADDED_DESC -> deviceAudios.sortedByDescending { it.dateAdded };
                            FilterOption.LENGTH_ASC -> deviceAudios.sortedBy { it.duration };
                            FilterOption.LENGTH_DESC -> deviceAudios.sortedByDescending { it.duration };
                            FilterOption.ALPHABETICAL_ASC -> deviceAudios.sortedBy { it.title.lowercase() }
                            FilterOption.ALPHABETICAL_DESC -> deviceAudios.sortedByDescending { it.title.lowercase() }
                        };
                        sharedAudioDataSource.setPlayingQueue(sortedAudios);
                        playbackController.setRepeatMode(repeatMode)
                        playbackController.setShuffleMode(shuffleMode)
                        playbackController.initiatePlayback(sortedAudios.first().uri);  // Delegates to controller (no duplication)
                    }
                    // If no songs or error, do nothing (widget remains "No song playing" until user adds songs via app)
                } else {
                    playbackController.playPause();  // Use controller for consistency
                }
            };
        } catch (e: Exception) {
            // Ignore errors to prevent crashes
        }
        updateWidget();  // Explicit update after toggle (listeners will handle further changes)
    }

    private fun handleRepeatToggle() {
        val current = exoPlayer.repeatMode;
        val next = when (current) {
            Player.REPEAT_MODE_OFF -> {
                serviceScope.launch {
                    settingsRepository.updateRepeatMode(RepeatMode.ALL)
                }
                Player.REPEAT_MODE_ALL;
            }
            Player.REPEAT_MODE_ALL -> {
                serviceScope.launch {
                    settingsRepository.updateRepeatMode(RepeatMode.ONE)
                }
                Player.REPEAT_MODE_ONE;
            }
            Player.REPEAT_MODE_ONE -> {
                serviceScope.launch {
                    settingsRepository.updateRepeatMode(RepeatMode.OFF)
                }
                Player.REPEAT_MODE_OFF
            };
            else -> Player.REPEAT_MODE_OFF;
        };
        exoPlayer.repeatMode = next;
    }

    private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = bitmap.width.coerceAtMost(bitmap.height);
        val output = createBitmap(size, size);
        val canvas = Canvas(output);
        val paint = Paint(Paint.ANTI_ALIAS_FLAG);
        val rect = Rect(0, 0, size, size);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    override fun onDestroy() {
        try {
            serviceScope.cancel();
            mediaSession?.run {
                exoPlayer.release();
                release();
                mediaSession = null;
            }
        } catch (e: Exception) {
        }
        super.onDestroy();
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MUSIC_NOTIFICATION_CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for music playback controls";
                setSound(null, null);
                enableLights(false);
                enableVibration(false);
            };
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
            notificationManager.createNotificationChannel(channel);
        }
    }

    private fun formatDuration(ms: Long): String {
        if (ms < 0) return "00:00";
        val seconds = (ms / 1000) % 60;
        val minutes = (ms / 1000) / 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Update widget UI without having a compile-time dependency on the app module.
     * Use resource name lookups (resources.getIdentifier) and target the widget via
     * ComponentName(packageName, WIDGET_PROVIDER_CLASS).
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateWidget() {
        try {
            val providerComponent = ComponentName(packageName, WIDGET_PROVIDER_CLASS);
            val appWidgetManager = AppWidgetManager.getInstance(this);
            val ids = appWidgetManager.getAppWidgetIds(providerComponent);
            if (ids.isEmpty()) return;

            val layoutId = resources.getIdentifier("widget_player", "layout", packageName);
            if (layoutId == 0) return;

            val views = RemoteViews(packageName, layoutId);
            val isPlaying = exoPlayer.isPlaying;
            val current = exoPlayer.currentMediaItem;
            val metadata: MediaMetadata? = current?.mediaMetadata;
            val title = metadata?.title?.toString() ?: if (current != null) getStringSafe("app_name") else "Click Play";
            val artist = metadata?.artist?.toString()?: if (current != null) getStringSafe("app_name") else "No song playing";
            val currentPositionMs = exoPlayer.currentPosition;
            val totalDurationMs = exoPlayer.duration;
            val durationText = "${formatDuration(currentPositionMs)} / ${formatDuration(totalDurationMs)}";

            val idTitle = resources.getIdentifier("widget_title", "id", packageName);
            val idArtist = resources.getIdentifier("widget_artist", "id", packageName);
            val idDuration = resources.getIdentifier("widget_duration", "id", packageName);
            val idPlayPause = resources.getIdentifier("widget_play_pause", "id", packageName);
            val idNext = resources.getIdentifier("widget_next", "id", packageName);
            val idPrev = resources.getIdentifier("widget_prev", "id", packageName);
            val idAlbumArt = resources.getIdentifier("widget_album_art", "id", packageName);
            val idRepeat = resources.getIdentifier("widget_repeat", "id", packageName);

            if (idTitle != 0) views.setTextViewText(idTitle, title);
            if (idArtist != 0) views.setTextViewText(idArtist, artist);
            if (idDuration != 0) views.setTextViewText(idDuration, durationText);

            val playDrawableResName = if (isPlaying) "ic_pause_24" else "ic_play_arrow_24";
            val playDrawableId = resources.getIdentifier(playDrawableResName, "drawable", packageName);
            if (idPlayPause != 0 && playDrawableId != 0) {
                views.setImageViewResource(idPlayPause, playDrawableId);
            }

            // Album art handling (circular + default)
            var artBitmap: Bitmap? = null;
            try {
                val artUri: Uri? = metadata?.artworkUri;
                if (artUri != null) {
                    contentResolver.openInputStream(artUri)?.use { input ->
                        artBitmap = BitmapFactory.decodeStream(input);
                    };
                }
            } catch (e: Exception) {}

            if (artBitmap == null) {
                val defaultId = resources.getIdentifier("ic_music_note_24", "drawable", packageName);
                if (defaultId != 0) {
                    artBitmap = BitmapFactory.decodeResource(resources, defaultId);
                } else {
                    // Fallback to launcher icon if music note not found
                    val fallbackId = resources.getIdentifier("ic_launcher_round", "mipmap", packageName);
                    if (fallbackId != 0) artBitmap = BitmapFactory.decodeResource(resources, fallbackId);
                }
            }
            if (artBitmap != null && idAlbumArt != 0) {
                val circularBitmap = createCircularBitmap(artBitmap!!);
                views.setImageViewBitmap(idAlbumArt, circularBitmap);
            }

            // Fixed icon tints (no palette)
            if (idPrev != 0) views.setInt(idPrev, "setColorFilter", Color.WHITE);
            if (idNext != 0) views.setInt(idNext, "setColorFilter", Color.WHITE);

            // Repeat icon
            val repeatMode = exoPlayer.repeatMode;
            val repeatDrawableResName = if (repeatMode == Player.REPEAT_MODE_ONE) "ic_repeat_one_24" else "ic_repeat_24";
            val repeatDrawableId = resources.getIdentifier(repeatDrawableResName, "drawable", packageName);
            if (idRepeat != 0 && repeatDrawableId != 0) {
                views.setImageViewResource(idRepeat, repeatDrawableId);
                // Tint based on mode: white for ALL/ONE, light gray for OFF
                val tintColor = when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> Color.LTGRAY;
                    else -> Color.WHITE;
                };
                views.setInt(idRepeat, "setColorFilter", tintColor);
            }

            // Pending intents
            if (idPlayPause != 0) views.setOnClickPendingIntent(idPlayPause, pendingBroadcastFor(ACTION_WIDGET_PLAY_PAUSE, ACTION_WIDGET_PLAY_PAUSE.hashCode()));
            if (idNext != 0) views.setOnClickPendingIntent(idNext, pendingBroadcastFor(ACTION_WIDGET_NEXT, ACTION_WIDGET_NEXT.hashCode()));
            if (idPrev != 0) views.setOnClickPendingIntent(idPrev, pendingBroadcastFor(ACTION_WIDGET_PREV, ACTION_WIDGET_PREV.hashCode()));
            if (idRepeat != 0) views.setOnClickPendingIntent(idRepeat, pendingBroadcastFor(ACTION_WIDGET_REPEAT, ACTION_WIDGET_REPEAT.hashCode()));

            // Responsiveness: Hide elements based on widget size (unchanged, as it matches your request)
            ids.forEach { appWidgetId ->
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId);
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                val viewsCopy = RemoteViews(views);  // Copy for per-widget changes
                if (minWidth < 250) {  // Small widget, hide prev and artist
                    if (idPrev != 0) viewsCopy.setViewVisibility(idPrev, android.view.View.GONE);
                    if (idArtist != 0) viewsCopy.setViewVisibility(idArtist, android.view.View.GONE);
                } else {
                    if (idPrev != 0) viewsCopy.setViewVisibility(idPrev, android.view.View.VISIBLE);
                    if (idArtist != 0) viewsCopy.setViewVisibility(idArtist, android.view.View.VISIBLE);
                }
                appWidgetManager.updateAppWidget(appWidgetId, viewsCopy);
            };
        } catch (e: Exception) {
            // ignore widget update errors so service won't crash
        }
    }

    /**
     * Build PendingIntents that target the provider broadcast receiver in the app package.
     */
    private fun pendingBroadcastFor(action: String, requestCode: Int): PendingIntent {
        val i = Intent().apply {
            component = ComponentName(packageName, WIDGET_PROVIDER_CLASS);
            this.action = action;
        };
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0;
        return PendingIntent.getBroadcast(this, requestCode, i, flags);
    }

    /**
     * Safe getString that uses runtime resource lookup for app_name in case R from this module differs.
     */
    private fun getStringSafe(name: String): String {
        val resId = resources.getIdentifier(name, "string", packageName);
        return if (resId != 0) resources.getString(resId) else "Music Player";
    }
}