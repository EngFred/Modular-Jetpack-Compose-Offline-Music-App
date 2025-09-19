package com.engfred.musicplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.R
import com.engfred.musicplayer.feature_player.data.service.PlaybackService
import androidx.core.net.toUri
import com.engfred.musicplayer.core.domain.repository.SettingsRepository
import com.engfred.musicplayer.core.domain.repository.LibraryRepository
import com.engfred.musicplayer.feature_player.data.service.WidgetDisplayInfo
import com.engfred.musicplayer.feature_player.data.service.WidgetUpdater
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.InstallIn
import dagger.hilt.EntryPoint
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.media3.common.Player
import com.engfred.musicplayer.core.domain.repository.RepeatMode
import com.engfred.musicplayer.core.domain.model.WidgetBackgroundMode

private const val TAG = "PlayerWidgetProvider"

@OptIn(UnstableApi::class)
class PlayerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.engfred.musicplayer.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.engfred.musicplayer.ACTION_WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.engfred.musicplayer.ACTION_WIDGET_PREV"
        const val ACTION_UPDATE_WIDGET = "com.engfred.musicplayer.ACTION_UPDATE_WIDGET"
        const val ACTION_WIDGET_REPEAT = "com.engfred.musicplayer.ACTION_WIDGET_REPEAT"
    }

    // Hilt entrypoint to fetch repositories from onReceive
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun settingsRepository(): SettingsRepository
        fun libraryRepository(): LibraryRepository
    }

    @OptIn(UnstableApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive action=${intent.action}")

        when (intent.action) {
            ACTION_WIDGET_PLAY_PAUSE,
            ACTION_WIDGET_NEXT,
            ACTION_WIDGET_PREV,
            ACTION_WIDGET_REPEAT -> {
                val svcIntent = Intent(context, PlaybackService::class.java).apply {
                    action = intent.action
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(svcIntent)
                    } else {
                        context.startService(svcIntent)
                    }
                } catch (e: IllegalStateException) {
                    // Foreground start not allowed (e.g. during boot). Fall back to safe broadcast:
                    Log.w(TAG, "startForegroundService refused (IllegalState) - falling back to broadcast action: ${e.message}")
                    // Broadcast to provider itself so it can update widgets without requiring the service
                    safeSendUpdateBroadcast(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start service for widget action: ${e.message}", e)
                }
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "Received BOOT_COMPLETED")
                // DO NOT force-start a foreground service at boot time.
                // Instead request a safe widget refresh. requestServiceRefresh will itself try service start
                // and fallback if not allowed.
                requestServiceRefresh(context)
            }

            ACTION_UPDATE_WIDGET -> {
                // existing ACTION_UPDATE_WIDGET code (unchanged)
                try {
                    val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
                    val settingsRepo = entry.settingsRepository()
                    val libRepo = entry.libraryRepository()

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val lastState = settingsRepo.getLastPlaybackState().first()
                            val appSettings = settingsRepo.getAppSettings().first()
                            val useThemeAware = appSettings.widgetBackgroundMode == WidgetBackgroundMode.THEME_AWARE

                            if (lastState.audioId != null) {
                                // try to resolve the audio and build idle display info
                                val audios = libRepo.getAllAudioFiles().first()
                                val audio = audios.find { it.id == lastState.audioId }
                                if (audio != null) {
                                    val display = WidgetDisplayInfo(
                                        title = audio.title,
                                        artist = audio.artist ?: "Unknown Artist",
                                        durationMs = audio.duration,
                                        positionMs = lastState.positionMs.coerceAtLeast(0L).coerceAtMost(audio.duration),
                                        artworkUri = audio.albumArtUri
                                    )

                                    val idleRepeat = when (appSettings.repeatMode) {
                                        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                                        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                                        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                                    }

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        WidgetUpdater.updateWidget(context, null, display, idleRepeat, useThemeAware)
                                    }
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        WidgetUpdater.updateWidget(context, null, null, Player.REPEAT_MODE_OFF, useThemeAware)
                                    }
                                }
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    WidgetUpdater.updateWidget(context, null, null, Player.REPEAT_MODE_OFF, useThemeAware)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Quick widget update failed in onReceive: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Widget quick-update entrypoint failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Try to request the service to refresh the widget. If startForegroundService is refused (boot, restricted),
     * fallback to sending ACTION_UPDATE_WIDGET broadcast which updates widgets without requiring the service.
     */
    private fun requestServiceRefresh(context: Context) {
        try {
            val refresh = Intent(context, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_REFRESH_WIDGET
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(refresh)
                } catch (ise: IllegalStateException) {
                    // Foreground start refused (common during boot). Fallback to safe approach.
                    Log.w(TAG, "startForegroundService refused in requestServiceRefresh: ${ise.message}")
                    safeSendUpdateBroadcast(context)
                }
            } else {
                try {
                    context.startService(refresh)
                } catch (e: Exception) {
                    Log.w(TAG, "startService failed in requestServiceRefresh: ${e.message}")
                    safeSendUpdateBroadcast(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "requestServiceRefresh failed: ${e.message}", e)
            safeSendUpdateBroadcast(context)
        }
    }

    /** Sends ACTION_UPDATE_WIDGET as a broadcast to this Provider — safe fallback when service cannot start. */
    private fun safeSendUpdateBroadcast(context: Context) {
        try {
            val b = Intent(context, PlayerWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            // Use sendBroadcast; AppWidgetProvider.onReceive will be called
            context.sendBroadcast(b)
        } catch (e: Exception) {
            Log.w(TAG, "safeSendUpdateBroadcast failed: ${e.message}")
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "onUpdate widgets count=${appWidgetIds.size}")

        // For initial updates, show the compact idle layout (no placeholders)
        // and attach per-widget pending intents. The WidgetUpdater will later replace with full info if available.
        appWidgetIds.forEach { appWidgetId ->
            try {
                val views = buildRemoteViews(context, appWidgetId)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget id=$appWidgetId: ${e.message}", e)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Let the service refresh with authoritative live state (if it is running).
            requestServiceRefresh(context)
        }
    }

    // Build RemoteViews per widget instance so we can create unique PendingIntents per widgetId.
    // Key: inflate compact idle layout (widget_player_idle) when available so we don't show placeholders.
    private fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
        val pkg = context.packageName
        val resources = context.resources

        // Prefer a compact idle layout if present, otherwise fall back to full layout.
        val idleLayoutId = resources.getIdentifier("widget_player_idle", "layout", pkg)
        val fullLayoutId = resources.getIdentifier("widget_player", "layout", pkg)
        val layoutToInflate = when {
            idleLayoutId != 0 -> idleLayoutId
            fullLayoutId != 0 -> fullLayoutId
            else -> R.layout.widget_player // fallback resource id (should exist)
        }

        val views = RemoteViews(pkg, layoutToInflate)

        fun pendingIntentFor(action: String, widgetId: Int): PendingIntent {
            val i = Intent(context, PlayerWidgetProvider::class.java).apply {
                this.action = action
                // Make the Intent unique across widget instances and updates
                data = "app://widget/$action/$widgetId".toUri()
                `package` = pkg
            }
            val requestCode = (action.hashCode() xor widgetId)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getBroadcast(context, requestCode, i, flags)
        }

        try {
            // These ids may or may not exist depending on chosen layout — RemoteViews ignores calls for missing ids.
            views.setOnClickPendingIntent(R.id.widget_play_pause, pendingIntentFor(ACTION_WIDGET_PLAY_PAUSE, appWidgetId))
            views.setOnClickPendingIntent(R.id.widget_next, pendingIntentFor(ACTION_WIDGET_NEXT, appWidgetId))
            views.setOnClickPendingIntent(R.id.widget_prev, pendingIntentFor(ACTION_WIDGET_PREV, appWidgetId))
            views.setOnClickPendingIntent(R.id.widget_repeat, pendingIntentFor(ACTION_WIDGET_REPEAT, appWidgetId))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set button pending intents: ${e.message}", e)
        }

        val openAppIntent = context.packageManager.getLaunchIntentForPackage(pkg)
        openAppIntent?.let {
            // make open intents unique too (to avoid reuse across widget instances)
            it.data = "app://widget/open/$appWidgetId".toUri()
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val openPending = PendingIntent.getActivity(context, appWidgetId, it, flags)
            // safe to call even when ids not present (ignored)
            views.setOnClickPendingIntent(R.id.widget_album_art, openPending)
            views.setOnClickPendingIntent(R.id.widget_title, openPending)
            views.setOnClickPendingIntent(R.id.widget_artist, openPending)
        }

        return views
    }
}
