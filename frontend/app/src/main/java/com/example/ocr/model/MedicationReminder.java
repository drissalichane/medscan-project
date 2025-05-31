package com.example.ocr.model;

import java.util.List;

public class MedicationReminder {
    private int id;
    private String medicationName;
    private String dosage;
    private String frequency;
    private List<String> reminderTimes;
    private boolean isActive;
    private String notes;
    private String lastTaken;

    public MedicationReminder() {
    }


    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public List<String> getReminderTimes() {
        return reminderTimes;
    }

    public void setReminderTimes(List<String> reminderTimes) {
        this.reminderTimes = reminderTimes;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLastTaken() {
        return lastTaken;
    }

    public void setLastTaken(String lastTaken) {
        this.lastTaken = lastTaken;
    }
}