package com.example.parkeasy.util;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class VehiclePrefs {
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_VEHICLES = "vehicles";
    private static final String KEY_PRIMARY = "primary_vehicle";
    private static final String KEY_LEGACY = "my_car";
    private static final Pattern VEHICLE_PLATE_PATTERN =
            Pattern.compile("^[A-Z]{2}-\\d{2}-[A-Z]{1,2}-\\d{4}$");
    private static final Pattern VEHICLE_PLATE_EXTRACT_PATTERN =
            Pattern.compile("([A-Z]{2})\\s?-?\\s?(\\d{2})\\s?-?\\s?([A-Z]{1,2})\\s?-?\\s?(\\d{4})",
                    Pattern.CASE_INSENSITIVE);

    private VehiclePrefs() {
    }

    public static List<String> getVehicles(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> stored = prefs.getStringSet(KEY_VEHICLES, new HashSet<>());
        Set<String> vehicles = new HashSet<>(stored != null ? stored : new HashSet<>());

        String legacy = prefs.getString(KEY_LEGACY, "");
        String normalizedLegacy = normalizeVehicle(legacy);
        boolean updated = false;
        if (!normalizedLegacy.isEmpty() && !vehicles.contains(normalizedLegacy)) {
            vehicles.add(normalizedLegacy);
            updated = true;
        }

        String primary = prefs.getString(KEY_PRIMARY, "");
        if (primary.isEmpty() && !normalizedLegacy.isEmpty()) {
            primary = normalizedLegacy;
            updated = true;
        }

        if (updated) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet(KEY_VEHICLES, vehicles);
            if (!primary.isEmpty()) {
                editor.putString(KEY_PRIMARY, primary);
            }
            editor.apply();
        }

        List<String> list = new ArrayList<>(vehicles);
        Collections.sort(list);
        return list;
    }

    public static String getPrimaryVehicle(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String primary = prefs.getString(KEY_PRIMARY, "");
        if (!primary.isEmpty()) {
            return primary;
        }
        String legacy = normalizeVehicle(prefs.getString(KEY_LEGACY, ""));
        return legacy;
    }

    public static boolean addVehicle(Context context, String vehicle) {
        String normalized = normalizeVehicle(vehicle);
        if (normalized.isEmpty()) {
            return false;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> stored = prefs.getStringSet(KEY_VEHICLES, new HashSet<>());
        Set<String> vehicles = new HashSet<>(stored != null ? stored : new HashSet<>());
        if (vehicles.contains(normalized)) {
            return false;
        }
        vehicles.add(normalized);
        prefs.edit().putStringSet(KEY_VEHICLES, vehicles).apply();
        if (getPrimaryVehicle(context).isEmpty()) {
            setPrimaryVehicle(context, normalized);
        }
        return true;
    }

    public static void removeVehicle(Context context, String vehicle) {
        String normalized = normalizeVehicle(vehicle);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> stored = prefs.getStringSet(KEY_VEHICLES, new HashSet<>());
        Set<String> vehicles = new HashSet<>(stored != null ? stored : new HashSet<>());
        if (!vehicles.remove(normalized)) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_VEHICLES, vehicles);

        String primary = prefs.getString(KEY_PRIMARY, "");
        if (!primary.isEmpty() && primary.equals(normalized)) {
            editor.remove(KEY_PRIMARY);
        }
        String legacy = prefs.getString(KEY_LEGACY, "");
        if (!legacy.isEmpty() && legacy.equals(normalized)) {
            editor.remove(KEY_LEGACY);
        }
        editor.apply();
    }

    public static void setPrimaryVehicle(Context context, String vehicle) {
        String normalized = normalizeVehicle(vehicle);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (normalized.isEmpty()) {
            editor.remove(KEY_PRIMARY);
        } else {
            editor.putString(KEY_PRIMARY, normalized);
            editor.putString(KEY_LEGACY, normalized);
        }
        editor.apply();
    }

    public static String normalizeVehicle(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.US);
    }

    public static boolean isValidPlate(String normalized) {
        if (normalized == null) {
            return false;
        }
        return VEHICLE_PLATE_PATTERN.matcher(normalized).matches();
    }

    public static String extractPlate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        java.util.regex.Matcher matcher = VEHICLE_PLATE_EXTRACT_PATTERN.matcher(raw.toUpperCase(Locale.US));
        if (!matcher.find()) {
            return "";
        }
        String state = matcher.group(1);
        String district = matcher.group(2);
        String series = matcher.group(3);
        String number = matcher.group(4);
        return state + "-" + district + "-" + series + "-" + number;
    }
}
