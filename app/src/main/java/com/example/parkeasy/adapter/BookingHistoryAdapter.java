package com.example.parkeasy.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.parkeasy.R;
import com.example.parkeasy.model.Booking;
import com.google.android.material.button.MaterialButton; // Import this!
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.ViewHolder> {

    private Context context;
    private List<Booking> bookingList;
    private OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onItemClick(Booking booking);
        void onCancelClick(Booking booking);
        void onExtendClick(Booking booking);
    }

    public BookingHistoryAdapter(Context context, List<Booking> bookingList, OnBookingActionListener listener) {
        this.context = context;
        this.bookingList = bookingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookingList.get(position);

        // 1. Basic Details
        holder.tvSlotName.setText(booking.getSlotName());
        holder.tvPrice.setText("₹" + (int)booking.getTotalCost());

        // Fix: Use the correct ID from XML (tvLocationLabel)
        holder.tvLocation.setText(booking.getLocationName());

        // New: Bind the Booking ID (Safety check in case ID is null)
        String shortId = booking.getBookingId();
        if (shortId != null && shortId.length() > 8) shortId = shortId.substring(0, 8);
        holder.tvBookingId.setText("ID: #" + (shortId != null ? shortId.toUpperCase() : "---"));

        // 2. Format Date (Start - End)
        if (booking.getStartTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
            String start = sdf.format(booking.getStartTime());
            // Optionally add End Time if available
            // String end = sdf.format(booking.getEndTime());
            holder.tvDate.setText(start);
        }

        // 3. Status Logic (Visual Auto-Correction)
        String rawStatus = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "UNKNOWN";
        Date now = new Date();
        boolean isTimeExpired = booking.getEndTime() != null && booking.getEndTime().before(now);

        String displayStatus = rawStatus;
        // If time is up, force visual status to COMPLETED
        if (isTimeExpired && (rawStatus.equals("ACTIVE") || rawStatus.equals("CONFIRMED"))) {
            displayStatus = "COMPLETED";
        }

        // 4. Styling the "Chip"
        holder.tvStatus.setBackgroundResource(R.drawable.bg_card_modern); // Ensure rounded bg

        if (displayStatus.equals("ACTIVE") || displayStatus.equals("CONFIRMED") || displayStatus.equals("EXTENDED")) {
            // BLUE Theme
            holder.tvStatus.setText("ACTIVE");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_primary));
            holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
            holder.layoutActions.setVisibility(View.VISIBLE);
        }
        else if (displayStatus.equals("COMPLETED")) {
            // GREEN Theme
            holder.tvStatus.setText("COMPLETED");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_secondary));
            holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            holder.layoutActions.setVisibility(View.GONE);
        }
        else {
            // RED Theme
            holder.tvStatus.setText("CANCELLED");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error_red));
            holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            holder.layoutActions.setVisibility(View.GONE);
        }

        // 5. Click Listeners
        holder.itemView.setOnClickListener(v -> listener.onItemClick(booking));
        holder.btnCancel.setOnClickListener(v -> listener.onCancelClick(booking));
        holder.btnExtend.setOnClickListener(v -> listener.onExtendClick(booking));
    }

    @Override
    public int getItemCount() { return bookingList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Updated View Names to match new XML
        TextView tvSlotName, tvBookingId, tvLocation, tvPrice, tvDate, tvStatus;
        MaterialButton btnCancel, btnExtend; // Changed to MaterialButton
        LinearLayout layoutActions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // MAPPING IDs TO XML
            tvSlotName = itemView.findViewById(R.id.tvSlotName);
            tvBookingId = itemView.findViewById(R.id.tvBookingId); // Added this!
            tvLocation = itemView.findViewById(R.id.tvLocationLabel); // FIXED ID
            tvPrice = itemView.findViewById(R.id.tvCost);
            tvDate = itemView.findViewById(R.id.tvTimeRange);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnExtend = itemView.findViewById(R.id.btnExtend);
        }
    }
}