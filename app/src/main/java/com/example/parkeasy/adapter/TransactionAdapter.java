package com.example.parkeasy.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.parkeasy.R;
import com.example.parkeasy.model.Transaction;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TxViewHolder> {

    private final List<Transaction> list;

    public TransactionAdapter(List<Transaction> list) {
        this.list = list;
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
