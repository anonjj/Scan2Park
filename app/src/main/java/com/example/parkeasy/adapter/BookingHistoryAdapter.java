package com.example.parkeasy.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.parkeasy.R;
import com.example.parkeasy.model.Booking;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.ViewHolder> {

    private Context context;
    private List<Booking> bookingList;
    private OnBookingActionListener listener;

    // ✅ CORRECT INTERFACE: Has all 3 actions needed by your Activity
    public interface OnBookingActionListener {
        void onItemClick(Booking booking);   // View Receipt
        void onCancelClick(Booking booking); // Cancel Action
        void onExtendClick(Booking booking); // Extend Action
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

        // ✅ USE REAL DATA (No string hacking)
        holder.tvSlotName.setText(booking.getSlotName());
        holder.tvLocation.setText(booking.getLocationName());
        holder.tvPrice.setText("₹" + (int)booking.getTotalCost());

        if (booking.getStartTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
            holder.tvDate.setText(sdf.format(booking.getStartTime()));
        }

        String status = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "UNKNOWN";
        holder.tvStatus.setText(status);

        // 🎨 SUPERIOR COLOR LOGIC
        if (status.equals("ACTIVE") || status.equals("CONFIRMED")) {
            int color = Color.parseColor("#00FF88"); // Neon Green
            holder.statusStrip.setBackgroundColor(color);
            holder.tvStatus.setTextColor(color);
            holder.layoutActions.setVisibility(View.VISIBLE); // ✅ Show Buttons
        }
        else if (status.equals("CANCELLED")) {
            int color = Color.parseColor("#FF0055"); // Neon Red
            holder.statusStrip.setBackgroundColor(color);
            holder.tvStatus.setTextColor(color);
            holder.layoutActions.setVisibility(View.GONE); // 🙈 Hide Buttons
        }
        else {
            int color = Color.parseColor("#00F0FF"); // Neon Cyan
            holder.statusStrip.setBackgroundColor(color);
            holder.tvStatus.setTextColor(color);
            holder.layoutActions.setVisibility(View.GONE); // 🙈 Hide Buttons
        }

        // ✅ CLICK LISTENERS
        holder.itemView.setOnClickListener(v -> listener.onItemClick(booking));
        holder.btnCancel.setOnClickListener(v -> listener.onCancelClick(booking));
        holder.btnExtend.setOnClickListener(v -> listener.onExtendClick(booking));
    }

    @Override
    public int getItemCount() { return bookingList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSlotName, tvLocation, tvPrice, tvDate, tvStatus;
        View statusStrip, btnCancel, btnExtend;
        LinearLayout layoutActions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs exist in item_booking_history.xml
            tvSlotName = itemView.findViewById(R.id.tvSlotName);
            tvLocation = itemView.findViewById(R.id.tvLocationName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            statusStrip = itemView.findViewById(R.id.viewStatusColor);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnExtend = itemView.findViewById(R.id.btnExtend);
        }
    }
}