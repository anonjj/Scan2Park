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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.parkeasy.R;
import com.example.parkeasy.model.Booking;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
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
        void onExitClick(Booking booking);
    }

    public BookingHistoryAdapter(Context context, List<Booking> bookingList, OnBookingActionListener listener) {
        this.context = context;
        this.bookingList = new ArrayList<>(bookingList);
        this.listener = listener;
    }

    public void submitList(List<Booking> newList) {
        List<Booking> oldList = new ArrayList<>(bookingList);
        List<Booking> nextList = newList != null ? new ArrayList<>(newList) : new ArrayList<>();

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return nextList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                String oldId = oldList.get(oldItemPosition).getBookingId();
                String newId = nextList.get(newItemPosition).getBookingId();
                if (oldId == null || newId == null) {
                    return oldItemPosition == newItemPosition;
                }
                return oldId.equals(newId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Booking oldItem = oldList.get(oldItemPosition);
                Booking newItem = nextList.get(newItemPosition);
                return safeEquals(oldItem.getStatus(), newItem.getStatus())
                        && safeEquals(oldItem.getLocationName(), newItem.getLocationName())
                        && safeEquals(oldItem.getSlotName(), newItem.getSlotName())
                        && safeEquals(oldItem.getVehicleNumber(), newItem.getVehicleNumber())
                        && oldItem.getTotalCost() == newItem.getTotalCost()
                        && safeDateEquals(oldItem.getStartTime(), newItem.getStartTime())
                        && safeDateEquals(oldItem.getEndTime(), newItem.getEndTime());
            }
        });

        bookingList.clear();
        bookingList.addAll(nextList);
        diffResult.dispatchUpdatesTo(this);
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
        holder.tvLocation.setText(booking.getLocationName());

        String shortId = booking.getBookingId();
        if (shortId != null && shortId.length() > 8) shortId = shortId.substring(0, 8);
        holder.tvBookingId.setText("ID: #" + (shortId != null ? shortId.toUpperCase() : "---"));

        // 2. Format Date
        if (booking.getStartTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
            holder.tvDate.setText(sdf.format(booking.getStartTime()));
        }

        String vehicleNumber = booking.getVehicleNumber();
        if (vehicleNumber != null && !vehicleNumber.isEmpty() && !"NOT_SET".equals(vehicleNumber)) {
            holder.tvVehicleNumber.setText("Vehicle: " + vehicleNumber);
            holder.layoutVehicle.setVisibility(View.VISIBLE);
        } else {
            holder.layoutVehicle.setVisibility(View.GONE);
        }

        // 3. Status Logic
        String rawStatus = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "UNKNOWN";
        Date now = new Date();
        boolean isTimeExpired = booking.getEndTime() != null && booking.getEndTime().before(now);

        String displayStatus = rawStatus;
        if (isTimeExpired && (rawStatus.equals("ACTIVE") || rawStatus.equals("CONFIRMED"))) {
            displayStatus = "COMPLETED";
        }

        // 4. Styling and Action Visibility
        holder.tvStatus.setBackgroundResource(R.drawable.bg_card_modern);

        if (displayStatus.equals("ACTIVE") || displayStatus.equals("CONFIRMED") || displayStatus.equals("EXTENDED")) {
            // 🔵 ACTIVE State
            holder.tvStatus.setText("ACTIVE");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_primary));
            holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
            holder.layoutActions.setVisibility(View.VISIBLE);

            // 🟢 Action Buttons for Running Session
            holder.btnExit.setVisibility(View.VISIBLE);
            holder.btnExit.setText("End Session");
            holder.btnExit.setOnClickListener(v -> listener.onExitClick(booking));

            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setText("Cancel");
            holder.btnCancel.setOnClickListener(v -> listener.onCancelClick(booking));

            holder.btnExtend.setVisibility(View.VISIBLE);
            holder.btnExtend.setOnClickListener(v -> listener.onExtendClick(booking));
        }
        else if (displayStatus.equals("COMPLETED")) {
            // 🟢 COMPLETED State
            holder.tvStatus.setText("COMPLETED");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_secondary));
            holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            
            holder.layoutActions.setVisibility(View.GONE);
            holder.btnExit.setVisibility(View.GONE);
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnExtend.setVisibility(View.GONE);
        }
        else {
            // 🔴 CANCELLED State
            holder.tvStatus.setText("CANCELLED");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error_red));
            holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            
            holder.layoutActions.setVisibility(View.GONE);
            holder.btnExit.setVisibility(View.GONE);
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnExtend.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(booking));
    }

    @Override
    public int getItemCount() { return bookingList.size(); }

    private boolean safeEquals(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private boolean safeDateEquals(Date a, Date b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSlotName, tvBookingId, tvLocation, tvPrice, tvDate, tvStatus, tvVehicleNumber;
        MaterialButton btnCancel, btnExtend, btnExit;
        LinearLayout layoutActions, layoutVehicle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSlotName = itemView.findViewById(R.id.tvSlotName);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvLocation = itemView.findViewById(R.id.tvLocationLabel);
            tvPrice = itemView.findViewById(R.id.tvCost);
            tvDate = itemView.findViewById(R.id.tvTimeRange);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            layoutVehicle = itemView.findViewById(R.id.vehicleLayout);
            tvVehicleNumber = itemView.findViewById(R.id.tvVehicleNumber);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnExtend = itemView.findViewById(R.id.btnExtend);
            btnExit = itemView.findViewById(R.id.btnExit);
        }
    }
}
