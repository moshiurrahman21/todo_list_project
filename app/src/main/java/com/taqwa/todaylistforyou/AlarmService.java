package com.taqwa.todaylistforyou;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class AlarmService extends Service {

    private Ringtone ringtone;
    private static boolean isAlarmRunning = false;  // এলার্ম চলছে কিনা তা ট্র্যাক করার জন্য ফ্ল্যাগ

    @Override
    public void onCreate() {
        super.onCreate();

        // Android 8.0 এর জন্য নোটিফিকেশন চ্যানেল তৈরি করুন
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "alarm_channel",
                    "Alarm Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // যদি অ্যালার্ম ইতোমধ্যে চলমান থাকে, তবে নতুন করে না শুরু করার ব্যবস্থা
        if (isAlarmRunning) {
            Log.d("AlarmService", "Alarm is already running.");
            return START_NOT_STICKY; // নতুন করে সার্ভিস শুরু না করে আগেরটাই চালু রাখুন
        }

        isAlarmRunning = true;  // এলার্ম চালু করা হয়েছে সেট করুন

        String taskTitle = intent.getStringExtra("task_title");
        int taskId = intent.getIntExtra("task_id", -1); // টাস্ক আইডি পান

        // Stop বাটনের Intent তৈরি করুন
        Intent stopIntent = new Intent(this, StopAlarmReceiver.class);
        stopIntent.setAction("STOP_ALARM"); // Action সেট করুন
        stopIntent.putExtra("task_title", taskTitle); // টাস্কের টাইটেল পাঠান
        stopIntent.putExtra("task_id", taskId); // টাস্ক আইডি পাঠান
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent to open the MainActivity when notification is clicked
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // নোটিফিকেশন তৈরি করুন
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "alarm_channel")
                .setSmallIcon(R.drawable.ic_completed)
                .setContentTitle("Alarm Ringing")
                .setContentText(taskTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)  // Stop বাটন যোগ করুন
                .setContentIntent(pendingIntent)  // Set the intent that will fire when the user taps the notification
                .setAutoCancel(true);  // Notification will disappear when clicked

        // Foreground Service শুরু করুন
        startForeground(1, builder.build());

        // এলার্ম বাজানো শুরু করুন
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        ringtone = RingtoneManager.getRingtone(this, alarmUri);
        ringtone.play();

        return START_STICKY; // সার্ভিস চালু রাখার জন্য START_STICKY ব্যবহার করুন
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // সার্ভিস বন্ধ হওয়ার আগে অ্যালার্ম বন্ধ করুন
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        isAlarmRunning = false;  // এলার্ম বন্ধ হওয়ায় ফ্ল্যাগ পরিবর্তন করুন
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
