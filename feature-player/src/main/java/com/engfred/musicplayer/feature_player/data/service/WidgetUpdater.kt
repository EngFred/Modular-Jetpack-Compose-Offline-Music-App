package com.engfred.musicplayer.feature_player.data.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.util.Locale

private const val TAG = "WidgetUpdater"

/**
 * Data class for holding display information when the player is idle but a last playback state exists.
 */
data class WidgetDisplayInfo(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val positionMs: Long,
    val artworkUri: Uri?
)

/**
 * Contains the large widget update logic extracted from PlaybackService.
 * Behavior is preserved; resource lookup names are the same.
 */
@SuppressLint("UseKtx")
@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.P)
object WidgetUpdater {

    fun updateWidget(
        context: Context,
        exoPlayer: ExoPlayer,
        idleDisplayInfo: WidgetDisplayInfo? = null,
        idleRepeatMode: Int = Player.REPEAT_MODE_OFF
    ) {
        try {
            val providerComponent = ComponentName(context.packageName, PlaybackService.WIDGET_PROVIDER_CLASS)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(providerComponent)
            if (ids.isEmpty()) return

            val resources = context.resources
            val fullLayoutId = resources.getIdentifier("widget_player", "layout", context.packageName)
            val idleLayoutId = resources.getIdentifier("widget_player_idle", "layout", context.packageName)
            if (fullLayoutId == 0) return

            val current = exoPlayer.currentMediaItem
            val isIdle = current == null
            val showFullInfo = !isIdle || (idleDisplayInfo != null)

            val idPlayPause = resources.getIdentifier("widget_play_pause", "id", context.packageName)
            val idNext = resources.getIdentifier("widget_next", "id", context.packageName)
            val idPrev = resources.getIdentifier("widget_prev", "id", context.packageName)
            val idRepeat = resources.getIdentifier("widget_repeat", "id", context.packageName)
            val idAlbumArt = resources.getIdentifier("widget_album_art", "id", context.packageName)
            val idTitle = resources.getIdentifier("widget_title", "id", context.packageName)
            val idArtist = resources.getIdentifier("widget_artist", "id", context.packageName)
            val idDuration = resources.getIdentifier("widget_duration", "id", context.packageName)

            if (!showFullInfo) {
                // Plain idle: only play button
                if (idleLayoutId == 0) {
                    Log.w(TAG, "widget_player_idle layout missing; falling back to full with hides")
                    val fallbackViews = RemoteViews(context.packageName, fullLayoutId)
                    buildFullWidgetWithHides(context, fallbackViews, idPlayPause)
                } else {
                    val baseViews = RemoteViews(context.packageName, idleLayoutId)
                    val playDrawableId = resources.getIdentifier("ic_play_arrow_24", "drawable", context.packageName)
                    if (idPlayPause != 0 && playDrawableId != 0) {
                        baseViews.setImageViewResource(idPlayPause, playDrawableId)
                    }
                    ids.forEach { appWidgetId ->
                        try {
                            val viewsCopy = RemoteViews(baseViews)
                            val playReq = PlaybackService.ACTION_WIDGET_PLAY_PAUSE.hashCode() xor appWidgetId
                            viewsCopy.setOnClickPendingIntent(idPlayPause, perWidgetBroadcast(context, PlaybackService.ACTION_WIDGET_PLAY_PAUSE, playReq, appWidgetId))
                            appWidgetManager.updateAppWidget(appWidgetId, viewsCopy)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error updating idle widget id=$appWidgetId: ${e.message}")
                        }
                    }
                }
                return
            }

            // Show full info: either current media or idle display info
            val metadata: MediaMetadata? = if (!isIdle) current?.mediaMetadata else null
            val title = if (!isIdle) (metadata?.title?.toString() ?: getStringSafe(context, "app_name"))
            else idleDisplayInfo!!.title
            val artist = if (!isIdle) (metadata?.artist?.toString() ?: getStringSafe(context, "app_name"))
            else idleDisplayInfo!!.artist
            val currentPositionMs = if (!isIdle) exoPlayer.currentPosition.coerceAtLeast(0L) else idleDisplayInfo!!.positionMs
            val totalDurationMs = if (!isIdle) exoPlayer.duration.takeIf { it > 0L } ?: 0L else idleDisplayInfo!!.durationMs
            val durationText = "${formatDuration(currentPositionMs)} / ${formatDuration(totalDurationMs)}"

            val baseViews = RemoteViews(context.packageName, fullLayoutId)

            if (idTitle != 0) baseViews.setTextViewText(idTitle, title)
            if (idArtist != 0) baseViews.setTextViewText(idArtist, artist)
            if (idDuration != 0) baseViews.setTextViewText(idDuration, durationText)

            val isPlaying = exoPlayer.isPlaying
            val playDrawableResName = if (isPlaying) "ic_pause_24" else "ic_play_arrow_24"
            val playDrawableId = resources.getIdentifier(playDrawableResName, "drawable", context.packageName)
            if (idPlayPause != 0 && playDrawableId != 0) {
                baseViews.setImageViewResource(idPlayPause, playDrawableId)
            }

            // load artwork
            val artworkUri: Uri? = if (!isIdle) metadata?.artworkUri else idleDisplayInfo!!.artworkUri
            var artBitmap = tryLoadArtwork(context, artworkUri)
            if (artBitmap == null) {
                val defaultId = resources.getIdentifier("ic_music_note_24", "drawable", context.packageName)
                if (defaultId != 0) {
                    artBitmap = BitmapFactory.decodeResource(resources, defaultId)
                } else {
                    val fallbackId = resources.getIdentifier("ic_launcher_round", "mipmap", context.packageName)
                    if (fallbackId != 0) artBitmap = BitmapFactory.decodeResource(resources, fallbackId)
                }
            }
            if (artBitmap != null && idAlbumArt != 0) {
                val circular = createCircularBitmap(artBitmap)
                baseViews.setImageViewBitmap(idAlbumArt, circular)
            }

            if (idPrev != 0) baseViews.setInt(idPrev, "setColorFilter", android.graphics.Color.WHITE)
            if (idNext != 0) baseViews.setInt(idNext, "setColorFilter", android.graphics.Color.WHITE)

            val repeatMode = if (!isIdle) exoPlayer.repeatMode else idleRepeatMode
            val repeatDrawableResName = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> "ic_repeat_one_24"
                Player.REPEAT_MODE_ALL -> "ic_repeat_on_24"
                else -> "ic_repeat_24"
            }
            var repeatDrawableId = resources.getIdentifier(repeatDrawableResName, "drawable", context.packageName)
            if (repeatDrawableId == 0 && repeatMode == Player.REPEAT_MODE_ALL) {
                repeatDrawableId = resources.getIdentifier("ic_repeat_24", "drawable", context.packageName)
            }
            if (idRepeat != 0 && repeatDrawableId != 0) {
                baseViews.setImageViewResource(idRepeat, repeatDrawableId)
                val tintColor = when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> android.graphics.Color.LTGRAY
                    else -> android.graphics.Color.WHITE
                }
                baseViews.setInt(idRepeat, "setColorFilter", tintColor)
            }

            ids.forEach { appWidgetId ->
                try {
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    val viewsCopy = RemoteViews(baseViews)

                    val playReq = PlaybackService.ACTION_WIDGET_PLAY_PAUSE.hashCode() xor appWidgetId
                    val nextReq = PlaybackService.ACTION_WIDGET_NEXT.hashCode() xor appWidgetId
                    val prevReq = PlaybackService.ACTION_WIDGET_PREV.hashCode() xor appWidgetId
                    val repeatReq = PlaybackService.ACTION_WIDGET_REPEAT.hashCode() xor appWidgetId

                    if (idPlayPause != 0) viewsCopy.setOnClickPendingIntent(idPlayPause, perWidgetBroadcast(context, PlaybackService.ACTION_WIDGET_PLAY_PAUSE, playReq, appWidgetId))
                    if (idNext != 0) viewsCopy.setOnClickPendingIntent(idNext, perWidgetBroadcast(context, PlaybackService.ACTION_WIDGET_NEXT, nextReq, appWidgetId))
                    if (idPrev != 0) viewsCopy.setOnClickPendingIntent(idPrev, perWidgetBroadcast(context, PlaybackService.ACTION_WIDGET_PREV, prevReq, appWidgetId))
                    if (idRepeat != 0) viewsCopy.setOnClickPendingIntent(idRepeat, perWidgetBroadcast(context, PlaybackService.ACTION_WIDGET_REPEAT, repeatReq, appWidgetId))

                    if (idAlbumArt != 0) {
                        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        openAppIntent?.data = "app://widget/open/$appWidgetId".toUri()
                        val openFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        val openPending = PendingIntent.getActivity(context, appWidgetId, openAppIntent, openFlags)
                        viewsCopy.setOnClickPendingIntent(idAlbumArt, openPending)
                    }
                    if (idTitle != 0) {
                        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        openAppIntent?.data = "app://widget/open/$appWidgetId/title".toUri()
                        val openFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        val openPending = PendingIntent.getActivity(context, appWidgetId + 1, openAppIntent, openFlags)
                        viewsCopy.setOnClickPendingIntent(idTitle, openPending)
                    }
                    if (idArtist != 0) {
                        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        openAppIntent?.data = "app://widget/open/$appWidgetId/artist".toUri()
                        val openFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        val openPending = PendingIntent.getActivity(context, appWidgetId + 2, openAppIntent, openFlags)
                        viewsCopy.setOnClickPendingIntent(idArtist, openPending)
                    }

                    if (minWidth < 250) {
                        if (idPrev != 0) viewsCopy.setViewVisibility(idPrev, View.GONE)
                        if (idArtist != 0) viewsCopy.setViewVisibility(idArtist, View.GONE)
                    } else {
                        if (idPrev != 0) viewsCopy.setViewVisibility(idPrev, View.VISIBLE)
                        if (idArtist != 0) viewsCopy.setViewVisibility(idArtist, View.VISIBLE)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, viewsCopy)
                } catch (_: Exception) {
                    // ignore
                }
            }
        } catch (e: Exception) {
            // ignore widget update errors so service won't crash
            Log.w(TAG, "updateWidget throwable: ${e.message}")
        }
    }

    private fun tryLoadArtwork(context: Context, uri: Uri?): Bitmap? {
        if (uri == null) return null
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("UseKtx")
    private fun buildFullWidgetWithHides(context: Context, baseViews: RemoteViews, idPlayPause: Int) {
        val resources = context.resources
        val idAlbumArt = resources.getIdentifier("widget_album_art", "id", context.packageName)
        val idTitle = resources.getIdentifier("widget_title", "id", context.packageName)
        val idArtist = resources.getIdentifier("widget_artist", "id", context.packageName)
        val idDuration = resources.getIdentifier("widget_duration", "id", context.packageName)
        val idNext = resources.getIdentifier("widget_next", "id", context.packageName)
        val idPrev = resources.getIdentifier("widget_prev", "id", context.packageName)
        val idRepeat = resources.getIdentifier("widget_repeat", "id", context.packageName)

        if (idAlbumArt != 0) baseViews.setViewVisibility(idAlbumArt, View.GONE)
        if (idTitle != 0) baseViews.setViewVisibility(idTitle, View.GONE)
        if (idArtist != 0) baseViews.setViewVisibility(idArtist, View.GONE)
        if (idDuration != 0) baseViews.setViewVisibility(idDuration, View.GONE)
        if (idNext != 0) baseViews.setViewVisibility(idNext, View.GONE)
        if (idPrev != 0) baseViews.setViewVisibility(idPrev, View.GONE)
        if (idRepeat != 0) baseViews.setViewVisibility(idRepeat, View.GONE)

        val playDrawableId = resources.getIdentifier("ic_play_arrow_24", "drawable", context.packageName)
        if (idPlayPause != 0 && playDrawableId != 0) {
            baseViews.setImageViewResource(idPlayPause, playDrawableId)
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context.packageName, PlaybackService.WIDGET_PROVIDER_CLASS))
        ids.forEach { appWidgetId ->
            try {
                val viewsCopy = RemoteViews(baseViews)
                val playReq = PlaybackService.ACTION_WIDGET_PLAY_PAUSE.hashCode() xor appWidgetId
                viewsCopy.setOnClickPendingIntent(idPlayPause, perWidgetBroadcast(context, PlaybackService.ACTION_WIDGET_PLAY_PAUSE, playReq, appWidgetId))
                appWidgetManager.updateAppWidget(appWidgetId, viewsCopy)
            } catch (e: Exception) {
                Log.w(TAG, "Error in fallback idle update for id=$appWidgetId: ${e.message}")
            }
        }
    }

    private fun perWidgetBroadcast(context: Context, action: String, requestCode: Int, widgetId: Int): PendingIntent {
        val i = Intent().apply {
            component = ComponentName(context.packageName, PlaybackService.WIDGET_PROVIDER_CLASS)
            this.action = action
            data = "app://widget/$action/$widgetId".toUri()
            `package` = context.packageName
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, i, flags)
    }

    private fun getStringSafe(context: Context, name: String): String {
        val resId = context.resources.getIdentifier(name, "string", context.packageName)
        return if (resId != 0) context.resources.getString(resId) else "Music Player"
    }

    private fun formatDuration(ms: Long): String {
        if (ms < 0) return "00:00"
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 1000) / 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    /**
     * Small bitmap helpers used by the service/widget code.
     */
    private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = bitmap.width.coerceAtMost(bitmap.height)
        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }
}