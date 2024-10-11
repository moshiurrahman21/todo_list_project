package com.taqwa.todaylistforyou;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper; // Import the ItemTouchHelper
import com.google.android.material.snackbar.Snackbar; // Import Snackbar

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompletedTasksActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> completedTaskList = new ArrayList<>();
    private Task removedTask; // Recently removed task for undo
    private int removedPosition; // Position of the recently removed task
    private ImageView back_icon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_tasks);

        back_icon = findViewById(R.id.back_icon);



        back_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(CompletedTasksActivity.this, MainActivity.class));

                finish();

            }
        });

        recyclerView = findViewById(R.id.recycler_view_completed_tasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load completed tasks
        displayCompletedTasks();

        // Add ItemTouchHelper for swipe to delete
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false; // No need to move items
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                removedTask = completedTaskList.get(position); // Task to be removed
                removedPosition = position; // Save position for undo

                // Remove the task and notify the adapter
                completedTaskList.remove(position);
                taskAdapter.notifyItemRemoved(position);
                Toast.makeText(CompletedTasksActivity.this, "Task removed successfully!", Toast.LENGTH_SHORT).show();

                MediaPlayer deleteSound = MediaPlayer.create(CompletedTasksActivity.this, R.raw.delete_sound);
                deleteSound.start();


                // Show snackbar with UNDO action
                Snackbar snackbar = Snackbar.make(recyclerView, "Task deleted", Snackbar.LENGTH_LONG);
                snackbar.setAction("UNDO", v -> {
                    // Restore the task if UNDO is clicked
                    completedTaskList.add(removedPosition, removedTask); // Restore the task
                    taskAdapter.notifyItemInserted(removedPosition);
                    saveTaskToPreferences(removedTask); // Save back to preferences
                });
                snackbar.show();

                // Remove from shared preferences if not undone
                saveDeletedTaskToPreferences(removedTask);
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    Paint paint = new Paint();
                   if (dX < 0) {
                        // বাম দিকে সুইফট করার জন্য
                        paint.setColor(Color.RED);
                        RectF background = new RectF(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                        c.drawRect(background, paint);
                    }

                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }
        });



        itemTouchHelper.attachToRecyclerView(recyclerView); // Attach to RecyclerView
    }




    private void displayCompletedTasks() {
        SharedPreferences sharedPreferences = getSharedPreferences("COMPLETED_TASKS", MODE_PRIVATE);
        int completedTaskCount = sharedPreferences.getInt("COMPLETED_TASK_COUNT", 0);

        for (int i = 0; i < completedTaskCount; i++) {
            String taskData = sharedPreferences.getString("COMPLETED_TASK_" + i, "");
            String[] taskParts = taskData.split(";");

            if (taskParts.length >= 8) {
                int id = Integer.parseInt(taskParts[0]);
                String title = taskParts[1];
                List<String> descriptions = Arrays.asList(taskParts[2].split(","));
                String category = taskParts[3];
                String date = taskParts[4];
                String time = taskParts[5];
                boolean isAlarmSet = Boolean.parseBoolean(taskParts[6]);
                boolean isCompleted = Boolean.parseBoolean(taskParts[7]);

                Task task = new Task(id, title, category, descriptions, date, time);
                task.setAlarmSet(isAlarmSet);
                task.setCompleted(isCompleted);

                completedTaskList.add(task);
            }
        }

        taskAdapter = new TaskAdapter(completedTaskList, task -> {
            // এলার্ম ক্লিকের লজিক (যদি প্রয়োজন হয়)
        });

        recyclerView.setAdapter(taskAdapter);
        taskAdapter.notifyDataSetChanged();
    }

    // Save deleted task to shared preferences for permanent deletion
    private void saveDeletedTaskToPreferences(Task task) {
        SharedPreferences sharedPreferences = getSharedPreferences("COMPLETED_TASKS", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int completedTaskCount = sharedPreferences.getInt("COMPLETED_TASK_COUNT", 0);
        editor.putInt("COMPLETED_TASK_COUNT", completedTaskCount - 1); // Decrease count
        editor.apply();
    }

    // Save the restored task back to shared preferences
    private void saveTaskToPreferences(Task task) {
        SharedPreferences sharedPreferences = getSharedPreferences("COMPLETED_TASKS", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int completedTaskCount = sharedPreferences.getInt("COMPLETED_TASK_COUNT", 0);
        editor.putString("COMPLETED_TASK_" + completedTaskCount, task.getId() + ";" + task.getTitle() + ";" + String.join(",", task.getDescriptions()) + ";" + task.getCategory() + ";" + task.getDate() + ";" + task.getTime() + ";" + task.isAlarmSet() + ";" + task.isCompleted());
        editor.putInt("COMPLETED_TASK_COUNT", completedTaskCount + 1); // Increase count
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
