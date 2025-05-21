package com.example.ocr.model;

public class ScannedMedication {
    private long id;
    private String name;
    private String purpose;
    private String usage;
    private String warnings;
    private String precautions;
    private String adverseReactions;
    private String overdosage;
    private String doNotUse;
    private String stopUse;
    private String whenUse;
    private String askDoctor;
    private String askDoctorOrPharmacist;

    // Constructor
    public ScannedMedication() {}

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getWarnings() {
        return warnings;
    }

    public void setWarnings(String warnings) {
        this.warnings = warnings;
    }

    public String getPrecautions() {
        return precautions;
    }

    public void setPrecautions(String precautions) {
        this.precautions = precautions;
    }

    public String getAdverseReactions() {
        return adverseReactions;
    }

    public void setAdverseReactions(String adverseReactions) {
        this.adverseReactions = adverseReactions;
    }

    public String getOverdosage() {
        return overdosage;
    }

    public void setOverdosage(String overdosage) {
        this.overdosage = overdosage;
    }

    public String getDoNotUse() {
        return doNotUse;
    }

    public void setDoNotUse(String doNotUse) {
        this.doNotUse = doNotUse;
    }

    public String getStopUse() {
        return stopUse;
    }

    public void setStopUse(String stopUse) {
        this.stopUse = stopUse;
    }

    public String getWhenUse() {
        return whenUse;
    }

    public void setWhenUse(String whenUse) {
        this.whenUse = whenUse;
    }

    public String getAskDoctor() {
        return askDoctor;
    }

    public void setAskDoctor(String askDoctor) {
        this.askDoctor = askDoctor;
    }

    public String getAskDoctorOrPharmacist() {
        return askDoctorOrPharmacist;
    }

    public void setAskDoctorOrPharmacist(String askDoctorOrPharmacist) {
        this.askDoctorOrPharmacist = askDoctorOrPharmacist;
    }
} 