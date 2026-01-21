package com.example.parkeasy.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parkeasy.R;
import com.example.parkeasy.databinding.ItemSlotBinding;
import com.example.parkeasy.model.Slot;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the grid of parking slots in the RecyclerView.
 * Responsible for displaying the state of each slot (Available, Occupied, Selected).
 */
public class SlotAdapter extends RecyclerView.Adapter<SlotAdapter.SlotViewHolder> {

    private List<Slot> slots = new ArrayList<>();
    private final OnSlotClickListener listener;
    private Slot selectedSlot;

    public interface OnSlotClickListener {
        void onSlotClick(Slot slot);
    }

    public SlotAdapter(OnSlotClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Slot> newSlots) {
        List<Slot> oldSlots = new ArrayList<>(this.slots);
        List<Slot> nextSlots = newSlots != null ? new ArrayList<>(newSlots) : new ArrayList<>();

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldSlots.size();
            }

            @Override
            public int getNewListSize() {
                return nextSlots.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                String oldId = oldSlots.get(oldItemPosition).getSlotId();
                String newId = nextSlots.get(newItemPosition).getSlotId();
                if (oldId == null || newId == null) {
                    return oldItemPosition == newItemPosition;
                }
                return oldId.equals(newId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Slot oldSlot = oldSlots.get(oldItemPosition);
                Slot newSlot = nextSlots.get(newItemPosition);
                return oldSlot.isOccupied() == newSlot.isOccupied()
                        && oldSlot.getExpiryTime() == newSlot.getExpiryTime()
                        && oldSlot.isActive() == newSlot.isActive()
                        && safeEquals(oldSlot.getName(), newSlot.getName());
            }
        });

        this.slots = nextSlots;
        if (selectedSlot != null && findIndexById(selectedSlot.getSlotId()) == -1) {
            selectedSlot = null;
        }
        diffResult.dispatchUpdatesTo(this);
    }

    public void setSelectedSlot(Slot slot) {
        String previousId = selectedSlot != null ? selectedSlot.getSlotId() : null;
        this.selectedSlot = slot;

        int previousIndex = findIndexById(previousId);
        int newIndex = findIndexById(slot != null ? slot.getSlotId() : null);

        if (previousIndex >= 0) {
            notifyItemChanged(previousIndex);
        }
        if (newIndex >= 0 && newIndex != previousIndex) {
            notifyItemChanged(newIndex);
        }
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

    private int findIndexById(String slotId) {
        if (slotId == null) {
            return -1;
        }
        for (int i = 0; i < slots.size(); i++) {
            if (slotId.equals(slots.get(i).getSlotId())) {
                return i;
            }
        }
        return -1;
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    class SlotViewHolder extends RecyclerView.ViewHolder {
        private final ItemSlotBinding binding;

        public SlotViewHolder(ItemSlotBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Slot slot) {
            Context context = itemView.getContext();
            binding.tvSlotName.setText(slot.getName());

            long currentTime = System.currentTimeMillis();
            boolean isActuallyOccupied = slot.isOccupied() && (slot.getExpiryTime() > currentTime);
            boolean isSelected = selectedSlot != null && selectedSlot.getSlotId().equals(slot.getSlotId());

            if (isSelected) {
                // --- CASE 1: SELECTED (Brand Blue Highlight) ---
                binding.slotContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.brand_primary));
                binding.tvSlotName.setTextColor(ContextCompat.getColor(context, R.color.white));
                binding.ivSlotIcon.setImageResource(R.drawable.ic_car_top_view);
                binding.ivSlotIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white)));
                binding.slotContainer.setAlpha(1.0f);
                itemView.setEnabled(true);
            } 
            else if (isActuallyOccupied) {
                // --- CASE 2: OCCUPIED (Greyed Out) ---
                binding.slotContainer.setCardBackgroundColor(Color.parseColor("#E0E0E0")); // Light Grey
                binding.tvSlotName.setTextColor(Color.parseColor("#9E9E9E")); // Dimmed Text
                binding.ivSlotIcon.setImageResource(R.drawable.ic_car_top_view);
                binding.ivSlotIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E"))); // Dimmed Icon
                binding.slotContainer.setAlpha(0.8f);
                itemView.setEnabled(false);
            } 
            else {
                // --- CASE 3: AVAILABLE (Default White State) ---
                binding.slotContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white));
                binding.tvSlotName.setTextColor(ContextCompat.getColor(context, R.color.text_primary)); // Dark Grey
                binding.ivSlotIcon.setImageResource(R.drawable.ic_slot_empty);
                binding.ivSlotIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brand_primary))); // Blue Icon
                binding.slotContainer.setAlpha(1.0f);
                itemView.setEnabled(true);
            }

            itemView.setOnClickListener(v -> {
                if (!isActuallyOccupied) {
                    listener.onSlotClick(slot);
                }
            });
        }
    }
}
