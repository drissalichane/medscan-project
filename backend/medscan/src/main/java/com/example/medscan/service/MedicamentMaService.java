package com.example.medscan.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@Service
public class MedicamentMaService {

    private final RestTemplate restTemplate;

    public MedicamentMaService() {
        this.restTemplate = new RestTemplate();
    }

    public String fetchMedicationInfoByName(String name) {
        try {
            String url = "https://medicament.ma/?choice=specialite&keyword=starts&s=" + name;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                String html = response.getBody();
                // TODO: parse the HTML to extract medication info
                // For now, return raw HTML or a placeholder
                return html != null ? html : "No data found";
            } else {
                return "No data found";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching data";
        }
    }
}
