package com.example.parkeasy.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.parkeasy.R;
import com.example.parkeasy.SlotSelectionActivity;
import com.example.parkeasy.model.ParkingLocation;
import java.util.List;

public class ParkingLocationAdapter extends RecyclerView.Adapter<ParkingLocationAdapter.LocationViewHolder> {

    private Context context;
    private List<ParkingLocation> locationList;

    public ParkingLocationAdapter(Context context, List<ParkingLocation> locationList) {
        this.context = context;
        this.locationList = locationList;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        ParkingLocation location = locationList.get(position);

        holder.tvName.setText(location.getName());
        holder.tvAddress.setText(location.getAddress());

        // 1. SHOW ACTUAL SLOT COUNT
        holder.tvSlots.setText(location.getTotalSlots() + " Slots Total");

        // Optional: Show Price if you have a TextView for it in item_parking_location.xml
        // holder.tvPrice.setText("₹" + location.getRatePerHour() + "/hr");

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SlotSelectionActivity.class);
            intent.putExtra("LOCATION_ID", location.getLocationId());
            intent.putExtra("LOCATION_NAME", location.getName());

            // 2. PASS THE CORRECT RATE
            intent.putExtra("RATE", location.getRatePerHour());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return locationList.size(); }

    public static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvSlots;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvLocationName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvSlots = itemView.findViewById(R.id.tvSlotsInfo); // Ensure ID matches your XML
        }
    }
}