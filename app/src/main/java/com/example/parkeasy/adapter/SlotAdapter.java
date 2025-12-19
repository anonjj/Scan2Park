package com.example.parkeasy.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkeasy.R;
import com.example.parkeasy.databinding.ItemSlotBinding;
import com.example.parkeasy.model.Slot;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the grid of parking slots in the RecyclerView.
 * Responsible for displaying the state of each slot (Available, Booked, Selected).
 */
public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.SlotViewHolder> {

    private List<Slot> slots = new ArrayList<>();
    private final OnSlotClickListener listener;
    private Slot selectedSlot; // Holds the currently tapped slot to give it a visual glow.


    /**
     * Callback interface to notify the Activity/Fragment when a user clicks on an available slot.
     */
    public interface OnSlotClickListener {
        void onSlotClick(Slot slot);
    }

    public SlotAdapter(OnSlotClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the list of slots and triggers a full UI refresh.
     * Typically called after fetching data from Firestore.
     */
    public void submitList(List<Slot> newSlots) {
        this.slots = newSlots;
        notifyDataSetChanged();
    }

    /**
     * Caches the user's selected slot and triggers a redraw to apply the 'Selected' state visuals.
     */
    public void setSelectedSlot(Slot slot) {
        this.selectedSlot = slot;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSlotBinding binding = ItemSlotBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new SlotViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        Slot slot = slots.get(position);
        holder.bind(slot);
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    /**
     * ViewHolder for a single parking slot item.
     * This is where the core logic for the slot's visual state lives.
     */
    class SlotViewHolder extends RecyclerView.ViewHolder {
        private final ItemSlotBinding binding;

        public SlotViewHolder(ItemSlotBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds a slot object to the view, setting its appearance based on its state.
         * States:
         * - Red (#FF1744): Booked and currently active.
         * - Purple (#BC13FE): Available, but currently selected by the user.
         * - Cyan (#00F3FF): Available for booking.
         */
        // Inside SlotViewHolder class
        public void bind(Slot slot) {
            // 1. Set Name
            binding.tvSlotName.setText(slot.getName());

            // 2. Logic: Image & Color
            long currentTime = System.currentTimeMillis();
            boolean isActuallyOccupied = slot.isOccupied() && (slot.getExpiryTime() > currentTime);

            // Get views (Assuming you updated binding or using findViewById)
            // ImageView ivIcon = binding.ivSlotIcon;
            // TextView tvName = binding.tvSlotName;
            // CardView container = binding.slotContainer; // If you gave ID to CardView

            if (isActuallyOccupied) {
                // --- CASE 1: OCCUPIED (Red Car) ---
                binding.ivSlotIcon.setImageResource(R.drawable.ic_car_top_view);
                binding.ivSlotIcon.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF1744"))); // Red

                binding.tvSlotName.setTextColor(android.graphics.Color.parseColor("#505050")); // Dim text
                binding.slotContainer.setAlpha(0.7f); // Dim the whole slot slightly
                itemView.setClickable(false);
            }
            else if (selectedSlot != null && selectedSlot.getSlotId().equals(slot.getSlotId())) {
                // --- CASE 2: SELECTED (Purple Car) ---
                binding.ivSlotIcon.setImageResource(R.drawable.ic_car_top_view); // Show car to preview
                binding.ivSlotIcon.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#BC13FE"))); // Neon Purple

                binding.tvSlotName.setTextColor(android.graphics.Color.parseColor("#BC13FE"));
                binding.slotContainer.setAlpha(1.0f);
                itemView.setClickable(true);
            }
            else {
                // --- CASE 3: FREE (Empty Box) ---
                binding.ivSlotIcon.setImageResource(R.drawable.ic_slot_empty);
                binding.ivSlotIcon.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00FF88"))); // Cyan

                binding.tvSlotName.setTextColor(android.graphics.Color.parseColor("#00FF88"));
                binding.slotContainer.setAlpha(1.0f);
                itemView.setClickable(true);
            }

            itemView.setOnClickListener(v -> {
                if (!isActuallyOccupied) {
                    listener.onSlotClick(slot);
                }
            });
        }
    }
    public void setSlots(List<Slot> newSlots) {
        this.slots = newSlots; // Or whatever your internal list variable is named
        notifyDataSetChanged();
    }
}
