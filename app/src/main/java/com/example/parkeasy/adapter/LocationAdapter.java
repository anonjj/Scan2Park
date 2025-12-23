package com.example.parkeasy.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.parkeasy.databinding.ItemLocationBinding;
import com.example.parkeasy.model.ParkingLocation;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private List<ParkingLocation> locationList;
    private final OnLocationClickListener listener;

    public interface OnLocationClickListener {
        void onLocationClick(ParkingLocation location);
    }

    public LocationAdapter(List<ParkingLocation> locationList, OnLocationClickListener listener) {
        this.locationList = locationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLocationBinding binding = ItemLocationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LocationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        ParkingLocation location = locationList.get(position);

        // 1. Bind Name & Address (Updated IDs)
        holder.binding.tvLocName.setText(location.getName());
        holder.binding.tvLocAddress.setText(location.getAddress());

        // 2. Bind Price (The new Modern UI highlights this!)
        // Note: Assuming your ParkingLocation model has getRate() or getRatePerHour()
        // If not, just put a placeholder like "₹40/hr"
        int rate = location.getRatePerHour();
        holder.binding.tvRate.setText("₹" + rate + "/hr");

        // 3. Click Listener
        holder.itemView.setOnClickListener(v -> listener.onLocationClick(location));
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        ItemLocationBinding binding;
        public LocationViewHolder(ItemLocationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}