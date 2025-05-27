package com.example.ocr.model;

public class MedicamentMaResult {
    private String data; // raw HTML or parsed info as string

    public MedicamentMaResult() {}

    public MedicamentMaResult(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
