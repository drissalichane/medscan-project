package com.example.medscan.model;

import jakarta.persistence.*;

@Entity
@Table(name = "medications")
public class Medication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String sideEffects;
    private String interactions;

    // Constructors
    public Medication() {}

    public Medication(String name, String sideEffects, String interactions) {
        this.name = name;
        this.sideEffects = sideEffects;
        this.interactions = interactions;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSideEffects() { return sideEffects; }
    public void setSideEffects(String sideEffects) { this.sideEffects = sideEffects; }

    public String getInteractions() { return interactions; }
    public void setInteractions(String interactions) { this.interactions = interactions; }
}
