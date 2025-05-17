package com.example.medscan.controller;

import com.example.medscan.model.Medication;
import com.example.medscan.service.MedicationService;

import com.example.medscan.exception.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    @Autowired
    private MedicationService medicationService;

    // Get all medications
    @GetMapping
    public List<Medication> getAllMedications() {
        return medicationService.getAllMedications();
    }

    // Get medication by ID
    @GetMapping("/{id}")
    public Optional<Medication> getMedicationById(@PathVariable Long id) {
        return medicationService.getMedicationById(id);
    }

    // Add a new medication
    @PostMapping
    public Medication addMedication(@RequestBody Medication medication) {
        return medicationService.saveMedication(medication);
    }

    // Update a medication by ID
    @PutMapping("/{id}")
    public ResponseEntity<Medication> updateMedication(@PathVariable Long id, @RequestBody Medication medicationDetails) {
        Medication medication = medicationService.getMedicationById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication not found with id " + id));
        
        medication.setName(medicationDetails.getName());
        medication.setSideEffects(medicationDetails.getSideEffects());
        medication.setInteractions(medicationDetails.getInteractions());
        
        final Medication updatedMedication = medicationService.saveMedication(medication);
        return ResponseEntity.ok(updatedMedication);
    }
    

    // Delete a medication by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedication(@PathVariable Long id) {
        // Check if the medication exists
        Medication medication = medicationService.getMedicationById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication not found with id " + id));

        // Delete the medication
        medicationService.deleteMedication(id);
        return ResponseEntity.noContent().build();
    }
}
