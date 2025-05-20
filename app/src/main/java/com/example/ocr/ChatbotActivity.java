package com.example.ocr;

import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatbotActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private Button resetButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;

    // Chip Groups
    private ChipGroup painTypeChipGroup;
    private ChipGroup durationChipGroup;
    private ChipGroup intensityChipGroup;
    private ChipGroup triggerChipGroup;
    private ChipGroup ageGroupChipGroup;
    private ChipGroup allergyChipGroup;

    // ScrollViews
    private HorizontalScrollView painTypeScroll;
    private HorizontalScrollView durationScroll;
    private HorizontalScrollView intensityScroll;
    private HorizontalScrollView triggerScroll;
    private HorizontalScrollView ageGroupScroll;
    private HorizontalScrollView allergyScroll;

    // Selected values
    private String selectedPainType = "";
    private String selectedDuration = "";
    private String selectedIntensity = "";
    private String selectedTrigger = "";
    private String selectedAgeGroup = "";
    private String allergyInfo = "";

    // Pain types and symptoms
    private final Map<String, String[]> painTypeToSymptoms = new HashMap<String, String[]>() {{
        put("🤕 Headache", new String[]{"Mild", "Severe", "Throbbing", "With fever", "With nausea", "Sudden onset"});
        put("🤢 Stomachache", new String[]{"Cramping", "Sharp", "With diarrhea", "With vomiting", "With fever"});
        put("👁️ Eye pain", new String[]{"Redness", "Blurriness", "Itching", "Discharge", "Sudden vision loss"});
        put("🦴 Bone pain", new String[]{"Fracture", "Mild pain", "Swelling", "Bruising", "After injury"});
        put("❤️ Chest pain", new String[]{"Sharp", "Dull", "With shortness of breath", "With sweating", "Radiating to arm"});
        put("🤧 Throat pain", new String[]{"Sore", "Scratchy", "With cough", "With fever", "Difficulty swallowing"});
        put("🦷 Toothache", new String[]{"Sharp", "Dull", "With swelling", "With fever", "After eating/drinking"});
        put("👂 Ear pain", new String[]{"Sharp", "Dull", "With discharge", "With hearing loss", "With fever"});
        put("🦿 Muscle pain", new String[]{"Soreness", "Cramping", "After exercise", "Sudden", "With weakness"});
        put("🦵 Leg pain", new String[]{"Cramps", "Swelling", "Numbness", "After injury", "With redness"});
        put("🦶 Foot pain", new String[]{"Swelling", "Numbness", "After walking", "With redness", "With fever"});
        put("🤲 Hand pain", new String[]{"Swelling", "Numbness", "After injury", "With redness", "With fever"});
        put("🦻 Earache", new String[]{"Sharp", "Dull", "With discharge", "With hearing loss", "With fever"});
        put("🦶 Ankle pain", new String[]{"Swelling", "Bruising", "After injury", "With redness", "With fever"});
        put("🦵 Knee pain", new String[]{"Swelling", "Stiffness", "After injury", "With redness", "With fever"});
        put("🦾 Arm pain", new String[]{"Swelling", "Numbness", "After injury", "With redness", "With fever"});
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Doctor Chatbot");

        initializeViews();
        setupRecyclerView();
        setupChipGroups();
        setupResetButton();

        // Start the conversation
        resetAll();
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        resetButton = findViewById(R.id.buttonResetAll);

        // Initialize ChipGroups
        painTypeChipGroup = findViewById(R.id.painTypeChipGroup);
        durationChipGroup = findViewById(R.id.durationChipGroup);
        intensityChipGroup = findViewById(R.id.intensityChipGroup);
        triggerChipGroup = findViewById(R.id.triggerChipGroup);
        ageGroupChipGroup = findViewById(R.id.ageGroupChipGroup);
        allergyChipGroup = findViewById(R.id.allergyChipGroup);

        // Initialize ScrollViews
        painTypeScroll = findViewById(R.id.painTypeScroll);
        durationScroll = findViewById(R.id.durationScroll);
        intensityScroll = findViewById(R.id.intensityScroll);
        triggerScroll = findViewById(R.id.triggerScroll);
        ageGroupScroll = findViewById(R.id.ageGroupScroll);
        allergyScroll = findViewById(R.id.allergyScroll);
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        chatAdapter = new ChatAdapter(messages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupChipGroups() {
        // Pain Type Chips
        String[] painTypes = {
            "🤕 Headache", "🦷 Toothache", "👄 Mouth pain",
            "🫁 Chest pain", "🫃 Stomach pain", "🦿 Muscle pain",
            "🦴 Joint pain", "👁️ Eye pain", "👂 Ear pain"
        };
        for (String type : painTypes) {
            addChip(painTypeChipGroup, type);
        }

        // Duration Chips
        String[] durations = {
            "Less than 1 hour", "Few hours", "1–2 days",
            "More than 3 days", "I don't remember"
        };
        for (String duration : durations) {
            addChip(durationChipGroup, duration);
        }

        // Intensity Chips
        String[] intensities = {"Mild", "Moderate", "Severe"};
        for (String intensity : intensities) {
            addChip(intensityChipGroup, intensity);
        }

        // Trigger Chips
        String[] triggers = {
            "After eating", "After exercise", "While resting",
            "During movement", "After waking up", "At night",
            "Weather changes", "Stress related", "No specific trigger"
        };
        for (String trigger : triggers) {
            addChip(triggerChipGroup, trigger);
        }

        // Age Group Chips
        String[] ageGroups = {"Child", "Teen", "Adult", "Elderly"};
        for (String age : ageGroups) {
            addChip(ageGroupChipGroup, age);
        }

        // Allergy Chips
        String[] allergies = {"Yes", "No"};
        for (String allergy : allergies) {
            addChip(allergyChipGroup, allergy);
        }

        setupChipListeners();
    }

    private void setupChipListeners() {
        painTypeChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID) {
                Chip chip = group.findViewById(checkedId);
                selectedPainType = chip.getText().toString();
                addUserMessage("Pain Type: " + selectedPainType);
                addBotMessage("How long have you been experiencing this pain?");
                showNextStep(durationScroll);
            }
        });

        durationChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID) {
                Chip chip = group.findViewById(checkedId);
                selectedDuration = chip.getText().toString();
                addUserMessage("Duration: " + selectedDuration);
                addBotMessage("How would you rate the intensity of the pain?");
                showNextStep(intensityScroll);
            }
        });

        intensityChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID) {
                Chip chip = group.findViewById(checkedId);
                selectedIntensity = chip.getText().toString();
                addUserMessage("Intensity: " + selectedIntensity);
                addBotMessage("Is there anything that triggers or affects this pain?");
                showNextStep(triggerScroll);
            }
        });

        triggerChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID) {
                Chip chip = group.findViewById(checkedId);
                selectedTrigger = chip.getText().toString();
                addUserMessage("Trigger: " + selectedTrigger);
                addBotMessage("What is your age group?");
                showNextStep(ageGroupScroll);
            }
        });

        ageGroupChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID) {
                Chip chip = group.findViewById(checkedId);
                selectedAgeGroup = chip.getText().toString();
                addUserMessage("Age Group: " + selectedAgeGroup);
                addBotMessage("Do you have any allergies?");
                showNextStep(allergyScroll);
            }
        });

        allergyChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID) {
                Chip chip = group.findViewById(checkedId);
                String selection = chip.getText().toString();
                if ("Yes".equals(selection)) {
                    showAllergyDialog();
                } else {
                    allergyInfo = "No allergies";
                    addUserMessage("Allergies: " + allergyInfo);
                    allergyScroll.setVisibility(View.GONE);
                    askGeminiForAdvice();
                }
            }
        });
    }

    private void showAllergyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Allergy Information");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            allergyInfo = input.getText().toString();
            if (allergyInfo.isEmpty()) {
                allergyInfo = "Not specified";
            }
            addUserMessage("Allergies: " + allergyInfo);
            askGeminiForAdvice();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            allergyChipGroup.clearCheck();
            dialog.cancel();
        });

        builder.show();
    }

    private void addChip(ChipGroup chipGroup, String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCheckable(true);
        chip.setClickable(true);
        chipGroup.addView(chip);
    }

    private void showNextStep(View nextStep) {
        // Hide all steps first
        painTypeScroll.setVisibility(View.GONE);
        durationScroll.setVisibility(View.GONE);
        intensityScroll.setVisibility(View.GONE);
        triggerScroll.setVisibility(View.GONE);
        ageGroupScroll.setVisibility(View.GONE);
        allergyScroll.setVisibility(View.GONE);

        // Show only the next step
        nextStep.setVisibility(View.VISIBLE);
    }

    private void resetAll() {
        // Clear selections
        selectedPainType = "";
        selectedDuration = "";
        selectedIntensity = "";
        selectedTrigger = "";
        selectedAgeGroup = "";
        allergyInfo = "";

        // Clear chip selections
        painTypeChipGroup.clearCheck();
        durationChipGroup.clearCheck();
        intensityChipGroup.clearCheck();
        triggerChipGroup.clearCheck();
        ageGroupChipGroup.clearCheck();
        allergyChipGroup.clearCheck();

        // Hide all steps except pain type
        durationScroll.setVisibility(View.GONE);
        intensityScroll.setVisibility(View.GONE);
        triggerScroll.setVisibility(View.GONE);
        ageGroupScroll.setVisibility(View.GONE);
        allergyScroll.setVisibility(View.GONE);

        // Clear messages
        messages.clear();
        chatAdapter.notifyDataSetChanged();

        // Show welcome message
        addBotMessage("Hello! I'm your medical assistant. What type of pain are you experiencing?");
        painTypeScroll.setVisibility(View.VISIBLE);
    }

    private void setupResetButton() {
        resetButton.setOnClickListener(v -> resetAll());
    }

    private void addUserMessage(String message) {
        messages.add(new ChatMessage(message, true));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
    }

    private void addBotMessage(String message) {
        messages.add(new ChatMessage(message, false));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.smoothScrollToPosition(messages.size() - 1);
    }

    private void askGeminiForAdvice() {
        addBotMessage("Analyzing your symptoms...");
        
        GeminiHelper geminiHelper = new GeminiHelper();
        String userInfo = String.format(
            "A user reports the following symptoms:\n" +
            "- Pain type: %s\n" +
            "- Duration: %s\n" +
            "- Intensity: %s\n" +
            "- Trigger: %s\n" +
            "- Age group: %s\n" +
            "- Allergies: %s",
            selectedPainType,
            selectedDuration,
            selectedIntensity,
            selectedTrigger,
            selectedAgeGroup,
            allergyInfo
        );

        geminiHelper.getMedicalAdvice(userInfo, new GeminiHelper.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                addBotMessage(response);
                addBotMessage("Would you like to start over? Click the 'Reset All' button.");
            }

            @Override
            public void onFailure(Exception e) {
                addBotMessage("Sorry, I couldn't process your request: " + e.getMessage());
                addBotMessage("Please try again by clicking the 'Reset All' button.");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 