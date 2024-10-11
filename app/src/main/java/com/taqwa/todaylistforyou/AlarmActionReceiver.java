package com.taqwa.todaylistforyou;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String taskTitle = intent.getStringExtra("task_title");

        if (action != null) {
            switch (action) {
                case "CANCEL_ALARM":
                    cancelAlarm(context, taskTitle);
                    break;
                case "COMPLETE_TASK":
                    completeTask(context, taskTitle);
                    break;
            }
        }
    }

    private void cancelAlarm(Context context, String taskTitle) {
        // এলার্ম ক্যান্সেল করার লজিক এখানে যোগ করুন
        Toast.makeText(context, "Alarm canceled for: " + taskTitle, Toast.LENGTH_SHORT).show();

        // নোটিফিকেশন ক্যান্সেল করা
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(taskTitle.hashCode());
    }

    private void completeTask(Context context, String taskTitle) {
        // টাস্ক কমপ্লিট করার লজিক এখানে যোগ করুন
        Toast.makeText(context, "Task completed: " + taskTitle, Toast.LENGTH_SHORT).show();

        // নোটিফিকেশন ক্যান্সেল করা
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(taskTitle.hashCode());
    }
}
