package com.example.ocr.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ocr.R;
import com.example.ocr.model.BackendMedicationResult;

import java.util.ArrayList;
import java.util.List;

public class BackendMedicationAdapter extends RecyclerView.Adapter<BackendMedicationAdapter.MedicationViewHolder> implements Filterable {
    private List<BackendMedicationResult> medications;
    private List<BackendMedicationResult> medicationsFull;

    public BackendMedicationAdapter(List<BackendMedicationResult> medications) {
        this.medications = medications;
        this.medicationsFull = new ArrayList<>(medications);
    }

    public void updateMedications(List<BackendMedicationResult> medications) {
        this.medications = medications;
        this.medicationsFull = new ArrayList<>(medications);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MedicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medication, parent, false);
        return new MedicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicationViewHolder holder, int position) {
        BackendMedicationResult medication = medications.get(position);
        holder.bind(medication);
    }

    @Override
    public int getItemCount() {
        return medications.size();
    }

    @Override
    public Filter getFilter() {
        return medicationFilter;
    }

    private Filter medicationFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<BackendMedicationResult> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(medicationsFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (BackendMedicationResult item : medicationsFull) {
                    if (item.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            medications.clear();
            medications.addAll((List<BackendMedicationResult>) results.values);
            notifyDataSetChanged();
        }
    };

    static class MedicationViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textPurpose;
        private final TextView textUsage;

        public MedicationViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textMedicationName);
            textPurpose = itemView.findViewById(R.id.textPurpose);
            textUsage = itemView.findViewById(R.id.textUsage);
        }

        public void bind(BackendMedicationResult medication) {
            textName.setText(medication.getName());
            
            if (medication.getSideEffects() != null && !medication.getSideEffects().isEmpty()) {
                textPurpose.setText("Side Effects: " + medication.getSideEffects());
                textPurpose.setVisibility(View.VISIBLE);
            } else {
                textPurpose.setVisibility(View.GONE);
            }

            if (medication.getInteractions() != null && !medication.getInteractions().isEmpty()) {
                textUsage.setText("Interactions: " + medication.getInteractions());
                textUsage.setVisibility(View.VISIBLE);
            } else {
                textUsage.setVisibility(View.GONE);
            }
        }
    }
} 