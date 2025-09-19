package com.engfred.musicplayer.feature_player.data.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "WidgetUpdater"

/**
 * WidgetDisplayInfo - small data holder for idle display state.
 */
data class WidgetDisplayInfo(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val positionMs: Long,
    val artworkUri: Uri?
)

@SuppressLint("UseKtx")
@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.P)
object WidgetUpdater {

    // Debounce window for coalescing rapid updates (milliseconds).
    // private const val DEBOUNCE_MS: Long = 200L

    // Main thread handler for scheduling updates.
    private val handler = Handler(Looper.getMainLooper())

    // Keep last scheduled runnable (so we can cancel when new update arrives).
    // Debounce disabled: comment out lastRunnableRef that was used for cancellation.
    // private val lastRunnableRef: AtomicReference<Runnable?> = AtomicReference(null)

    // Keep most recent request params
    private data class Req(
        val contextAppPackage: String,
        val contextAppName: String, // not strictly required - for debugging if needed
        val exoPlayer: Player?, // nullable so callers can update without a running player
        val idleDisplayInfo: WidgetDisplayInfo?,
        val idleRepeatMode: Int,
        val useThemeAware: Boolean,
        val forceImmediate: Boolean
    )

    private val lastReqRef = AtomicReference<Req?>(null)

    // Keep last known positive duration so we don't flash 00:00 while the player warms up.
    // This is a simple global fallback. If you prefer per-track durations, replace with a Map<mediaId, Long>.
    private var lastKnownDurationMs: Long = 0L
    private const val UNKNOWN_DURATION_TEXT = "00:00"

    /**
     * Public API.
     *
     * Debounce has been removed — updates are posted immediately to the main thread.
     * The original debounce code (DEBOUNCE_MS and cancellation) is retained as comments.
     */
    fun updateWidget(
        context: Context,
        exoPlayer: Player?, // nullable so callers can update without a running player
        idleDisplayInfo: WidgetDisplayInfo? = null,
        idleRepeatMode: Int = Player.REPEAT_MODE_OFF,
        useThemeAware: Boolean = false, // whether to adapt to system theme
        forceImmediate: Boolean = false
    ) {
        try {
            // Build a compact request and store as last.
            val req = Req(
                contextAppPackage = context.applicationContext.packageName,
                contextAppName = context.applicationContext.javaClass.simpleName,
                exoPlayer = exoPlayer,
                idleDisplayInfo = idleDisplayInfo,
                idleRepeatMode = idleRepeatMode,
                useThemeAware = useThemeAware,
                forceImmediate = forceImmediate
            )

            // store latest request (kept for debugging / fallback)
            lastReqRef.set(req)

            val runnable = Runnable {
                try {
                    // Previously: val last = lastReqRef.getAndSet(null)
                    // Now: execute immediately using the captured req (or fallback to lastReqRef)
                    val last = lastReqRef.getAndSet(null) ?: req
                    performUpdate(context.applicationContext, last)
                } catch (t: Throwable) {
                    Log.w(TAG, "Scheduled widget update failed: ${t.message}")
                }
            }

            // Debounce cancellation logic removed (commented for reference)
            // val previous = lastRunnableRef.getAndSet(runnable)
            // previous?.let { handler.removeCallbacks(it) }

            // Always post immediately on main thread (debounce removed).
            handler.post(runnable)

            // If you want to preserve "forceImmediate" semantics in future, you can keep it
            // to decide whether to post immediately or after a delay. For now, everything runs immediately.
        } catch (e: Exception) {
            Log.w(TAG, "updateWidget enqueue failed: ${e.message}")
        }
    }

    /**
     * Actual heavy-lifting update executed on main thread.
     * Uses the request snapshot captured earlier.
     */
    private fun performUpdate(appContext: Context, req: Req) {
        try {
            val context = appContext
            val providerComponent = ComponentName(
                context.packageName,
                PlaybackService.WIDGET_PROVIDER_CLASS
            )
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(providerComponent)
            if (ids.isEmpty()) return

            val resources = context.resources
            val fullLayoutId = resources.getIdentifier("widget_player", "layout", context.packageName)
            val idleLayoutId = resources.getIdentifier("widget_player_idle", "layout", context.packageName)
            if (fullLayoutId == 0) return

            val current = req.exoPlayer?.currentMediaItem
            val isIdle = (req.exoPlayer == null) || (current == null)
            // Show full iff there is a media item or we have idleDisplayInfo to render
            val showFullInfo = !isIdle || (req.idleDisplayInfo != null)

            val idRoot = resources.getIdentifier("widget_root", "id", context.packageName)
            val idIdleRoot = resources.getIdentifier("widget_root_idle", "id", context.packageName)
            val idPlayPause = resources.getIdentifier("widget_play_pause", "id", context.packageName)
            val idNext = resources.getIdentifier("widget_next", "id", context.packageName)
            val idPrev = resources.getIdentifier("widget_prev", "id", context.packageName)
            val idRepeat = resources.getIdentifier("widget_repeat", "id", context.packageName)
            val idAlbumArt = resources.getIdentifier("widget_album_art", "id", context.packageName)
            val idTitle = resources.getIdentifier("widget_title", "id", context.packageName)
            val idArtist = resources.getIdentifier("widget_artist", "id", context.packageName)
            val idDuration = resources.getIdentifier("widget_duration", "id", context.packageName)

            // System dark flag
            val isSystemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            val bgResToUse: Int = if (req.useThemeAware) {
                resources.getIdentifier(
                    if (isSystemDark) "widget_background_dark" else "widget_background_light",
                    "drawable",
                    context.packageName
                ).takeIf { it != 0 } ?: resources.getIdentifier("widget_background", "drawable", context.packageName)
            } else {
                resources.getIdentifier("widget_background", "drawable", context.packageName)
            }

            // icon/text tints for theme-aware (but ALWAYS keep play/pause black)
            val defaultIconTint = if (req.useThemeAware) {
                if (isSystemDark) Color.WHITE else Color.BLACK
            } else Color.WHITE

            val playPauseTint = Color.BLACK // ALWAYS black per request

            val textColorPrimary = if (req.useThemeAware) {
                if (isSystemDark) Color.WHITE else Color.BLACK
            } else Color.WHITE

            val textColorSecondary = if (req.useThemeAware) {
                if (isSystemDark) Color.LTGRAY else Color.DKGRAY
            } else 0xFFCCCCCC.toInt()

            if (!showFullInfo) {
                // Plain idle: show compact idle layout when available so we don't display placeholders
                if (idleLayoutId == 0) {
                    // fallback: use full and hide elements
                    val fallbackViews = RemoteViews(context.packageName, fullLayoutId)
                    if (idRoot != 0 && bgResToUse != 0) fallbackViews.setInt(idRoot, "setBackgroundResource", bgResToUse)
                    if (idPlayPause != 0) fallbackViews.setInt(idPlayPause, "setColorFilter", playPauseTint)
                    buildFullWidgetWithHides(context, fallbackViews, idPlayPause)
                } else {
                    val baseViews = RemoteViews(context.packageName, idleLayoutId)
                    if (idIdleRoot != 0 && bgResToUse != 0) baseViews.setInt(idIdleRoot, "setBackgroundResource", bgResToUse)

                    val playDrawableId = resources.getIdentifier("ic_play_arrow_24", "drawable", context.packageName)
                    if (idPlayPause != 0 && playDrawableId != 0) {
                        baseViews.setImageViewResource(idPlayPause, playDrawableId)
                        baseViews.setInt(idPlayPause, "setColorFilter", playPauseTint)
                    }

                    ids.forEach { appWidgetId ->
                        try {
                            val viewsCopy = RemoteViews(baseViews)
                            val playReq = PlaybackService.ACTION_WIDGET_PLAY_PAUSE.hashCode() xor appWidgetId
                            viewsCopy.setOnClickPendingIntent(idPlayPause, perWidgetBroadcast(context, com.engfred.musicplayer.feature_player.data.service.PlaybackService.ACTION_WIDGET_PLAY_PAUSE, playReq, appWidgetId))
                            appWidgetManager.updateAppWidget(appWidgetId, viewsCopy)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error updating idle widget id=$appWidgetId: ${e.message}")
                        }
                    }
                }
                return
            }

            // Show full info
            val metadata: androidx.media3.common.MediaMetadata? = if (!isIdle) current?.mediaMetadata else null
            val title = if (!isIdle) (metadata?.title?.toString() ?: getStringSafe(context, "app_name")) else req.idleDisplayInfo!!.title
            val artist = if (!isIdle) (metadata?.artist?.toString() ?: getStringSafe(context, "app_name")) else req.idleDisplayInfo!!.artist

            val currentPositionMs = if (!isIdle) req.exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L else req.idleDisplayInfo!!.positionMs

            // Player sometimes reports duration as 0 or an "unset" value when playback just started.
            // Only accept a duration > 0 as valid and update lastKnownDurationMs. Otherwise reuse lastKnownDurationMs.
            val candidateDurationMs = if (!isIdle) (req.exoPlayer?.duration ?: 0L) else req.idleDisplayInfo!!.durationMs

            val totalDurationMs = when {
                // Prefer a valid positive duration from the player / idle info
                candidateDurationMs > 0L -> {
                    lastKnownDurationMs = candidateDurationMs
                    candidateDurationMs
                }
                // Fallback to the last known positive duration (if any)
                lastKnownDurationMs > 0L -> lastKnownDurationMs
                // No known duration yet
                else -> 0L
            }

            // Build duration text: when we don't have a valid total duration show 00:00
            val totalDurationText = if (totalDurationMs > 0L) formatDuration(totalDurationMs) else UNKNOWN_DURATION_TEXT
            val durationText = "${formatDuration(currentPositionMs)} / $totalDurationText"

            val baseViews = RemoteViews(context.packageName, fullLayoutId)

            if (idRoot != 0 && bgResToUse != 0) baseViews.setInt(idRoot, "setBackgroundResource", bgResToUse)

            if (idTitle != 0) {
                baseViews.setTextViewText(idTitle, title)
                if (req.useThemeAware) baseViews.setTextColor(idTitle, textColorPrimary)
            }
            if (idArtist != 0) {
                baseViews.setTextViewText(idArtist, artist)
                if (req.useThemeAware) baseViews.setTextColor(idArtist, textColorSecondary)
            }
            if (idDuration != 0) {
                baseViews.setTextViewText(idDuration, durationText)
                if (req.useThemeAware) baseViews.setTextColor(idDuration, textColorSecondary)
            }

            val isPlaying = req.exoPlayer?.isPlaying ?: false
            val playDrawableResName = if (isPlaying) "ic_pause_24" else "ic_play_arrow_24"
            val playDrawableId = resources.getIdentifier(playDrawableResName, "drawable", context.packageName)
            if (idPlayPause != 0 && playDrawableId != 0) {
                baseViews.setImageViewResource(idPlayPause, playDrawableId)
                baseViews.setInt(idPlayPause, "setColorFilter", playPauseTint) // always black
            }

            // load artwork
            val artworkUri: Uri? = if (!isIdle) metadata?.artworkUri else req.idleDisplayInfo!!.artworkUri
            var artBitmap = tryLoadArtwork(context, artworkUri)
            var isUsingDefaultIcon = false
            if (artBitmap == null) {
                val defaultId = resources.getIdentifier("ic_music_note_24", "drawable", context.packageName)
                if (defaultId != 0) {
                    val tintColor = if (req.useThemeAware) {
                        if (isSystemDark) Color.WHITE else Color.BLACK
                    } else Color.WHITE
                    artBitmap = getTintedBitmap(context, defaultId, tintColor)
                    isUsingDefaultIcon = true
                }
            }
            if (artBitmap != null && idAlbumArt != 0) {
                val circular = createCircularBitmap(artBitmap)
                baseViews.setImageViewBitmap(idAlbumArt, circular)
            }

            // tint next/prev icons
            if (idPrev != 0) baseViews.setInt(idPrev, "setColorFilter", defaultIconTint)
            if (idNext != 0) baseViews.setInt(idNext, "setColorFilter", defaultIconTint)

            val repeatMode = if (!isIdle) req.exoPlayer?.repeatMode ?: Player.REPEAT_MODE_OFF else req.idleRepeatMode
            val repeatDrawableResName = if (repeatMode == Player.REPEAT_MODE_ONE) "repeat_once" else "repeat"
            var repeatDrawableId = resources.getIdentifier(repeatDrawableResName, "drawable", context.packageName)
            if (repeatDrawableId == 0) {
                repeatDrawableId = resources.getIdentifier(
                    when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> "ic_repeat_one_24"
                        Player.REPEAT_MODE_ALL -> "ic_repeat_on_24"
                        else -> "ic_repeat_24"
                    }, "drawable", context.packageName
                )
            }
            if (idRepeat != 0 && repeatDrawableId != 0) {
                baseViews.setImageViewResource(idRepeat, repeatDrawableId)
                val tintColor = when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> Color.GRAY
                    else -> defaultIconTint
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
                } catch (_: Throwable) {
                    // ignore per-widget failures (do not crash service)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "performUpdate throwable: ${e.message}")
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
            baseViews.setInt(idPlayPause, "setColorFilter", Color.BLACK) // always black
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

    private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = bitmap.width.coerceAtMost(bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    private fun getTintedBitmap(context: Context, drawableId: Int, tintColor: Int): Bitmap? {
        val drawable: Drawable? = ContextCompat.getDrawable(context, drawableId)
        drawable?.let {
            val wrapped = DrawableCompat.wrap(it.mutate())
            DrawableCompat.setTint(wrapped, tintColor)
            wrapped.setBounds(0, 0, wrapped.intrinsicWidth, wrapped.intrinsicHeight)
            val bitmap = Bitmap.createBitmap(wrapped.intrinsicWidth, wrapped.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            wrapped.draw(canvas)
            return bitmap
        }
        return null
    }
}