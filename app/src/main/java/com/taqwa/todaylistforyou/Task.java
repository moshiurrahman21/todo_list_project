package com.taqwa.todaylistforyou;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.List;
public class Task implements Parcelable {
    private int id;
    private String title;
    private String category;
    private List<String> descriptions;
    private String date;
    private String time;
    private boolean isCompleted;
    private boolean isAlarmSet;
    // Constructor, getters, and setters

    public Task(int id, String title, String category, List<String> descriptions, String date, String time) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.descriptions = descriptions;
        this.date = date;
        this.time = time;
        this.isCompleted = false;
        this.isAlarmSet = false;
    }


    protected Task(Parcel in) {
        id = in.readInt();
        title = in.readString();
        category = in.readString();
        descriptions = in.createStringArrayList();
        date = in.readString();
        time = in.readString();
        isCompleted = in.readByte() != 0;
        isAlarmSet = in.readByte() != 0;
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }

    public List<String> getDescriptions() {
        return descriptions;
    }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public boolean isCompleted() { return isCompleted; }
    public boolean isAlarmSet() { return isAlarmSet; }

    public void setTitle(String title) { this.title = title; }
    public void setCategory(String category) { this.category = category; }
    public void setDescriptions(List<String> descriptions) { this.descriptions = descriptions; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public void setAlarmSet(boolean alarmSet) { isAlarmSet = alarmSet; }


    public static final Creator<Task> CREATOR = new Creator<Task>() {
        @Override
        public Task createFromParcel(Parcel in) {
            return new Task(in);
        }

        @Override
        public Task[] newArray(int size) {
            return new Task[size];
        }
    };
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(category);
        dest.writeStringList(descriptions);
        dest.writeString(date);
        dest.writeString(time);
        dest.writeByte((byte) (isCompleted ? 1 : 0));
        dest.writeByte((byte) (isAlarmSet ? 1 : 0));
    }

    public long getTimeInMillis() {
        // টাস্কের তারিখ এবং সময় থেকে টাইম ইন মিলিসেকেন্ড বের করুন
        String[] dateParts = this.date.split("/");
        String[] timeParts = this.time.split(" ");

        Calendar calendar = Calendar.getInstance();
        int day = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]) - 1; // মাস 0-ভিত্তিক
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

        return calendar.getTimeInMillis(); // টাইম ইন মিলিসেকেন্ড রিটার্ন করুন
    }

}
