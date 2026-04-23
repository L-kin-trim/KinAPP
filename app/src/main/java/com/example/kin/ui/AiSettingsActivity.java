package com.example.kin.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kin.R;
import com.example.kin.data.AiConfigStore;
import com.example.kin.model.AiConfig;
import com.example.kin.model.AiProviderPreset;
import com.example.kin.ui.common.KinUi;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class AiSettingsActivity extends AppCompatActivity {
    private final List<AiProviderPreset> presets = AiProviderPreset.defaults();

    private AiConfigStore configStore;
    private Spinner providerSpinner;
    private TextView providerNoteView;
    private TextView statusView;
    private ProgressBar progressBar;
    private TextInputEditText baseUrlEdit;
    private TextInputEditText apiKeyEdit;
    private TextInputEditText modelEdit;
    private TextInputEditText systemPromptEdit;
    private boolean suppressPresetAutoFill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configStore = new AiConfigStore(this);
        setTitle("AI Model Settings");
        setContentView(buildContentView());
        bindConfig(configStore.load());
    }

    private View buildContentView() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(getColor(KinUi.isNight(this) ? R.color.kin_dark_bg : R.color.kin_light_bg));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        root.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout content = KinUi.sectionContainer(this, 18);
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        MaterialCardView card = KinUi.card(this);
        LinearLayout body = KinUi.sectionContainer(this, 18);
        body.addView(KinUi.text(this, "AI Recommendation Config", 22, true));
        TextView subtitle = KinUi.muted(this, "Recommendations use local library first, then model general knowledge.", 14);
        KinUi.margins(subtitle, this, 0, 8, 0, 0);
        body.addView(subtitle);

        TextView providerLabel = KinUi.muted(this, "Provider Preset", 13);
        KinUi.margins(providerLabel, this, 0, 14, 0, 0);
        body.addView(providerLabel);

        providerSpinner = new Spinner(this);
        List<String> labels = new ArrayList<>();
        for (AiProviderPreset preset : presets) {
            labels.add(preset.label);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels);
        providerSpinner.setAdapter(adapter);
        providerSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                AiProviderPreset preset = presets.get(position);
                providerNoteView.setText(preset.note);
                if (!suppressPresetAutoFill) {
                    baseUrlEdit.setText(preset.baseUrl);
                    modelEdit.setText(preset.model);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        KinUi.margins(providerSpinner, this, 0, 6, 0, 0);
        body.addView(providerSpinner);

        providerNoteView = KinUi.muted(this, "", 12);
        KinUi.margins(providerNoteView, this, 0, 8, 0, 0);
        body.addView(providerNoteView);

        baseUrlEdit = addInput(body, "Base URL");
        apiKeyEdit = addInput(body, "API Key");
        modelEdit = addInput(body, "Model");
        systemPromptEdit = addInput(body, "System Prompt (optional)", true);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton applyPresetButton = KinUi.outlinedButton(this, "Apply Preset");
        applyPresetButton.setOnClickListener(v -> applySelectedPreset());
        MaterialButton saveButton = KinUi.filledButton(this, "Save Config");
        saveButton.setOnClickListener(v -> saveConfig());
        actions.addView(applyPresetButton);
        actions.addView(saveButton);
        KinUi.margins(saveButton, this, 10, 0, 0, 0);
        KinUi.margins(actions, this, 0, 14, 0, 0);
        body.addView(actions);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        KinUi.margins(progressBar, this, 0, 12, 0, 0);
        body.addView(progressBar);

        statusView = KinUi.muted(this, "", 13);
        statusView.setVisibility(View.GONE);
        KinUi.margins(statusView, this, 0, 8, 0, 0);
        body.addView(statusView);
        card.addView(body);

        content.addView(card);

        TextView hint = KinUi.muted(this,
                "Note: Claude preset uses an OpenAI-compatible route. Replace base URL if needed.",
                12);
        hint.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(hint);
        return root;
    }

    private TextInputEditText addInput(LinearLayout parent, String hint) {
        return addInput(parent, hint, false);
    }

    private TextInputEditText addInput(LinearLayout parent, String hint, boolean multiline) {
        TextInputLayout layout = KinUi.inputLayout(this, hint, multiline);
        KinUi.margins(layout, this, 0, 10, 0, 0);
        parent.addView(layout);
        return KinUi.edit(layout);
    }

    private void bindConfig(AiConfig config) {
        int selectedIndex = 0;
        for (int i = 0; i < presets.size(); i++) {
            if (presets.get(i).id.equals(config.providerId)) {
                selectedIndex = i;
                break;
            }
        }
        suppressPresetAutoFill = true;
        providerSpinner.setSelection(selectedIndex, false);
        suppressPresetAutoFill = false;
        providerNoteView.setText(presets.get(selectedIndex).note);

        baseUrlEdit.setText(config.baseUrl);
        apiKeyEdit.setText(config.apiKey);
        modelEdit.setText(config.model);
        systemPromptEdit.setText(config.systemPrompt);

        if (isEmpty(config.baseUrl) || isEmpty(config.model)) {
            applySelectedPreset();
        }
    }

    private void applySelectedPreset() {
        AiProviderPreset preset = presets.get(providerSpinner.getSelectedItemPosition());
        baseUrlEdit.setText(preset.baseUrl);
        modelEdit.setText(preset.model);
        providerNoteView.setText(preset.note);
        status("Preset applied: " + preset.label);
    }

    private void saveConfig() {
        progress(true);
        AiConfig config = new AiConfig();
        AiProviderPreset preset = presets.get(providerSpinner.getSelectedItemPosition());
        config.providerId = preset.id;
        config.baseUrl = text(baseUrlEdit);
        config.apiKey = text(apiKeyEdit);
        config.model = text(modelEdit);
        config.systemPrompt = text(systemPromptEdit);
        if (!config.isValid()) {
            progress(false);
            status("Please fill Base URL, API Key and Model.");
            return;
        }
        configStore.save(config);
        progress(false);
        status("AI config saved.");
    }

    private void progress(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void status(String message) {
        statusView.setText(message);
        statusView.setVisibility(isEmpty(message) ? View.GONE : View.VISIBLE);
    }

    private String text(TextInputEditText editText) {
        return String.valueOf(editText.getText()).trim();
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
