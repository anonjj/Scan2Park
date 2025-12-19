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
        holder.binding.tvLocationName.setText(location.getName());
        holder.binding.tvAddress.setText(location.getAddress());
        holder.binding.tvSlotsInfo.setText(location.getTotalSlots() + " Slots Total");

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