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

private const val TAG = "PlayerWidgetProvider"

class PlayerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.engfred.musicplayer.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.engfred.musicplayer.ACTION_WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.engfred.musicplayer.ACTION_WIDGET_PREV"
        const val ACTION_UPDATE_WIDGET = "com.engfred.musicplayer.ACTION_UPDATE_WIDGET"
        const val ACTION_WIDGET_REPEAT = "com.engfred.musicplayer.ACTION_WIDGET_REPEAT"
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
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start service for widget action: ${e.message}", e)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    requestServiceRefresh(context)
                }
            }
            ACTION_UPDATE_WIDGET -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    requestServiceRefresh(context)
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "onUpdate widgets count=${appWidgetIds.size}")

        // Update each widget individually with per-widget unique pending intents
        appWidgetIds.forEach { appWidgetId ->
            try {
                val views = buildRemoteViews(context, appWidgetId)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget id=$appWidgetId: ${e.message}", e)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requestServiceRefresh(context)
        }
    }

    @OptIn(UnstableApi::class)
    private fun requestServiceRefresh(context: Context) {
        try {
            val refresh = Intent(context, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_REFRESH_WIDGET
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(refresh)
            } else {
                context.startService(refresh)
            }
        } catch (e: Exception) {
            Log.e(TAG, "requestServiceRefresh failed: ${e.message}", e)
        }
    }

    // Build RemoteViews per widget instance so we can create unique PendingIntents per widgetId
    private fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
        val pkg = context.packageName
        val views = RemoteViews(pkg, R.layout.widget_player)

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
            views.setOnClickPendingIntent(R.id.widget_album_art, openPending)
            views.setOnClickPendingIntent(R.id.widget_title, openPending)
            views.setOnClickPendingIntent(R.id.widget_artist, openPending)
        }

        return views
    }
}
