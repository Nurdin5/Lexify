package com.example.lexify.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.lexify.R
import com.example.lexify.model.CalendarDay

class CalendarAdapter(
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DAY = 0
        private const val TYPE_WEEK_SEPARATOR = 1
    }

    private val items = mutableListOf<Any>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(days: List<CalendarDay>) {
        items.clear()
        days.forEachIndexed { index, day ->
            items.add(day)
            if (index < days.lastIndex && day.isEndOfWeek) {
                items.add(Unit)
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is CalendarDay -> TYPE_DAY
        else -> TYPE_WEEK_SEPARATOR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        TYPE_DAY -> DayViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_calendar_day, parent, false)
        )
        else -> WeekSeparatorViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_week_separator, parent, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DayViewHolder && items[position] is CalendarDay) {
            val day = items[position] as CalendarDay
            holder.bind(day)
            holder.itemView.setOnClickListener { onDayClick(day) }
        }
    }

    override fun getItemCount() = items.size

    inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dayOfWeekText: TextView = view.findViewById(R.id.dayOfWeekText)
        private val dateText: TextView = view.findViewById(R.id.dateText)
        private val incomeText: TextView = view.findViewById(R.id.incomeText)
        private val cardView = view as com.google.android.material.card.MaterialCardView

        fun bind(day: CalendarDay) {
            dayOfWeekText.text = day.dayOfWeek
            dateText.text = day.date.toString()
            incomeText.text = if (day.dailyIncome > 0) "${day.dailyIncome} ₽" else ""

            val context = itemView.context
            
            if (day.isCurrentDay) {
                // Current day styling
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.yellow_500)
                )
                cardView.strokeWidth = 0
                cardView.cardElevation = 4f
                
                val textColor = ContextCompat.getColor(context, R.color.black_700)
dateText.setTextColor(textColor)
                dayOfWeekText.setTextColor(textColor)
                incomeText.setTextColor(ContextCompat.getColor(context, R.color.black_700))
            } else {
                // Regular day styling
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.black_500))
                cardView.strokeWidth = 1
                cardView.strokeColor = ContextCompat.getColor(context, R.color.yellow_500)
                cardView.cardElevation = 0f
                
dateText.setTextColor(ContextCompat.getColor(context, R.color.yellow_500))
                dayOfWeekText.setTextColor(ContextCompat.getColor(context, R.color.yellow_500))
                incomeText.setTextColor(ContextCompat.getColor(context, R.color.white))
            }

            if (day.isWeekend) {
                // Keep the red color for weekends but with our theme's yellow
                val weekendColor = ContextCompat.getColor(context, R.color.yellow_200)
dayOfWeekText.setTextColor(weekendColor)
                dateText.setTextColor(weekendColor)
                incomeText.setTextColor(weekendColor)
            }
        }
    }

    inner class WeekSeparatorViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
