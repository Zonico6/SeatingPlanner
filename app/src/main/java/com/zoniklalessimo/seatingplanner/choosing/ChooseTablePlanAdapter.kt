package com.zoniklalessimo.seatingplanner.choosing

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.zoniklalessimo.seatingplanner.R

class ChooseTablePlanAdapter(private val context: Context, private val model: ChoosePlanDialogViewModel) : BaseAdapter() {

    private data class ViewHolder(val name: TextView, val rows: TextView, val seats: TextView)

    override fun getView(i: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.choose_empty_table_plan_item, parent)

            val name = view.findViewById(R.id.name) as TextView
            val rows = view.findViewById(R.id.rows) as TextView
            val seats = view.findViewById(R.id.seats) as TextView

            holder = ViewHolder(name, rows, seats)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val entry = getItem(i) ?: return view

        holder.name.text = entry.name
        holder.rows.text = entry.rows.toString()
        holder.seats.text = entry.rows.toString()

        return view
    }

    override fun getItem(i: Int) = model.getEntries().value?.get(i)

    override fun getItemId(i: Int) = i.toLong()

    override fun getCount() = model.getEntries().value?.size ?: 0
}