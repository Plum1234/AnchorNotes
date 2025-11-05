package com.example.anchornotes.context;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.anchornotes.MainActivity;
import com.example.anchornotes.R;
import com.example.anchornotes.data.db.NoteEntity;

/**
 * Helper class for creating and showing reminder notifications.
 */
public class NotificationHelper {
    private static final String CHANNEL_ID = "ANCHOR_NOTES_REMINDERS";
    private static final String CHANNEL_NAME = "Anchor Notes Reminders";

    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for note reminders");
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Builds a notification for a reminder.
     * @param note The note to remind about
     * @return A NotificationCompat.Builder ready to be built and shown
     */
    @NonNull
    public NotificationCompat.Builder buildReminder(@NonNull NoteEntity note) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("noteId", note.id);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) note.id,
                intent,
                flags
        );

        String title = note.title != null && !note.title.isEmpty() ? note.title : "Reminder";
        String text = note.bodyHtml != null ? android.text.Html.fromHtml(note.bodyHtml, 0).toString() : "";
        if (text.length() > 100) {
            text = text.substring(0, 100) + "...";
        }

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
    }

    /**
     * Shows a reminder notification.
     * @param note The note to remind about
     */
    public void showReminder(@NonNull NoteEntity note) {
        NotificationCompat.Builder builder = buildReminder(note);
        notificationManager.notify((int) note.id, builder.build());
    }
}
