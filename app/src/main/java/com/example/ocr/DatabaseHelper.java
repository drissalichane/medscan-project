package com.example.ocr;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "medications.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "medications";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_USES = "uses";
    public static final String COLUMN_SIDE_EFFECTS = "side_effects";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT UNIQUE, " +  // Ensures unique medication names
                    COLUMN_USES + " TEXT, " +
                    COLUMN_SIDE_EFFECTS + " TEXT);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        insertDummyData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Insert sample data
    private void insertDummyData(SQLiteDatabase db) {
        insertMedication(db, "Paracetamol", "Used for fever and pain relief", "Nausea, liver damage if overdosed");
        insertMedication(db, "Ibuprofen", "Pain relief, anti-inflammatory", "Stomach pain, dizziness");
        insertMedication(db, "Aspirin", "Pain relief, blood thinner", "Stomach ulcers, bleeding risk");
    }

    // Insert a new medication (Prevents duplicates)
    public void insertMedication(SQLiteDatabase db, String name, String uses, String sideEffects) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_USES, uses);
        values.put(COLUMN_SIDE_EFFECTS, sideEffects);

        // Use `insertWithOnConflict` to avoid duplicates
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    // Get medication info based on **partial match** (better OCR handling)
    public Cursor getMedicationInfo(String medicineName) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_NAME + " LIKE ?",
                new String[]{"%" + medicineName + "%"} // Partial matching
        );
    }
}
