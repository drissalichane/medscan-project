package com.example.medscan.controller;

import com.example.medscan.service.MedicamentMaService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MedicamentMaController {

    @Autowired
    private MedicamentMaService medicamentMaService;

    @GetMapping("/api/medicamentma")
    public String getMedicationInfo(@RequestParam String name) {
        return medicamentMaService.fetchMedicationInfoByName(name);
    }
}
