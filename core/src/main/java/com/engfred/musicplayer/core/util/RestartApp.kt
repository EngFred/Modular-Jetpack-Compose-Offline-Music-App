package com.engfred.musicplayer.core.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlin.system.exitProcess

/**
 * Restarts the application by scheduling a restart of the app's launcher activity
 * using AlarmManager, then terminating the current process.
 *
 * - Pass applicationContext (context.applicationContext) to avoid leaking an Activity.
 * - onBeforeRestart is invoked synchronously before scheduling/killing so you can stop playback,
 *   release resources, stop services or save state.
 * - Some OEMs or battery optimizers may prevent automatic restarts. This method works on
 *   most stock Android devices but cannot override OEM-level autostart policies.
 *
 * @param context Application context (use context.applicationContext).
 * @param delayMs How long to wait (ms) before the restart Intent fires. Default 200 ms.
 * @param toastMessage Optional message shown to the user before restart (Toast on main thread).
 * @param onBeforeRestart Optional lambda to release resources (stop media, stop services) before restart.
 */
fun restartApp(
    context: Context,
    delayMs: Long = 200L,
    toastMessage: String? = null,
    onBeforeRestart: (() -> Unit)? = null
) {
    val appContext = context.applicationContext ?: context

    // Show optional toast on main thread
    toastMessage?.let { msg ->
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
            } catch (ignored: Exception) { /* ignore toast failures */ }
        }
    }

    try {
        // Give caller a chance to stop playback / release heavy resources gracefully
        try {
            onBeforeRestart?.invoke()
        } catch (e: Exception) {
            // We log/ignore — we still proceed to restart
            e.printStackTrace()
        }

        val pm = appContext.packageManager ?: run {
            // cannot proceed without PackageManager
            return
        }

        // Try to obtain the launch intent for the package (normal case)
        var launchIntent = pm.getLaunchIntentForPackage(appContext.packageName)

        // Fallback: build a MAIN/LAUNCHER Intent that targets this package
        if (launchIntent == null) {
            launchIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(appContext.packageName)
            }
        }

        // Resolve the activity so we can construct a ComponentName (more robust)
        val resolved = pm.resolveActivity(launchIntent, 0)
        val componentName: ComponentName? = resolved?.activityInfo?.let { info ->
            ComponentName(info.packageName, info.name)
        }

        // Build restart Intent. If we have a ComponentName use makeRestartActivityTask to create a fresh task stack.
        val restartIntent: Intent = if (componentName != null) {
            Intent.makeRestartActivityTask(componentName)
        } else {
            // Last resort: reuse the launch intent but enforce new/clear task flags
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            launchIntent
        }

        // PendingIntent flags:
        var pendingFlags = PendingIntent.FLAG_CANCEL_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // On modern Androids we must specify mutability. Use IMMUTABLE as we don't need to mutate it.
            pendingFlags = pendingFlags or PendingIntent.FLAG_IMMUTABLE
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0xCAFE_BABE.toInt(), // request code (unique-ish)
            restartIntent,
            pendingFlags
        )

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            val triggerAt = System.currentTimeMillis() + delayMs
            // setExactAndAllowWhileIdle is preferred for exactness across doze states
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            // last resort: start activity directly (may fail since process still alive)
            try {
                restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                appContext.startActivity(restartIntent)
            } catch (ignored: Exception) {
            }
        }
    } catch (e: Exception) {
        // If scheduling restart failed, we still attempt to kill process to avoid inconsistent state.
        e.printStackTrace()
    } finally {
        // Kill the process — do this after scheduling the alarm
        try {
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (ignored: Exception) {
        }
        // Ensure the VM exits
        try {
            exitProcess(0)
        } catch (ignored: Exception) {
        }
    }
}
