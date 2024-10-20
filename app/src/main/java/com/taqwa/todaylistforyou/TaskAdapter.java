package com.taqwa.todaylistforyou;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnAlarmClickListener onAlarmClickListener;
    private OnItemClickListener onItemClickListener;

    // ইন্টারফেস ডিফাইন করা হয়েছে টাস্ক ক্লিকের জন্য
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    // ইন্টারফেস ডিফাইন করা হয়েছে এলার্ম ক্লিকের জন্য
    public interface OnAlarmClickListener {
        void onAlarmClick(Task task);
    }

    // কনস্ট্রাক্টরে এলার্ম ক্লিক লিসেনার পাস করা হচ্ছে
    public TaskAdapter(List<Task> taskList, OnAlarmClickListener listener) {
        this.taskList = taskList;
        this.onAlarmClickListener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // সঠিক লেআউট ফাইলটি ইনফ্লেট করুন
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.taskTitle.setText(task.getTitle());
        holder.taskCategory.setText(task.getCategory());
        holder.taskDate.setText(task.getDate());
        holder.taskTime.setText(task.getTime());

        // ডিসক্রিপশন দেখানোর জন্য
        List<String> descriptions = task.getDescriptions();

        // যদি ডিসক্রিপশন খালি থাকে, তবে কিছুই দেখাবো না
        if (descriptions == null || descriptions.isEmpty()) {
            holder.taskDescription.setVisibility(View.GONE);  // ডিসক্রিপশন দেখাবো না, স্থান ফাঁকা থাকবে
        } else {
            // যদি ডিসক্রিপশন থাকে, তবে পয়েন্ট আকারে দেখাবো
            StringBuilder descriptionBuilder = new StringBuilder();
            for (int i = 0; i < descriptions.size(); i++) {
                descriptionBuilder.append(i + 1).append(". ").append(descriptions.get(i)).append("\n");
            }
            holder.taskDescription.setVisibility(View.VISIBLE);  // ডিসক্রিপশন দেখাবো
            holder.taskDescription.setText(descriptionBuilder.toString().trim());
        }

        // এলার্ম সেট করা থাকলে আইকন পরিবর্তন
        if (task.isAlarmSet()) {
            holder.alarmIcon.setImageResource(R.drawable.ic_alarm_on); // এলার্ম সক্রিয় আইকন
        } else {
            holder.alarmIcon.setImageResource(R.drawable.ic_alarm_off); // এলার্ম নিষ্ক্রিয় আইকন
        }

        if (task.getTimeInMillis() < System.currentTimeMillis()) {
            // অতীতের সময় হলে এলার্ম বন্ধের আইকন দেখাও
            holder.alarmIcon.setImageResource(R.drawable.ic_alarm_off);
            // অতীতের সময়ে ক্লিক করলে অ্যালার্ম সেট/বাতিল করতে পারবে না
            holder.alarmIcon.setOnClickListener(v -> {
            });
        } else {
            // ভবিষ্যৎ সময় হলে এলার্মের আইকন দেখাও এবং সেট/বাতিল করার অনুমতি দিন
            if (task.isAlarmSet()) {
                holder.alarmIcon.setImageResource(R.drawable.ic_alarm_on); // এলার্ম অন আইকন দেখাও
            } else {
                holder.alarmIcon.setImageResource(R.drawable.ic_alarm_off); // এলার্ম অফ আইকন দেখাও
            }
        }

        // এলার্ম আইকনে ক্লিক করলে এলার্ম সেট বা ক্যান্সেল হবে
        holder.alarmIcon.setOnClickListener(v -> {
            if (onAlarmClickListener != null) {
                onAlarmClickListener.onAlarmClick(task);
            }
        });

        // টাস্ক আইটেমে ক্লিক করলে টাস্ক সম্পাদনা ডায়ালগ ওপেন হবে
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position);
            }
        });
    }



    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle, taskDescription, taskCategory, taskDate, taskTime;
        ImageView alarmIcon;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.task_title);
            taskDescription = itemView.findViewById(R.id.task_description);
            taskCategory = itemView.findViewById(R.id.task_category);
            taskDate = itemView.findViewById(R.id.task_date);
            taskTime = itemView.findViewById(R.id.task_time);
            alarmIcon = itemView.findViewById(R.id.iv_task_alarm); // এলার্ম আইকন
        }
    }

    @Override
    public long getItemId(int position) {
        // প্রতিটি আইটেমের জন্য একটি ইউনিক আইডি রিটার্ন করা হচ্ছে
        return taskList.get(position).getId(); // অথবা তুমি যে আইডি ব্যবহার করতে চাও
    }
}
