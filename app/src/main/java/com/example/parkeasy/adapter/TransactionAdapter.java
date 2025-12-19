package com.example.parkeasy.adapter;

import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
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

        holder.tvDesc.setText(tx.getDescription());
        holder.tvDate.setText(DateFormat.format("dd MMM, hh:mm a", tx.getTimestamp()));

        if ("CREDIT".equals(tx.getType())) {
            holder.tvAmount.setText("+ ₹" + (int)tx.getAmount());
            holder.tvAmount.setTextColor(Color.parseColor("#00FF88")); // Green
        } else {
            holder.tvAmount.setText("- ₹" + (int)tx.getAmount());
            holder.tvAmount.setTextColor(Color.parseColor("#FF1744")); // Red
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