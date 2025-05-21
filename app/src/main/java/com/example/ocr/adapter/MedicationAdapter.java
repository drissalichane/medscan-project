package com.example.ocr.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ocr.R;
import com.example.ocr.model.BackendMedicationResult;

import java.util.List;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder> {
    private List<BackendMedicationResult> medications;

    public MedicationAdapter(List<BackendMedicationResult> medications) {
        this.medications = medications;
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
        holder.nameTextView.setText(medication.getName());
        holder.sideEffectsTextView.setText(medication.getSideEffects());
        holder.interactionsTextView.setText(medication.getInteractions());
    }

    @Override
    public int getItemCount() {
        return medications.size();
    }

    public void updateMedications(List<BackendMedicationResult> newMedications) {
        this.medications = newMedications;
        notifyDataSetChanged();
    }

    static class MedicationViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView sideEffectsTextView;
        TextView interactionsTextView;

        MedicationViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.textViewMedicationName);
            sideEffectsTextView = itemView.findViewById(R.id.textViewSideEffects);
            interactionsTextView = itemView.findViewById(R.id.textViewInteractions);
        }
    }
} 