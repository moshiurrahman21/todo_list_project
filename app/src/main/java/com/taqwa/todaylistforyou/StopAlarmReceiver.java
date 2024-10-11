package com.taqwa.todaylistforyou;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

public class StopAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra("task_title");

        // এলার্ম সার্ভিস বন্ধ করুন
        Intent serviceIntent = new Intent(context, AlarmService.class);
        context.stopService(serviceIntent);  // সার্ভিস বন্ধ করুন

        // টাস্কের এলার্ম স্ট্যাটাসকে false করা এবং SharedPreferences আপডেট করা
        SharedPreferences sharedPreferences = context.getSharedPreferences("TASKS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        int taskCount = sharedPreferences.getInt("TASK_COUNT", 0);
        for (int i = 0; i < taskCount; i++) {
            String taskData = sharedPreferences.getString("TASK_" + i, "");
            String[] taskParts = taskData.split(";");

            if (taskParts.length == 8) {
                String title = taskParts[1];
                if (title.equals(taskTitle)) {
                    taskParts[6] = "false"; // এলার্ম স্ট্যাটাস false করা
                    String updatedTaskData = String.join(";", taskParts);
                    editor.putString("TASK_" + i, updatedTaskData);
                    editor.apply();

                    Intent updateUIIntent = new Intent("UPDATE_UI");
                    updateUIIntent.putExtra("task_id", Integer.parseInt(taskParts[0]));
                    context.sendBroadcast(updateUIIntent);

                    Toast.makeText(context, "Alarm stopped for task: " + taskTitle, Toast.LENGTH_SHORT).show();

                    // টাস্কের এলার্ম আইকন পরিবর্তন করুন
                    Intent updateIconIntent = new Intent(context, MainActivity.class);
                    updateIconIntent.putExtra("task_id", Integer.parseInt(taskParts[0]));
                    updateIconIntent.putExtra("update_icon", true); // আইকন পরিবর্তনের জন্য সিগনাল দিন
                    context.sendBroadcast(updateIconIntent);

                    break;
                }
            }
        }
    }
}
