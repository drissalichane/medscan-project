package com.example.ocr;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ocr.adapter.ReminderAdapter;
import com.example.ocr.model.MedicationReminder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import android.Manifest;
import android.content.pm.PackageManager;

// MedicationRemindersActivity.java
public class MedicationRemindersActivity extends AppCompatActivity implements ReminderAdapter.OnReminderClickListener {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private RecyclerView recyclerView;
    private ReminderAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<MedicationReminder> reminders;
    private ReminderNotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_reminders);

        // Request notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST_CODE);
            }
        }

        // Initialize notification manager
        notificationManager = new ReminderNotificationManager(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Medication Reminders");

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        reminders = new ArrayList<>();
        adapter = new ReminderAdapter(reminders, this);
        recyclerView.setAdapter(adapter);

        // Setup FAB
        FloatingActionButton fab = findViewById(R.id.fabAddReminder);
        fab.setOnClickListener(v -> showAddReminderDialog());

        // Load reminders
        loadReminders();
    }

    private void loadReminders() {
        reminders.clear();
        android.database.Cursor cursor = dbHelper.getAllReminders();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                MedicationReminder reminder = new MedicationReminder();
                reminder.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REMINDER_ID)));
                reminder.setMedicationName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REMINDER_MEDICATION_NAME)));
                reminder.setDosage(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REMINDER_DOSAGE)));
                reminder.setFrequency(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REMINDER_FREQUENCY)));
                
                // Parse reminder times from JSON
                String timesJson = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REMINDER_TIMES));
                Type listType = new TypeToken<List<String>>() {}.getType();
                List<String> times = new Gson().fromJson(timesJson, listType);
                reminder.setReminderTimes(times);
                
                reminder.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REMINDER_IS_ACTIVE)) == 1);
                reminder.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REMINDER_NOTES)));
                reminder.setLastTaken(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REMINDER_LAST_TAKEN)));
                
                reminders.add(reminder);
            }
            cursor.close();
        }
        adapter.updateReminders(reminders);
    }

    private void showAddReminderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_reminder, null);
        builder.setView(view);

        TextInputEditText etMedicationName = view.findViewById(R.id.etMedicationName);
        TextInputEditText etDosage = view.findViewById(R.id.etDosage);
        TextInputEditText etNotes = view.findViewById(R.id.etNotes);
        LinearLayout reminderTimesContainer = view.findViewById(R.id.reminderTimesContainer);
        Button btnAddTime = view.findViewById(R.id.btnAddTime);

        // Day selection checkboxes
        CheckBox cbMonday = view.findViewById(R.id.cbMonday);
        CheckBox cbTuesday = view.findViewById(R.id.cbTuesday);
        CheckBox cbWednesday = view.findViewById(R.id.cbWednesday);
        CheckBox cbThursday = view.findViewById(R.id.cbThursday);
        CheckBox cbFriday = view.findViewById(R.id.cbFriday);
        CheckBox cbSaturday = view.findViewById(R.id.cbSaturday);
        CheckBox cbSunday = view.findViewById(R.id.cbSunday);

        List<String> reminderTimes = new ArrayList<>();

        btnAddTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (TimePicker view1, int hourOfDay, int minute) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute);
                    reminderTimes.add(time);
                    updateReminderTimesUI(reminderTimesContainer, reminderTimes);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            );
            timePickerDialog.show();
        });

        builder.setPositiveButton("Add", (dialog, which) -> {
            String medicationName = etMedicationName.getText().toString().trim();
            String dosage = etDosage.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            if (medicationName.isEmpty()) {
                Toast.makeText(this, "Please enter medication name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (reminderTimes.isEmpty()) {
                Toast.makeText(this, "Please add at least one reminder time", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get selected days
            List<String> selectedDays = new ArrayList<>();
            if (cbMonday.isChecked()) selectedDays.add("Monday");
            if (cbTuesday.isChecked()) selectedDays.add("Tuesday");
            if (cbWednesday.isChecked()) selectedDays.add("Wednesday");
            if (cbThursday.isChecked()) selectedDays.add("Thursday");
            if (cbFriday.isChecked()) selectedDays.add("Friday");
            if (cbSaturday.isChecked()) selectedDays.add("Saturday");
            if (cbSunday.isChecked()) selectedDays.add("Sunday");

            if (selectedDays.isEmpty()) {
                Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create frequency string from selected days
            String frequency = String.join(", ", selectedDays);

            // Save to database
            long id = dbHelper.insertReminder(medicationName, dosage, frequency, reminderTimes, notes);
            Log.d("MedReminder", "Reminder added: ID=" + id + ", Medication=" + medicationName + ", Times=" + reminderTimes + ", Frequency=" + frequency);
            if (id != -1) {
                // Schedule the reminder
                notificationManager.scheduleReminder((int)id, medicationName, dosage, reminderTimes, frequency);
                Log.d("MedReminder", "Scheduled notifications for reminder ID=" + id + ", Times=" + reminderTimes);
                loadReminders();
                Toast.makeText(this, "Reminder added successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to add reminder", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateReminderTimesUI(LinearLayout container, List<String> times) {
        container.removeAllViews();
        for (String time : times) {
            TextView textView = new TextView(this);
            textView.setText(time);
            textView.setPadding(0, 8, 0, 8);
            container.addView(textView);
        }
    }



    @Override
    public void onDeleteClick(MedicationReminder reminder) {
        new AlertDialog.Builder(this)
            .setTitle("Delete Reminder")
            .setMessage("Are you sure you want to delete this reminder?")
            .setPositiveButton("Delete", (dialog, which) -> {
                // Cancel the alarm
                notificationManager.cancelReminder(reminder.getId());
                
                int result = dbHelper.deleteReminder(reminder.getId());
                if (result > 0) {
                    loadReminders();
                    Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to delete reminder", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onActiveChanged(MedicationReminder reminder, boolean isActive) {
        int result = dbHelper.updateReminderStatus(reminder.getId(), isActive);
        if (result > 0) {
            reminder.setActive(isActive);
            if (isActive) {
                // Reschedule the reminder
                notificationManager.scheduleReminder(
                    reminder.getId(),
                    reminder.getMedicationName(),
                    reminder.getDosage(),
                    reminder.getReminderTimes(),
                    reminder.getFrequency()
                );
            } else {
                // Cancel the reminder
                notificationManager.cancelReminder(reminder.getId());
            }
            Toast.makeText(this, isActive ? "Reminder activated" : "Reminder deactivated", 
                         Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to update reminder status", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}