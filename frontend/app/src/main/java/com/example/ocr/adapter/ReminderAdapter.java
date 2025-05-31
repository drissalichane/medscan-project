package com.example.ocr.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ocr.R;
import com.example.ocr.model.MedicationReminder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ViewHolder> {
    private List<MedicationReminder> reminders;
    private OnReminderClickListener listener;

    public interface OnReminderClickListener {

        void onDeleteClick(MedicationReminder reminder);
        void onActiveChanged(MedicationReminder reminder, boolean isActive);
    }

    public ReminderAdapter(List<MedicationReminder> reminders, OnReminderClickListener listener) {
        this.reminders = reminders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicationReminder reminder = reminders.get(position);
        holder.bind(reminder);
    }

    @Override
    public int getItemCount() {
        return reminders != null ? reminders.size() : 0;
    }

    public void updateReminders(List<MedicationReminder> newReminders) {
        this.reminders = newReminders;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvMedicationName;
        private TextView tvDosage;
        private TextView tvFrequency;
        private TextView tvReminderTimes;
        private SwitchMaterial switchActive;

        private ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvMedicationName = itemView.findViewById(R.id.tvMedicationName);
            tvDosage = itemView.findViewById(R.id.tvDosage);
            tvFrequency = itemView.findViewById(R.id.tvFrequency);
            tvReminderTimes = itemView.findViewById(R.id.tvReminderTimes);
            switchActive = itemView.findViewById(R.id.switchActive);

            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(MedicationReminder reminder) {
            tvMedicationName.setText(reminder.getMedicationName());
            tvDosage.setText(reminder.getDosage());
            tvFrequency.setText(reminder.getFrequency());
            tvReminderTimes.setText(formatReminderTimes(reminder.getReminderTimes()));
            switchActive.setChecked(reminder.isActive());

            switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onActiveChanged(reminder, isChecked);
                }
            });



            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(reminder);
                }
            });
        }

        private String formatReminderTimes(List<String> times) {
            if (times == null || times.isEmpty()) {
                return "No reminder times set";
            }
            return "Reminder times: " + String.join(", ", times);
        }
    }
}