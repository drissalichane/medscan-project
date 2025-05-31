package com.example.ocr;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import com.example.ocr.model.MedicationResult;
import com.google.gson.Gson;

import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "medications.db";
    private static final int DATABASE_VERSION = 3;

    public static final String TABLE_NAME = "medications";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PURPOSE = "purpose";
    public static final String COLUMN_USAGE = "usage";
    public static final String COLUMN_WARNINGS = "warnings";
    public static final String COLUMN_PRECAUTIONS = "precautions";
    public static final String COLUMN_ADVERSE_REACTIONS = "adverse_reactions";
    public static final String COLUMN_OVERDOSAGE = "overdosage";
    public static final String COLUMN_DO_NOT_USE = "do_not_use";
    public static final String COLUMN_STOP_USE = "stop_use";
    public static final String COLUMN_WHEN_USE = "when_use";
    public static final String COLUMN_ASK_DOCTOR = "ask_doctor";
    public static final String COLUMN_ASK_DOCTOR_PHARMACIST = "ask_doctor_pharmacist";

    // New reminders table constants
    public static final String TABLE_REMINDERS = "reminders";
    public static final String COLUMN_REMINDER_ID = "id";
    public static final String COLUMN_REMINDER_MEDICATION_NAME = "medication_name";
    public static final String COLUMN_REMINDER_DOSAGE = "dosage";
    public static final String COLUMN_REMINDER_FREQUENCY = "frequency";
    public static final String COLUMN_REMINDER_TIMES = "reminder_times";
    public static final String COLUMN_REMINDER_IS_ACTIVE = "is_active";
    public static final String COLUMN_REMINDER_NOTES = "notes";
    public static final String COLUMN_REMINDER_LAST_TAKEN = "last_taken";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_PURPOSE + " TEXT, " +
                    COLUMN_USAGE + " TEXT, " +
                    COLUMN_WARNINGS + " TEXT, " +
                    COLUMN_PRECAUTIONS + " TEXT, " +
                    COLUMN_ADVERSE_REACTIONS + " TEXT, " +
                    COLUMN_OVERDOSAGE + " TEXT, " +
                    COLUMN_DO_NOT_USE + " TEXT, " +
                    COLUMN_STOP_USE + " TEXT, " +
                    COLUMN_WHEN_USE + " TEXT, " +
                    COLUMN_ASK_DOCTOR + " TEXT, " +
                    COLUMN_ASK_DOCTOR_PHARMACIST + " TEXT);";

    private static final String TABLE_REMINDERS_CREATE =
            "CREATE TABLE " + TABLE_REMINDERS + " (" +
                    COLUMN_REMINDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_REMINDER_MEDICATION_NAME + " TEXT NOT NULL, " +
                    COLUMN_REMINDER_DOSAGE + " TEXT, " +
                    COLUMN_REMINDER_FREQUENCY + " TEXT, " +
                    COLUMN_REMINDER_TIMES + " TEXT, " + // Store as JSON array of times
                    COLUMN_REMINDER_IS_ACTIVE + " INTEGER DEFAULT 1, " +
                    COLUMN_REMINDER_NOTES + " TEXT, " +
                    COLUMN_REMINDER_LAST_TAKEN + " TEXT);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        db.execSQL(TABLE_REMINDERS_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Create reminders table if upgrading from version < 3
            db.execSQL(TABLE_REMINDERS_CREATE);
        }
    }

    // Insert sample data
    // Removed dummy data function since we're using OpenFDA data

    // Insert medication info from OpenFDA
    public long insertMedication(SQLiteDatabase db, String name, MedicationResult result) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        
        // Store all fields from OpenFDA response
        values.put(COLUMN_PURPOSE, result.purpose != null ? String.join("; ", result.purpose) : "");
        values.put(COLUMN_USAGE, result.indications_and_usage != null ? String.join("; ", result.indications_and_usage) : "");
        values.put(COLUMN_WARNINGS, result.warnings != null ? String.join("; ", result.warnings) : "");
        values.put(COLUMN_PRECAUTIONS, result.precautions != null ? String.join("; ", result.precautions) : "");
        values.put(COLUMN_ADVERSE_REACTIONS, result.adverse_reactions != null ? String.join("; ", result.adverse_reactions) : "");
        values.put(COLUMN_OVERDOSAGE, result.overdosage != null ? String.join("; ", result.overdosage) : "");
        values.put(COLUMN_DO_NOT_USE, result.do_not_use != null ? String.join("; ", result.do_not_use) : "");
        values.put(COLUMN_STOP_USE, result.stop_use != null ? String.join("; ", result.stop_use) : "");
        values.put(COLUMN_WHEN_USE, result.when_use != null ? String.join("; ", result.when_use) : "");
        values.put(COLUMN_ASK_DOCTOR, result.ask_doctor != null ? String.join("; ", result.ask_doctor) : "");
        values.put(COLUMN_ASK_DOCTOR_PHARMACIST, result.ask_doctor_or_pharmacist != null ? String.join("; ", result.ask_doctor_or_pharmacist) : "");

        // Use `insertWithOnConflict` to avoid duplicates and return the insert ID
        return db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    // Get medication info from database
    public Cursor getMedicationInfo(String medicineName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_NAME + " = ?",
                new String[]{medicineName}
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            return cursor;
        }
        return null;
    }

    // Get all medications
    public Cursor getAllMedications() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    public void clearAllMedications() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
        db.close();
    }

    // Add new methods for reminders
    public long insertReminder(String medicationName, String dosage, String frequency, 
                             List<String> reminderTimes, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        values.put(COLUMN_REMINDER_MEDICATION_NAME, medicationName);
        values.put(COLUMN_REMINDER_DOSAGE, dosage);
        values.put(COLUMN_REMINDER_FREQUENCY, frequency);
        values.put(COLUMN_REMINDER_TIMES, new Gson().toJson(reminderTimes));
        values.put(COLUMN_REMINDER_IS_ACTIVE, 1);
        values.put(COLUMN_REMINDER_NOTES, notes);
        
        return db.insert(TABLE_REMINDERS, null, values);
    }

    public Cursor getAllReminders() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_REMINDERS, null, null, null, null, null, 
                       COLUMN_REMINDER_MEDICATION_NAME + " ASC");
    }

    public Cursor getActiveReminders() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_REMINDERS, null, 
                       COLUMN_REMINDER_IS_ACTIVE + " = ?", 
                       new String[]{"1"}, null, null, 
                       COLUMN_REMINDER_MEDICATION_NAME + " ASC");
    }

    public int updateReminderStatus(int reminderId, boolean isActive) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REMINDER_IS_ACTIVE, isActive ? 1 : 0);
        
        return db.update(TABLE_REMINDERS, values, 
                        COLUMN_REMINDER_ID + " = ?", 
                        new String[]{String.valueOf(reminderId)});
    }

    public int updateLastTaken(int reminderId, String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REMINDER_LAST_TAKEN, timestamp);
        
        return db.update(TABLE_REMINDERS, values, 
                        COLUMN_REMINDER_ID + " = ?", 
                        new String[]{String.valueOf(reminderId)});
    }

    public int deleteReminder(int reminderId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_REMINDERS, 
                        COLUMN_REMINDER_ID + " = ?", 
                        new String[]{String.valueOf(reminderId)});
    }
}
