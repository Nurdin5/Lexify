package com.example.lexify.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lexify.R
import com.example.lexify.model.DailyProfit
import java.text.NumberFormat
import java.util.Locale

class ProfitAdapter(
    private val onDeleteClick: (DailyProfit) -> Unit
) : RecyclerView.Adapter<ProfitAdapter.ProfitViewHolder>() {

    private val profits = mutableListOf<DailyProfit>()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
    }

    fun submitList(newProfits: List<DailyProfit>) {
        profits.clear()
        profits.addAll(newProfits)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profit, parent, false)
        return ProfitViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfitViewHolder, position: Int) {
        holder.bind(profits[position])
    }

    override fun getItemCount() = profits.size

    inner class ProfitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val amountText: TextView = itemView.findViewById(R.id.profitAmount)
        private val noteText: TextView = itemView.findViewById(R.id.profitNote)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(profit: DailyProfit) {
            amountText.text = String.format("+%s", currencyFormat.format(profit.amount))
            noteText.text = profit.note.ifEmpty { "Без описания" }

            deleteButton.setOnClickListener {
                onDeleteClick(profit)
            }
        }
    }
}