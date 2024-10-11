package com.taqwa.todaylistforyou;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
//import java.util.Objects;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnAlarmClickListener {

    private LottieAnimationView fab;
    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList = new ArrayList<>();
    private String taskDate = "";
    private String taskTime = "";
    private MediaPlayer deleteSound;
    private MediaPlayer completeSound;
    private MediaPlayer sendSound;
    private int removedPosition;
    private AlarmManager alarmManager;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private LottieAnimationView background_image;  // ব্যাকগ্রাউন্ড ইমেজের জন্য ভেরিয়েবল
    private TextView background_text;
    private static final int MAX_SHARED_PREFERENCES_SIZE_MB = 2; // Maximum size in MB
    private InterstitialAd mInterstitialAd;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.blue)); // এখানে `your_color` রং পরিবর্তন করুন


        //==================== Mobile Ads Initialize ====================
        //================================================================


        new Thread(
                () -> {
                    // Initialize the Google Mobile Ads SDK on a background thread.
                    MobileAds.initialize(this, initializationStatus -> {
                    });
                })
                .start();

        //================================================================

        // Banner Ads
        // Step 3 : Finding adViewContainer and call loadBanner() method
        LinearLayout adViewContainer = findViewById(R.id.adViewContainer);
        loadBanner(adViewContainer);


        //================================================================
        //================================================================

        loadFullScreenAds();

        //================================================================
        //================================================================



        // ******************************************************************
        //*************************** Identify *****************************

        ImageView menuIcon = findViewById(R.id.menu_icon);
        background_image = findViewById(R.id.background_image);
        background_text = findViewById(R.id.background_text);
        fab = findViewById(R.id.fab);
        recyclerView = findViewById(R.id.recycler_view_tasks);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        //#####################################################################
        //#####################################################################

        //******************************************************************
        //***************** Adapter & Recycler View ********************

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(taskList, this);
        recyclerView.setAdapter(taskAdapter);

        //#####################################################################
        //#####################################################################



        //******************************************************************
        //*************************** Methood Declear *****************************


        displayTasks();
        checkIfTaskListIsEmpty();
        createNotificationChannel();


        // #####################################################################
        //#####################################################################


        //******************************************************************
        //************************** Sound Declear *************************


        sendSound = MediaPlayer.create(this, R.raw.send_sound);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        background_image.setSpeed(0.6f);

        //######################################################################
        //######################################################################

        //**************************************************************************
        // ******************************* Methood Declear *************************

        // UI আপডেটের জন্য ব্রডকাস্ট রিসিভার রেজিস্ট্রেশন
        IntentFilter filter = new IntentFilter("UPDATE_UI");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(updateUIReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }


        //----------------------------------------------------------------------

        // Set up the drawer toggle
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        //######################################################################
        //######################################################################


        //**********************************************************************
        //************************** OnClick Listener -*************************

        menuIcon.setOnClickListener(view -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });
        //----------------------------------------------------------------------
        fab.setOnClickListener(view -> openAddTaskDialog());
        //----------------------------------------------------------------------

        taskAdapter.setOnItemClickListener(position -> {
            Task task = taskList.get(position);
            openEditTaskDialog(task, position);
        });
        //----------------------------------------------------------------------


        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Task removedTask = taskList.get(position); // টাস্কটি ডিলিট করার জন্য
                removedPosition = position;

                if (direction == ItemTouchHelper.LEFT) {
                    // টাস্কটি ডিলিট করা হচ্ছে
                    taskList.remove(position);
                    taskAdapter.notifyItemRemoved(position);
                    removeTaskFromSharedPreferences(removedTask); // টাস্কটি SharedPreferences থেকে ডিলিট হচ্ছে
                    Toast.makeText(MainActivity.this, "Task removed successfully!", Toast.LENGTH_SHORT).show();

                    MediaPlayer deletedSound = MediaPlayer.create(MainActivity.this, R.raw.delete_sound);
                    deletedSound.start();

                    // UNDO অপশন দেখানোর জন্য Snackbar
                    Snackbar snackbar = Snackbar.make(recyclerView, "Task deleted", Snackbar.LENGTH_LONG);
                    snackbar.setAction("UNDO", v -> {
                        // টাস্কটি পুনরায় যোগ করা হচ্ছে
                        taskList.add(removedPosition, removedTask);
                        taskAdapter.notifyItemInserted(removedPosition);
                        saveTaskToSharedPreferences(removedTask); // পুনরায় SharedPreferences-এ সেভ করা হচ্ছে
                    });
                    snackbar.addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                                // UNDO বাটনে ক্লিক না করা হলে টাস্কটি স্থায়ীভাবে ডিলিট করা হচ্ছে
                                removeTaskFromSharedPreferences(removedTask);
                            }
                        }
                    });
                    snackbar.show();
                    checkIfTaskListIsEmpty();
                }
                else if (direction == ItemTouchHelper.RIGHT) {
                    markTaskComplete(position);

                    checkIfTaskListIsEmpty();
                    Toast.makeText(MainActivity.this, "Well done! \uD83C\uDF89 Task completed. Check it in 'Completed Tasks'!", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false; // মুভ করার জন্য প্রয়োজন নেই
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    Paint paint = new Paint();
                    if (dX > 0) {
                        // ডান দিকে সুইফট করার জন্য
                        paint.setColor(Color.GREEN);
                        RectF background = new RectF(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom());
                        c.drawRect(background, paint);
                    } else if (dX < 0) {
                        // বাম দিকে সুইফট করার জন্য
                        paint.setColor(Color.RED);
                        RectF background = new RectF(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                        c.drawRect(background, paint);
                    }

                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }
        });

        // RecyclerView-তে ItemTouchHelper অ্যাটাচ করুন
        itemTouchHelper.attachToRecyclerView(recyclerView);

        //----------------------------------------------------------------------

        // Navigation Drawer item click listener
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.menu_completed_tasks) {
                    if (mInterstitialAd != null) {
                        mInterstitialAd.show(MainActivity.this);
                    } else {
                        Log.d("TAG", "The interstitial ad wasn't ready yet.");
                        Toast.makeText(MainActivity.this, "Ad couldn't load. Please check your internet connection.", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MainActivity.this, CompletedTasksActivity.class);
                        startActivity(intent); // বিজ্ঞাপন লোড না হলে সরাসরি গন্তব্যে পাঠানো
                    }
                    drawerLayout.closeDrawers(); // Close the drawer after the action
                }
                return true;
            }
        });

        //#######################################################################
        //#######################################################################




        //=================================================================
    }//<<<<<<<<<<<<<<<======== OnCreate Methood END ======>>>>>>>>>>>>>>>>>





    @Override
    public void onAlarmClick(Task task) {
        String taskDate = task.getDate();
        String taskTime = task.getTime();

        // ইউজারের দেওয়া তারিখ ও সময় থেকে ক্যালেন্ডার তৈরি করা হচ্ছে
        Calendar calendar = Calendar.getInstance();
        String[] dateParts = taskDate.split("/");
        String[] timeParts = taskTime.split(" ");

        if (dateParts.length == 3 && timeParts.length == 2) {
            int day = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]) - 1; // মাস 0 থেকে শুরু হয়
            int year = Integer.parseInt(dateParts[2]);

            String[] hourMin = timeParts[0].split(":");
            int hour = Integer.parseInt(hourMin[0]);
            int minute = Integer.parseInt(hourMin[1]);
            String amPm = timeParts[1];

            if (amPm.equalsIgnoreCase("PM") && hour != 12) {
                hour += 12;
            } else if (amPm.equalsIgnoreCase("AM") && hour == 12) {
                hour = 0;
            }

            calendar.set(year, month, day, hour, minute, 0);

            // চেক করা হচ্ছে বর্তমান সময়ের তুলনায় টাস্কের সময় অতীত নাকি ভবিষ্যতের
            if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
                // যদি এলার্ম ইতিমধ্যে সেট করা থাকে, তখন সেট ক্যান্সেল করা হবে
                if (task.isAlarmSet()) {
                    cancelAlarm(task);
                    task.setAlarmSet(false);
                    taskAdapter.notifyDataSetChanged();
                } else {
                    // যদি এলার্ম সেট না থাকে, সেট করা হবে
                    setAlarm(task);
                    task.setAlarmSet(true);
                    taskAdapter.notifyDataSetChanged();
                }
            } else {
                // অতীতের সময় হলে এলার্ম সেট হবে না
                Toast.makeText(this, "Cannot set alarm for past time", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Invalid date/time format for task: " + task.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openAddTaskDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();

        EditText etTaskTitle = dialogView.findViewById(R.id.et_task_title);
        LinearLayout layoutDescriptions = dialogView.findViewById(R.id.layout_descriptions);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category);
        ImageView btnSaveTask = dialogView.findViewById(R.id.btn_save_task);
        ImageView ivAddDescription = dialogView.findViewById(R.id.iv_add_description);
        ImageView ivSetAlarm = dialogView.findViewById(R.id.iv_set_alarm);

        // ক্যাটাগরি স্পিনারে ডাটা সেট করা
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.category_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        ivSetAlarm.setOnClickListener(view -> {
            showDateTimePicker(new OnDateTimeSelectedListener() {
                @Override
                public void onDateTimeSelected(String date, String time) {
                    taskDate = date;
                    taskTime = time;
                }
            });
        });

        ivAddDescription.setOnClickListener(view -> {
            addDescriptionField(layoutDescriptions, ""); // Add empty description field
        });

        btnSaveTask.setOnClickListener(view -> {
            String taskTitle = etTaskTitle.getText().toString().trim();
            String selectedCategory = spinnerCategory.getSelectedItem().toString();
            List<String> updatedDescriptions = new ArrayList<>();

            // সকল ডিসক্রিপশন নেওয়া হচ্ছে
            for (int i = 0; i < layoutDescriptions.getChildCount(); i++) {
                View child = layoutDescriptions.getChildAt(i);
                if (child instanceof EditText) {
                    EditText descField = (EditText) child;
                    updatedDescriptions.add(descField.getText().toString().trim());
                }
            }

            // যদি টাইটেল খালি থাকে
            if (taskTitle.isEmpty()) {
                Toast.makeText(this, "Please give the title of the task", Toast.LENGTH_SHORT).show();
            } else {
                // যদি ইউজার কোনো তারিখ এবং সময় না দেয়, তাহলে বর্তমান সময় ও তারিখ ব্যবহার করা হবে
                String finalDate;
                String finalTime;

                if (taskDate.isEmpty() && taskTime.isEmpty()) {
                    finalDate = getCurrentDate(); // বর্তমান তারিখ
                    finalTime = getCurrentTime(); // বর্তমান সময়
                } else {
                    finalDate = taskDate;
                    finalTime = taskTime;
                }

                sendSound = MediaPlayer.create(this, R.raw.send_sound);
                sendSound.start();

                // নতুন টাস্ক তৈরি হচ্ছে
                Task newTask = new Task(generateUniqueId(), taskTitle, selectedCategory, updatedDescriptions, finalDate, finalTime);
                saveTask(newTask); // টাস্ক সেভ করা হচ্ছে
                dialog.dismiss();
                checkIfTaskListIsEmpty();

            }
        });

        dialog.show();

    }


    interface OnDateTimeSelectedListener {
        void onDateTimeSelected(String date, String time);
    }

    private void showDateTimePicker(OnDateTimeSelectedListener callback) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;

            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                hourOfDay = (hourOfDay > 12) ? hourOfDay - 12 : (hourOfDay == 0 ? 12 : hourOfDay);
                String selectedTime = hourOfDay + ":" + String.format("%02d", minute) + " " + amPm;

                callback.onDateTimeSelected(selectedDate, selectedTime);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
            timePickerDialog.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }
    private void saveTask(Task task) {
        taskList.add(0, task);

        SharedPreferences sharedPreferences = getSharedPreferences("TASKS", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        int taskCount = taskList.size();
        editor.putInt("TASK_COUNT", taskCount);

        for (int i = 0; i < taskCount; i++) {
            Task currentTask = taskList.get(i);
            String descriptions = currentTask.getDescriptions() == null || currentTask.getDescriptions().isEmpty() ? "" : TextUtils.join(",", currentTask.getDescriptions());

            editor.putString("TASK_" + i, currentTask.getId() + ";" + currentTask.getTitle() + ";" +
                    descriptions + ";" + // খালি ডিসক্রিপশন ফিল্টার
                    currentTask.getCategory() + ";" + currentTask.getDate() + ";" + currentTask.getTime() +
                    ";" + currentTask.isAlarmSet() + ";" + currentTask.isCompleted());
        }
        editor.apply();

        taskAdapter.notifyItemInserted(0);
        recyclerView.smoothScrollToPosition(0);
    }




    private void saveTasksToSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("TASKS", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        int taskCount = taskList.size();
        editor.putInt("TASK_COUNT", taskCount);

        for (int i = 0; i < taskCount; i++) {
            Task task = taskList.get(i);
            editor.putString("TASK_" + i, task.getId() + ";" + task.getTitle() + ";" + task.getDescriptions() + ";" + task.getCategory() + ";" + task.getDate() + ";" + task.getTime() + ";" + task.isAlarmSet() + ";" + task.isCompleted());
        }
        editor.apply();

        taskAdapter.notifyDataSetChanged();
    }



    private void displayTasks() {
        SharedPreferences sharedPreferences = getSharedPreferences("TASKS", MODE_PRIVATE);
        int taskCount = sharedPreferences.getInt("TASK_COUNT", 0);


        for (int i = 0; i < taskCount; i++) {
            String taskData = sharedPreferences.getString("TASK_" + i, "");
            String[] taskParts = taskData.split(";");

            if (taskParts.length == 8) {
                int id = Integer.parseInt(taskParts[0]);
                String title = taskParts[1];

                // ডিসক্রিপশন লোড করার সময় ব্র্যাকেট সরানো হচ্ছে
                String descriptionsString = taskParts[2].replace("[", "").replace("]", "");
                List<String> descriptions = new ArrayList<>(Arrays.asList(descriptionsString.split(",")));

                String category = taskParts[3];
                String date = taskParts[4];
                String time = taskParts[5];
                boolean isAlarmSet = Boolean.parseBoolean(taskParts[6]);
                boolean isCompleted = Boolean.parseBoolean(taskParts[7]);

                Task task = new Task(id, title, category, descriptions, date, time);
                task.setAlarmSet(isAlarmSet);
                task.setCompleted(isCompleted);
                taskList.add(task);
            }
        }

        taskAdapter.notifyDataSetChanged();
        checkIfTaskListIsEmpty();
    }



    private void openEditTaskDialog(Task task, int position) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(dialogView);
        AlertDialog dialog = dialogBuilder.create();

        EditText etTaskTitle = dialogView.findViewById(R.id.et_task_title);
        LinearLayout layoutDescriptions = dialogView.findViewById(R.id.layout_descriptions); // Layout for descriptions
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category);
        ImageView btnSaveTask = dialogView.findViewById(R.id.btn_save_task);
        ImageView ivAddDescription = dialogView.findViewById(R.id.iv_add_description);
        ImageView ivSetAlarm = dialogView.findViewById(R.id.iv_set_alarm);

        // Populate fields with current task data
        etTaskTitle.setText(task.getTitle());
        List<String> descriptions = task.getDescriptions();



        // Set the category spinner
        String[] categories = {"Personal", "Work", "Shopping", "Study", "Others"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setSelection(adapter.getPosition(task.getCategory()));



        // Set existing descriptions
        for (String description : descriptions) {
            addDescriptionField(layoutDescriptions, description);
        }

        spinnerCategory.setSelection(adapter.getPosition(task.getCategory()));

        // Alarm icon handling
        if (task.isAlarmSet()) {
            ivSetAlarm.setImageResource(R.drawable.ic_alarm_on_dialog);
        } else {
            ivSetAlarm.setImageResource(R.drawable.ic_alarm_off_dialog);
        }


        ivAddDescription.setOnClickListener(view -> {
            addDescriptionField(layoutDescriptions, ""); // Add empty description field
        });

        ivSetAlarm.setOnClickListener(view -> {
            if (!task.isAlarmSet()) {
                showDateTimePicker(new OnDateTimeSelectedListener() {
                    @Override
                    public void onDateTimeSelected(String date, String time) {
                        taskDate = date;
                        taskTime = time;
                        ivSetAlarm.setImageResource(R.drawable.ic_alarm_on_dialog); // Alarm active icon
                        task.setAlarmSet(true);
                    }
                });
            } else {
                cancelAlarm(task);
                task.setAlarmSet(false);
                ivSetAlarm.setImageResource(R.drawable.ic_alarm_off_dialog); // Alarm inactive icon
            }
        });

        btnSaveTask.setOnClickListener(view -> {
            String taskTitle = etTaskTitle.getText().toString().trim();
            String selectedCategory = spinnerCategory.getSelectedItem().toString();

            sendSound = MediaPlayer.create(this, R.raw.send_sound);
            sendSound.start();

            if (taskTitle.isEmpty()) {
                Toast.makeText(this, "Please give the title of the task", Toast.LENGTH_SHORT).show();
            } else {
                task.setTitle(taskTitle);
                task.setCategory(selectedCategory);

                // Collect updated descriptions
                List<String> updatedDescriptions = new ArrayList<>();
                for (int i = 0; i < layoutDescriptions.getChildCount(); i++) {
                    View child = layoutDescriptions.getChildAt(i);
                    if (child instanceof EditText) {
                        EditText descField = (EditText) child;
                        updatedDescriptions.add(descField.getText().toString().trim());
                    }
                }

                task.setDescriptions(updatedDescriptions); // Update task descriptions

                task.setDate(taskDate.isEmpty() ? task.getDate() : taskDate);
                task.setTime(taskTime.isEmpty() ? task.getTime() : taskTime);

                if (task.isAlarmSet()) {
                    setAlarm(task);
                } else {
                    cancelAlarm(task);
                }

                taskList.set(position, task);
                saveTasksToSharedPreferences();

                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // Method to add a description field with a delete icon
    private void addDescriptionField(LinearLayout layout, String description) {
        EditText descEditText = new EditText(this);
        descEditText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        descEditText.setHint("Description");
        descEditText.setText(description); // Set existing description if available
        descEditText.setBackground(getResources().getDrawable(R.drawable.task_background));
        descEditText.setPadding(20, 20, 20, 20);
        descEditText.setTextColor(getResources().getColor(R.color.black));
        descEditText.setHintTextColor(getResources().getColor(R.color.gray));




        ImageView deleteIcon = new ImageView(this);
        deleteIcon.setImageResource(R.drawable.ic_delete); // Delete icon
        deleteIcon.setOnClickListener(v -> {
            layout.removeView(descEditText);
            layout.removeView(deleteIcon);
        });

        layout.addView(descEditText);
        layout.addView(deleteIcon);
    }




    private void markTaskComplete(int position) {
        MediaPlayer completed_sound = MediaPlayer.create(MainActivity.this, R.raw.completed_sound);
        completed_sound.start();
        Task task = taskList.get(position);
        task.setCompleted(true); // টাস্ককে কমপ্লিট হিসেবে মার্ক করা

        // যদি টাস্কের এলার্ম সেট করা থাকে, তা বাতিল করা হবে
        if (task.isAlarmSet()) {
            cancelAlarm(task);
            task.setAlarmSet(false);
        }

        // টাস্ক লিস্ট থেকে সরিয়ে নেওয়া হচ্ছে
        taskList.remove(position);
        saveTasksToSharedPreferences();

        // কমপ্লিট হওয়া টাস্ককে সেভ করার জন্য
        saveCompletedTask(task); // কমপ্লিট হওয়া টাস্ক সেভ করা হচ্ছে

        taskAdapter.notifyItemRemoved(position); // UI থেকে টাস্ক সরানো হচ্ছে
    }

    private void saveCompletedTask(Task completedTask) {
        SharedPreferences sharedPreferences = getSharedPreferences("COMPLETED_TASKS", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // কমপ্লিট টাস্কগুলোর সংখ্যা ইনক্রিমেন্ট করা হচ্ছে
        int completedTaskCount = sharedPreferences.getInt("COMPLETED_TASK_COUNT", 0);
        editor.putInt("COMPLETED_TASK_COUNT", completedTaskCount + 1);

        // টাস্কের সমস্ত তথ্য সেভ করা হচ্ছে
        editor.putString("COMPLETED_TASK_" + completedTaskCount,
                completedTask.getId() + ";" + completedTask.getTitle() + ";" +
                        TextUtils.join(",", completedTask.getDescriptions()) + ";" +
                        completedTask.getCategory() + ";" + completedTask.getDate() + ";" +
                        completedTask.getTime() + ";" + completedTask.isAlarmSet() + ";" + completedTask.isCompleted());

        editor.apply(); // সেভ করা হচ্ছে
    }


    private String getCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // 0-এ বেসড, তাই 1 যোগ করতে হবে
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return day + "/" + month + "/" + year;
    }

    private String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        String amPm = (hour >= 12) ? "PM" : "AM";
        hour = (hour > 12) ? hour - 12 : (hour == 0 ? 12 : hour);
        return hour + ":" + String.format("%02d", minute) + " " + amPm;
    }
    private int generateUniqueId() {
        // ইউনিক আইডি জেনারেট করার জন্য সিস্টেমের সময় ব্যবহার করতে পারেন
        return (int) System.currentTimeMillis();
    }

    private void setAlarm(Task task) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("task_title", task.getTitle());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, task.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        String[] dateParts = task.getDate().split("/");
        if (dateParts.length != 3) {
            Toast.makeText(this, "Invalid date format for task: " + task.getTitle(), Toast.LENGTH_SHORT).show();
            return;
        }
        int day = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]) - 1; // Months are 0-based
        int year = Integer.parseInt(dateParts[2]);

        String[] timeParts = task.getTime().split(" ");
        if (timeParts.length != 2) {
            Toast.makeText(this, "Invalid time format for task: " + task.getTitle(), Toast.LENGTH_SHORT).show();
            return;
        }
        String[] hourMin = timeParts[0].split(":");
        if (hourMin.length != 2) {
            Toast.makeText(this, "Invalid time format for task: " + task.getTitle(), Toast.LENGTH_SHORT).show();
            return;
        }
        int hour = Integer.parseInt(hourMin[0]);
        int minute = Integer.parseInt(hourMin[1]);
        String amPm = timeParts[1];

        if (amPm.equalsIgnoreCase("PM") && hour != 12) {
            hour += 12;
        } else if (amPm.equalsIgnoreCase("AM") && hour == 12) {
            hour = 0;
        }

        calendar.set(year, month, day, hour, minute, 0);

        // ভবিষ্যতের জন্য এলার্ম সেট করার অনুমতি এবং অতীতের সময়ের জন্য না সেট করার ব্যবস্থা
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            Toast.makeText(this, "Alarm time for task '" + task.getTitle() + "' is in the past!", Toast.LENGTH_SHORT).show();
            return;
        }

        // এলার্ম সেট করা হচ্ছে
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Toast.makeText(this, "Alarm set for task: " + task.getTitle(), Toast.LENGTH_SHORT).show();

        // টাস্কের এলার্ম স্ট্যাটাস আপডেট
        task.setAlarmSet(true); // টাস্কের এলার্ম স্ট্যাটাস সেভ করা
        saveTaskToSharedPreferences(task); // টাস্ক সেভ করা হচ্ছে
    }

    private void cancelAlarm(Task task) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, task.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // এলার্ম বন্ধ করা হচ্ছে
        alarmManager.cancel(pendingIntent);
        Toast.makeText(this, "Alarm canceled for task: " + task.getTitle(), Toast.LENGTH_SHORT).show();

        // টাস্কের এলার্ম স্ট্যাটাস আপডেট
        task.setAlarmSet(false); // টাস্কের এলার্ম স্ট্যাটাস false করা
        saveTaskToSharedPreferences(task); // টাস্ক সেভ করা হচ্ছে
    }


    public void completeTask(Task task) {
        // টাস্কটি কমপ্লিট করা হবে
        // এখানে আপনার ডাটাবেস আপডেট করার কোড যুক্ত করুন
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "TASK_ALARM",
                    "Task Alarm Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        return handleMenuItemClick(item.getItemId());

    }

    private boolean handleMenuItemClick(int itemId) {
        if (itemId == R.id.menu_completed_tasks) {
            Intent intent = new Intent(this, CompletedTasksActivity.class);
            startActivity(intent);
            return true;
        } else {
            return false;
        }
    }


//===========================================================================


    private void removeTaskFromSharedPreferences(Task task) {
        SharedPreferences sharedPreferences = getSharedPreferences("TASKS", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        int taskCount = sharedPreferences.getInt("TASK_COUNT", 0);

        for (int i = 0; i < taskCount; i++) {
            String taskData = sharedPreferences.getString("TASK_" + i, "");

            if (taskData != null && !taskData.isEmpty()) {
                String[] taskParts = taskData.split(";");

                // Ensure taskParts[0] is not empty or null before parsing
                if (taskParts.length > 0 && taskParts[0] != null && !taskParts[0].isEmpty()) {
                    try {
                        int taskId = Integer.parseInt(taskParts[0]);

                        if (taskId == task.getId()) {
                            editor.remove("TASK_" + i); // টাস্কটি ডিলিট করা হচ্ছে
                            break;
                        }

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        // Log error message or handle the invalid number format gracefully
                        Log.e("Error", "Invalid task ID format for TASK_" + i);
                    }
                } else {
                    // Log a message if taskParts[0] is invalid
                    Log.e("Error", "Task ID is empty or null for TASK_" + i);
                }
            }
        }

        // Update task count in shared preferences after deletion
        editor.putInt("TASK_COUNT", taskList.size()); // টাস্কের নতুন সংখ্যা আপডেট হচ্ছে
        editor.apply();
        checkIfTaskListIsEmpty();
    }



//===========================================================================

    private void saveTaskToSharedPreferences(Task task) {
        SharedPreferences sharedPreferences = getSharedPreferences("TASKS", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();


        // Check the size of the SharedPreferences
        if (getSharedPreferencesSizeInMB(sharedPreferences) >= MAX_SHARED_PREFERENCES_SIZE_MB) {
            Toast.makeText(this, "Storage full! Please delete some completed tasks to add new ones.", Toast.LENGTH_LONG).show();
            return; // Stop the function if storage is full
        }

        // টাস্কের descriptions সেভ করার আগে ব্র্যাকেট সরানো হচ্ছে
        String descriptions = task.getDescriptions() == null || task.getDescriptions().isEmpty() ? "" : TextUtils.join(",", task.getDescriptions());
        descriptions = descriptions.replace("[", "").replace("]", "");

        // টাস্কের তথ্য সেভ করা হচ্ছে
        editor.putString("TASK_" + task.getId(), task.getId() + ";" + task.getTitle() + ";" +
                descriptions + ";" + task.getCategory() + ";" + task.getDate() + ";" + task.getTime() +
                ";" + task.isAlarmSet() + ";" + task.isCompleted());
        editor.apply(); // সেভ করা হচ্ছে

        // সেভ হওয়ার পর UI আপডেটের জন্য notifyDataSetChanged() কল
        taskAdapter.notifyDataSetChanged();
        checkIfTaskListIsEmpty();
    }





    // MainActivity.java
    private BroadcastReceiver updateUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // টাস্ক আইডি পান
            int taskId = intent.getIntExtra("task_id", -1);

            // যদি সঠিক টাস্ক আইডি পাওয়া যায়, তখন লিস্ট আপডেট করুন
            if (taskId != -1) {
                for (int i = 0; i < taskList.size(); i++) {
                    Task task = taskList.get(i);
                    if (task.getId() == taskId) {
                        task.setAlarmSet(false);  // এলার্ম বন্ধ করা হয়েছে
                        taskAdapter.notifyItemChanged(i);  // নির্দিষ্ট টাস্ক আপডেট করা হচ্ছে
                        break;
                    }
                }
            }
        }
    };



   // ===========================================================================
private BroadcastReceiver updateIconReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getBooleanExtra("update_icon", false)) {
            int taskId = intent.getIntExtra("task_id", -1);
            updateTaskAlarmIcon(taskId); // আইকন পরিবর্তন করার জন্য একটি মেথড কল করুন
        }
    }
};

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("UPDATE_UI");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(updateIconReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(updateIconReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ব্রডকাস্ট রিসিভার আনরেজিস্টার করুন
        unregisterReceiver(updateUIReceiver);
    }



    // নতুন মেথড যা টাস্কের আইকন পরিবর্তন করবে
    private void updateTaskAlarmIcon(int taskId) {
        for (Task task : taskList) {
            if (task.getId() == taskId) {
                task.setAlarmSet(false); // আইকন পরিবর্তন করার সময় স্ট্যাটাস পরিবর্তন করুন
                taskAdapter.notifyDataSetChanged();
                break;
            }
        }
    }

    // Method to get the size of SharedPreferences in MB
    private int getSharedPreferencesSizeInMB(SharedPreferences sharedPreferences) {
        try {
            // Get all the keys and values
            Map<String, ?> allEntries = sharedPreferences.getAll();
            int size = 0;

            // Calculate the size of each entry
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                String value = String.valueOf(entry.getValue());
                size += key.length() + value.length();
            }

            // Return the size in MB
            return size / (1024 * 1024);
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // Return 0 on failure
        }
    }
//===========================================================================

    // চেক করা হবে টাস্ক লিস্ট ফাঁকা কিনা
    private void checkIfTaskListIsEmpty() {
        if (taskList.isEmpty()) {
            // যদি কোনো টাস্ক না থাকে, ব্যাকগ্রাউন্ড ইমেজ দেখাও
            background_image.setVisibility(View.VISIBLE);
            background_text.setVisibility(View.VISIBLE);
            recyclerView.setBackgroundColor(getResources().getColor(android.R.color.transparent)); // লিস্টের ব্যাকগ্রাউন্ড ক্লিয়ার করা
        } else {
            // টাস্ক থাকলে ব্যাকগ্রাউন্ড ইমেজ লুকাও
            background_image.setVisibility(View.GONE);
            background_text.setVisibility(View.GONE);
            recyclerView.setBackgroundColor(getResources().getColor(R.color.offGreen)); // তোমার ডিফল্ট ব্যাকগ্রাউন্ড কালার
        }
    }






    ///====================================================
    private static final int TIME_INTERVAL = 2000; // # milliseconds, desired
    private long mBackPressed;

    // When user click bakpress button this method is called
    @Override
    public void onBackPressed() {
        // When user press back button

        if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        } else {

            Toast.makeText(getBaseContext(), "Press again to exit",

            Toast.LENGTH_SHORT).show();
            super.onBackPressed();

        }

        mBackPressed = System.currentTimeMillis();



    } // end of onBackpressed method

    //#############################################################################################



//===========================================================================
//===========================================================================

    // Banner Ads
// Step 1 : Implementation Admob banner ads method in your Activity
    private void loadBanner(LinearLayout adViewContainer) {

        // Create a new ad view.
        AdView adView = new AdView(this);
        adView.setAdSize(getAdSize(adViewContainer));
        adView.setAdUnitId("ca-app-pub-3940256099942544/9214589741");

        // Replace ad container with new ad view.
        adViewContainer.removeAllViews();
        adViewContainer.addView(adView);

        // Start loading the ad in the background.
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private AdSize getAdSize(LinearLayout adViewContainer) {
        // Determine the screen width (less decorations) to use for the ad width.
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;

        float adWidthPixels = adViewContainer.getWidth();

        // If the ad hasn't been laid out, default to the full screen width.
        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }

        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }


    // Interstial Ads
    //========================================================================

    // Ad Loading Method
    private void loadFullScreenAds() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");

                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdClicked() {
                                Log.d(TAG, "Ad was clicked.");
                            }

                            @Override
                            public void onAdDismissedFullScreenContent() {
                                Log.d(TAG, "Ad dismissed fullscreen content.");
                                mInterstitialAd = null;
                                loadFullScreenAds();

                                Intent intent = new Intent(MainActivity.this, CompletedTasksActivity.class);
                                startActivity(intent);
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                Log.e(TAG, "Ad failed to show fullscreen content.");
                                mInterstitialAd = null;
                            }

                            @Override
                            public void onAdImpression() {
                                Log.d(TAG, "Ad recorded an impression.");
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                Log.d(TAG, "Ad showed fullscreen content.");
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, loadAdError.toString());
                        mInterstitialAd = null;
                    }
                });
    }

    //========================================================================


}

