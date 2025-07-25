package com.engfred.musicplayer.core.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import kotlin.system.exitProcess

/**
 * Restarts the application by relaunching its main launcher activity.
 *
 * @param context The application context. Using application context is generally
 * safer to avoid memory leaks if the activity context is transient.
 * @param message An optional message to display to the user before restarting.
 * Set to null or empty string if no message is needed.
 */
fun restartApp(context: Context, message: String? = null) {
    // 1. Display a message to the user (optional)
    message?.let {
        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
    }

    // 2. Get the Intent for the main launcher activity
    // This finds the default activity that starts when the app is launched from the home screen.
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)

    // Ensure the intent is not null (should not happen for a valid package)
    if (intent == null) {
        // Log an error or handle the case where the launcher intent cannot be found
        // For production, you might want a more robust error reporting mechanism.
        println("Error: Could not find launcher intent for package ${context.packageName}")
        return
    }

    // 3. Set flags to clear the activity stack
    // FLAG_ACTIVITY_CLEAR_TASK: Clears any existing task that would be associated with the activity.
    // FLAG_ACTIVITY_NEW_TASK: Starts the activity in a new task.
    // Together, these ensure that the new instance of the activity is the only one in the task.
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)

    // 4. Create a PendingIntent to schedule the restart
    // A PendingIntent allows another application (in this case, AlarmManager) to execute
    // a piece of code (our Intent) on your behalf.
    val pendingIntent = PendingIntent.getActivity(
        context,
        0, // Request code
        intent,
        // FLAG_CANCEL_CURRENT: If a previous PendingIntent exists, cancel it and create a new one.
        // FLAG_IMMUTABLE: Required for Android S (API 31) and above. Makes the PendingIntent immutable.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }
    )

    // 5. Schedule the restart using AlarmManager
    // AlarmManager can trigger an operation at a specified time.
    // Here, we schedule it to happen almost immediately.
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent) // 100ms delay

    // 6. Terminate the current process
    // This is done AFTER scheduling the new intent. The OS will then launch the new intent.
    android.os.Process.killProcess(android.os.Process.myPid())
    exitProcess(0) // Ensure the process is truly exited
}