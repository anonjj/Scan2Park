package com.example.parkeasy.util;

import android.app.Activity;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.example.parkeasy.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VehicleManagerDialog {

    public interface Listener {
        void onVehiclesUpdated();
    }

    private VehicleManagerDialog() {
    }

    public static void show(Activity activity, Listener listener, String prefillPlate) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_vehicle_manager, null);

        TextView tvPrimary = dialogView.findViewById(R.id.tvPrimaryVehicle);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.inputVehicle);
        TextInputEditText etVehicleInput = dialogView.findViewById(R.id.etVehicleInput);
        ListView listView = dialogView.findViewById(R.id.listVehicles);
        MaterialButton btnAdd = dialogView.findViewById(R.id.btnAddVehicle);
        MaterialButton btnRemove = dialogView.findViewById(R.id.btnRemoveVehicle);
        MaterialButton btnDone = dialogView.findViewById(R.id.btnDoneVehicles);

        List<String> vehicles = new ArrayList<>(VehiclePrefs.getVehicles(activity));
        android.widget.ArrayAdapter<String> adapter =
                new android.widget.ArrayAdapter<>(activity, android.R.layout.simple_list_item_single_choice, vehicles);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        String primary = VehiclePrefs.getPrimaryVehicle(activity);
        updatePrimaryLabel(tvPrimary, primary);
        int primaryIndex = vehicles.indexOf(primary);
        if (primaryIndex >= 0) {
            listView.setItemChecked(primaryIndex, true);
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = vehicles.get(position);
            VehiclePrefs.setPrimaryVehicle(activity, selected);
            updatePrimaryLabel(tvPrimary, selected);
        });

        if (prefillPlate != null && etVehicleInput != null && !prefillPlate.isEmpty()) {
            etVehicleInput.setText(prefillPlate);
        }

        if (etVehicleInput != null) {
            final boolean[] isFormatting = {false};
            etVehicleInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // No-op
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // No-op
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isFormatting[0]) {
                        return;
                    }
                    String raw = s != null ? s.toString() : "";
                    String formatted = formatPlate(raw);
                    if (!formatted.equals(raw)) {
                        isFormatting[0] = true;
                        etVehicleInput.setText(formatted);
                        etVehicleInput.setSelection(formatted.length());
                        isFormatting[0] = false;
                    }
                    updateInputState(inputLayout, btnAdd, formatted);
                }
            });
            updateInputState(inputLayout, btnAdd, etVehicleInput.getText() != null ? etVehicleInput.getText().toString() : "");
        }

        btnAdd.setOnClickListener(v -> {
            String raw = etVehicleInput.getText() != null ? etVehicleInput.getText().toString() : "";
            String normalized = VehiclePrefs.normalizeVehicle(raw);
            if (normalized.isEmpty()) {
                updateInputState(inputLayout, btnAdd, raw);
                return;
            }
            if (!VehiclePrefs.isValidPlate(normalized)) {
                updateInputState(inputLayout, btnAdd, raw);
                return;
            }
            boolean added = VehiclePrefs.addVehicle(activity, normalized);
            if (!added) {
                Toast.makeText(activity, "Vehicle already exists", Toast.LENGTH_SHORT).show();
                return;
            }
            vehicles.clear();
            vehicles.addAll(VehiclePrefs.getVehicles(activity));
            adapter.notifyDataSetChanged();
            listView.setItemChecked(vehicles.indexOf(normalized), true);
            VehiclePrefs.setPrimaryVehicle(activity, normalized);
            updatePrimaryLabel(tvPrimary, normalized);
            if (etVehicleInput != null) {
                etVehicleInput.setText("");
            }
        });

        btnRemove.setOnClickListener(v -> {
            int position = listView.getCheckedItemPosition();
            if (position == ListView.INVALID_POSITION || vehicles.isEmpty()) {
                Toast.makeText(activity, "Select a vehicle to remove", Toast.LENGTH_SHORT).show();
                return;
            }
            String selected = vehicles.get(position);
            VehiclePrefs.removeVehicle(activity, selected);
            vehicles.clear();
            vehicles.addAll(VehiclePrefs.getVehicles(activity));
            adapter.notifyDataSetChanged();

            String newPrimary = VehiclePrefs.getPrimaryVehicle(activity);
            if (newPrimary.isEmpty() && !vehicles.isEmpty()) {
                newPrimary = vehicles.get(0);
                VehiclePrefs.setPrimaryVehicle(activity, newPrimary);
            }
            updatePrimaryLabel(tvPrimary, newPrimary);
            if (!vehicles.isEmpty() && !newPrimary.isEmpty()) {
                int newIndex = vehicles.indexOf(newPrimary);
                if (newIndex >= 0) {
                    listView.setItemChecked(newIndex, true);
                }
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .create();
        dialog.setOnDismissListener(d -> {
            if (listener != null) {
                listener.onVehiclesUpdated();
            }
        });

        btnDone.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private static void updatePrimaryLabel(TextView tvPrimary, String primary) {
        if (primary == null || primary.isEmpty()) {
            tvPrimary.setText("Primary: Not set");
        } else {
            tvPrimary.setText("Primary: " + primary);
        }
    }

    private static void updateInputState(TextInputLayout inputLayout, MaterialButton btnAdd, String raw) {
        String normalized = VehiclePrefs.normalizeVehicle(raw);
        boolean isEmpty = normalized.isEmpty();
        boolean isValid = !isEmpty && VehiclePrefs.isValidPlate(normalized);

        if (inputLayout != null) {
            if (isEmpty) {
                inputLayout.setError(null);
            } else if (!isValid) {
                inputLayout.setError("Format: MH-01-AB-1234");
            } else {
                inputLayout.setError(null);
            }
        }
        btnAdd.setEnabled(isValid);
        btnAdd.setAlpha(isValid ? 1.0f : 0.5f);
    }

    private static String formatPlate(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.US);
        if (cleaned.length() > 10) {
            cleaned = cleaned.substring(0, 10);
        }
        if (cleaned.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int index = 0;

        int part1 = Math.min(2, cleaned.length());
        sb.append(cleaned, 0, part1);
        index += part1;
        if (cleaned.length() > index) {
            sb.append("-");
        }

        int part2 = Math.min(2, cleaned.length() - index);
        sb.append(cleaned, index, index + part2);
        index += part2;
        if (cleaned.length() > index) {
            sb.append("-");
        }

        int part3 = Math.min(2, cleaned.length() - index);
        sb.append(cleaned, index, index + part3);
        index += part3;
        if (cleaned.length() > index) {
            sb.append("-");
        }

        int part4 = Math.min(4, cleaned.length() - index);
        sb.append(cleaned, index, index + part4);

        return sb.toString();
    }
}
