package com.taqwa.todaylistforyou;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "Alarm received! Starting service...");

        // টাস্কের শিরোনাম ইনটেন্ট থেকে পান
        String taskTitle = intent.getStringExtra("task_title"); // এলার্ম শুরুর সময় টাস্কের শিরোনাম নিন
        int taskId = intent.getIntExtra("task_id", -1); // টাস্ক আইডি পান

        // এলার্ম সার্ভিস শুরু করুন
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra("task_title", taskTitle); // টাস্কের শিরোনাম যুক্ত করুন সার্ভিসে
        serviceIntent.putExtra("task_id", taskId); // টাস্ক আইডি যুক্ত করুন

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent); // Foreground service শুরু করুন
        } else {
            context.startService(serviceIntent); // পুরনো ভার্সনের জন্য
        }
    }
}
