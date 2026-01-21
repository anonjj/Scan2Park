package com.example.parkeasy.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.parkeasy.R;
import com.example.parkeasy.model.Transaction;
import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TxViewHolder> {

    private final List<Transaction> list;

    public TransactionAdapter(List<Transaction> list) {
        this.list = list;
    }

    public void updateData(List<Transaction> newList) {
        List<Transaction> oldList = new ArrayList<>(list);
        List<Transaction> nextList = newList != null ? new ArrayList<>(newList) : new ArrayList<>();

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
                Transaction oldItem = oldList.get(oldItemPosition);
                Transaction newItem = nextList.get(newItemPosition);
                String oldId = oldItem.getTransactionId();
                String newId = newItem.getTransactionId();
                if (oldId == null || newId == null) {
                    return safeDateEquals(oldItem.getTimestamp(), newItem.getTimestamp());
                }
                return oldId.equals(newId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Transaction oldItem = oldList.get(oldItemPosition);
                Transaction newItem = nextList.get(newItemPosition);
                return oldItem.getAmount() == newItem.getAmount()
                        && safeEquals(oldItem.getType(), newItem.getType())
                        && safeEquals(oldItem.getDescription(), newItem.getDescription())
                        && safeDateEquals(oldItem.getTimestamp(), newItem.getTimestamp());
            }
        });

        list.clear();
        list.addAll(nextList);
        diffResult.dispatchUpdatesTo(this);
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private boolean safeDateEquals(java.util.Date a, java.util.Date b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    @NonNull
    @Override
    public TxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TxViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TxViewHolder holder, int position) {
        Transaction tx = list.get(position);
        Context context = holder.itemView.getContext();

        holder.tvDesc.setText(tx.getDescription());
        holder.tvDate.setText(DateFormat.format("dd MMM, hh:mm a", tx.getTimestamp()));

        double amount = tx.getAmount();
        
        if (amount > 0) {
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.brand_secondary)); // Green
            holder.tvAmount.setText("+ ₹" + (int)amount);
        } else {
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.text_primary)); // Black
            holder.tvAmount.setText("- ₹" + (int)Math.abs(amount));
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class TxViewHolder extends RecyclerView.ViewHolder {
        TextView tvDesc, tvDate, tvAmount;
        public TxViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
