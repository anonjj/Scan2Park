package com.example.parkeasy.adapter; // Update package if needed

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.parkeasy.R;
import com.example.parkeasy.model.Booking;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private final Context context;
    private final List<Booking> bookingList;
    private final OnBookingActionListener listener;

    // Interface for handling button clicks in the activity
    public interface OnBookingActionListener {
        void onCancelClick(Booking booking);
        void onExtendClick(Booking booking);
    }

    public BookingAdapter(Context context, List<Booking> bookingList, OnBookingActionListener listener) {
        this.context = context;
        this.bookingList = bookingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        holder.bind(booking, listener);
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvSlotName, tvBookingId, tvTimeRange, tvCost;
        View viewStatus, btnCancel, btnExtend;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSlotName = itemView.findViewById(R.id.tvSlotName);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvTimeRange = itemView.findViewById(R.id.tvTimeRange);
            tvCost = itemView.findViewById(R.id.tvCost);
            viewStatus = itemView.findViewById(R.id.viewStatus);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnExtend = itemView.findViewById(R.id.btnExtend);
        }

        // The bind method is now the single source of truth for setting view properties.
        void bind(final Booking booking, final OnBookingActionListener listener) {
            // 1. Display Booking Info
            tvSlotName.setText("Slot: " + extractSlotName(booking.getSlotId()));
            tvBookingId.setText("#" + getShortBookingId(booking.getBookingId()));
            tvCost.setText("₹" + booking.getTotalCost());
            tvTimeRange.setText(formatDate(booking.getStartTime()));

            // 2. Set View Visibility and Colors based on Status
            if ("ACTIVE".equals(booking.getStatus())) {
                btnCancel.setVisibility(View.VISIBLE);
                btnExtend.setVisibility(View.VISIBLE);
                viewStatus.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
            } else { // For "CANCELED", "COMPLETED", or any other status
                btnCancel.setVisibility(View.GONE);
                btnExtend.setVisibility(View.GONE);
                viewStatus.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            }

            // 3. Set Click Listeners
            // Using a single listener for both buttons is more efficient
            btnCancel.setOnClickListener(v -> listener.onCancelClick(booking));
            btnExtend.setOnClickListener(v -> listener.onExtendClick(booking));
        }

        // --- Helper Methods ---

        private String formatDate(java.util.Date date) {
            if (date == null) return "N/A";
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
            return sdf.format(date);
        }

        private String getShortBookingId(String bookingId) {
            if (bookingId == null || bookingId.length() <= 6) {
                return bookingId;
            }
            return bookingId.substring(bookingId.length() - 6);
        }

        private String extractSlotName(String fullSlotId) {
            if (fullSlotId != null && fullSlotId.contains("_")) {
                return fullSlotId.substring(fullSlotId.lastIndexOf("_") + 1);
            }
            return fullSlotId;
        }
    }
}
