package com.example.medscan.service;

import com.example.medscan.exception.ResourceNotFoundException;
import com.example.medscan.model.Medication;
import com.example.medscan.repository.MedicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MedicationService {
    @Autowired
    private MedicationRepository medicationRepository;

    public List<Medication> getAllMedications() {
        return medicationRepository.findAll();
    }

    public Optional<Medication> getMedicationById(Long id) {
        return medicationRepository.findById(id);
    }

    public Medication saveMedication(Medication medication) {
        return medicationRepository.save(medication);
    }

    public void deleteMedication(Long id) {
        medicationRepository.deleteById(id);
    }
    public Medication updateMedication(Long id, Medication medicationDetails) {
    Medication medication = medicationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Medication not found with id " + id));
    
    medication.setName(medicationDetails.getName());
    medication.setSideEffects(medicationDetails.getSideEffects());
    medication.setInteractions(medicationDetails.getInteractions());
    
    return medicationRepository.save(medication);
}

}
