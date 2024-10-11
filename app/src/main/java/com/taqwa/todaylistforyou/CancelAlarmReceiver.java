package com.taqwa.todaylistforyou;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class CancelAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra("task_title");

        // এলার্ম ক্যান্সেল করার জন্য
        SharedPreferences sharedPreferences = context.getSharedPreferences("TASKS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        int taskCount = sharedPreferences.getInt("TASK_COUNT", 0);
        for (int i = 0; i < taskCount; i++) {
            String taskData = sharedPreferences.getString("TASK_" + i, "");
            String[] taskParts = taskData.split(";");

            if (taskParts.length == 8) {
                String title = taskParts[1];
                if (title.equals(taskTitle)) {
                    int id = Integer.parseInt(taskParts[0]);
                    cancelAlarm(context, id);
                    // এলার্ম বন্ধ হয়ে গেছে, সেটি SharedPreferences থেকে আপডেট করা হবে
                    taskParts[6] = "false";
                    String updatedTaskData = String.join(";", taskParts);
                    editor.putString("TASK_" + i, updatedTaskData);
                    editor.apply();
                    Toast.makeText(context, "Alarm canceled for task: " + taskTitle, Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    }

    private void cancelAlarm(Context context, int taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, taskId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
