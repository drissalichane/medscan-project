package com.example.ocr.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ocr.R;
import com.example.ocr.model.ScannedMedication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder> implements Filterable {
    private List<ScannedMedication> medications;
    private List<ScannedMedication> medicationsFull;

    public MedicationAdapter(List<ScannedMedication> medications) {
        this.medications = medications;
        this.medicationsFull = new ArrayList<>(medications);
    }

    public void updateMedications(List<ScannedMedication> medications) {
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
        ScannedMedication medication = medications.get(position);
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
            List<ScannedMedication> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(medicationsFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (ScannedMedication item : medicationsFull) {
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
            medications.addAll((List<ScannedMedication>) results.values);
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

        public void bind(ScannedMedication medication) {
            textName.setText(medication.getName());
            if (medication.getPurpose() != null && !medication.getPurpose().isEmpty()) {
                textPurpose.setText("Purpose: " + medication.getPurpose().split("; ")[0]);
                textPurpose.setVisibility(View.VISIBLE);
            } else {
                textPurpose.setVisibility(View.GONE);
            }
            if (medication.getUsage() != null && !medication.getUsage().isEmpty()) {
                textUsage.setText("Usage: " + medication.getUsage().split("; ")[0]);
                textUsage.setVisibility(View.VISIBLE);
            } else {
                textUsage.setVisibility(View.GONE);
            }

            // Show details dialog on click (Purpose, Usage, Warnings, Ask Doctor, Overdosage, Adverse Reactions)
            itemView.setOnClickListener(v -> {
                Context context = itemView.getContext();
                Map<String, String> details = new HashMap<>();
                if (medication.getPurpose() != null && !medication.getPurpose().isEmpty())
                    details.put("Purpose", medication.getPurpose());
                if (medication.getUsage() != null && !medication.getUsage().isEmpty())
                    details.put("Usage", medication.getUsage());
                if (medication.getWarnings() != null && !medication.getWarnings().isEmpty())
                    details.put("Warnings", medication.getWarnings());
                if (medication.getAskDoctor() != null && !medication.getAskDoctor().isEmpty())
                    details.put("Ask Doctor", medication.getAskDoctor());
                if (medication.getOverdosage() != null && !medication.getOverdosage().isEmpty())
                    details.put("Overdosage", medication.getOverdosage());
                if (medication.getAdverseReactions() != null && !medication.getAdverseReactions().isEmpty())
                    details.put("Adverse Reactions", medication.getAdverseReactions());

                if (details.isEmpty()) {
                    new AlertDialog.Builder(context)
                        .setTitle("No Details Available")
                        .setMessage("No additional information found for this medication.")
                        .setPositiveButton("OK", null)
                        .show();
                    return;
                }

                String[] keys = details.keySet().toArray(new String[0]);
                new AlertDialog.Builder(context)
                    .setTitle("Select Detail")
                    .setItems(keys, (dialog, which) -> {
                        String key = keys[which];
                        String value = details.get(key);
                        new AlertDialog.Builder(context)
                            .setTitle(key)
                            .setMessage(value)
                            .setPositiveButton("OK", null)
                            .show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
    }
} 