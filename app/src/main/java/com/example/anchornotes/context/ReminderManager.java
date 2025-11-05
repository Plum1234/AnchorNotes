package com.example.anchornotes.context;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import com.example.anchornotes.receiver.AlarmReceiver;

/**
 * Manages time-based reminders using AlarmManager.
 */
public class ReminderManager {
    private static final int NOTE_ALARM_BASE = 10000;

    private final Context context;
    private final AlarmManager alarmManager;

    public ReminderManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Schedules an exact alarm for a note reminder.
     * @param noteId The note ID
     * @param atMillis The time (in milliseconds since epoch) when the alarm should fire
     */
    public void scheduleExact(long noteId, long atMillis) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_NOTE_ID, noteId);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                NOTE_ALARM_BASE + (int) noteId,
                intent,
                flags
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent);
        }
    }

    /**
     * Cancels a scheduled alarm for a note.
     * @param noteId The note ID
     */
    public void cancel(long noteId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(AlarmReceiver.EXTRA_NOTE_ID, noteId);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                NOTE_ALARM_BASE + (int) noteId,
                intent,
                flags
        );

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }
}
