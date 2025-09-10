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
import androidx.annotation.RequiresApi
import androidx.media3.common.util.UnstableApi
import com.engfred.musicplayer.R
import com.engfred.musicplayer.feature_player.data.service.PlaybackService

private const val TAG = "PlayerWidgetProvider"

@RequiresApi(Build.VERSION_CODES.P)
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
                // request immediate refresh once service is available
                requestServiceRefresh(context)
            }
            ACTION_UPDATE_WIDGET -> {
                requestServiceRefresh(context)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d(TAG, "onUpdate widgets count=${appWidgetIds.size}")

        // Show a default placeholder view immediately
        appWidgetIds.forEach { appWidgetId ->
            try {
                val views = buildRemoteViews(context)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget id=$appWidgetId: ${e.message}", e)
            }
        }

        // Ask the PlaybackService to push the real current state (this ensures play/pause icon matches)
        requestServiceRefresh(context)
    }

    @RequiresApi(Build.VERSION_CODES.P)
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

    private fun buildRemoteViews(context: Context): RemoteViews {
        val pkg = context.packageName
        val views = RemoteViews(pkg, R.layout.widget_player)

        fun pendingIntentFor(action: String): PendingIntent {
            val i = Intent(context, PlayerWidgetProvider::class.java).apply { this.action = action }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getBroadcast(context, action.hashCode(), i, flags)
        }

        try {
            views.setOnClickPendingIntent(R.id.widget_play_pause, pendingIntentFor(ACTION_WIDGET_PLAY_PAUSE))
            views.setOnClickPendingIntent(R.id.widget_next, pendingIntentFor(ACTION_WIDGET_NEXT))
            views.setOnClickPendingIntent(R.id.widget_prev, pendingIntentFor(ACTION_WIDGET_PREV))
            views.setOnClickPendingIntent(R.id.widget_repeat, pendingIntentFor(ACTION_WIDGET_REPEAT))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set button pending intents: ${e.message}", e)
        }

        val openAppIntent = context.packageManager.getLaunchIntentForPackage(pkg)
        openAppIntent?.let {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
            val openPending = PendingIntent.getActivity(context, 0, it, flags)
            views.setOnClickPendingIntent(R.id.widget_album_art, openPending)
            views.setOnClickPendingIntent(R.id.widget_title, openPending)
            views.setOnClickPendingIntent(R.id.widget_artist, openPending)
        }

        return views
    }
}