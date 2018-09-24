package com.zoniklalessimo.seatingplanner.choosing

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.zoniklalessimo.seatingplanner.R

class ChooseTablePlanAdapter(private val context: Context, private val model: ChoosePlanDialogViewModel) : BaseAdapter() {

    private data class EntryViewHolder(val name: TextView, val rows: TextView, val seats: TextView)
    private data class AddEntryViewHolder(val add: ImageButton, val title: EditText)

    override fun getView(i: Int, convertView: View?, parent: ViewGroup?): View {
        return when (getItemViewType(i)) {
            0 -> getEntryView(i, convertView, parent)
            1 -> getAddEntryView(convertView, parent)
            else -> throw IllegalStateException("Received unknown ItemViewType.")
        }
    }

    private fun getEntryView(i: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: EntryViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.choose_empty_table_plan_item, parent)

            val name = view.findViewById(R.id.name) as TextView
            val rows = view.findViewById(R.id.rows) as TextView
            val seats = view.findViewById(R.id.seats) as TextView

            holder = EntryViewHolder(name, rows, seats)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as EntryViewHolder
        }

        val entry = getItem(i) ?: return view

        holder.name.text = entry.name
        holder.rows.text = entry.rows.toString()
        holder.seats.text = entry.rows.toString()

        return view
    }

    private fun getAddEntryView(convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: AddEntryViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.choose_empty_table_plan_item, parent)

            val add = view.findViewById(R.id.add_entry) as ImageButton
            val title = view.findViewById(R.id.title_text) as EditText

            holder = AddEntryViewHolder(add, title)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as AddEntryViewHolder
        }

        holder.add.setOnClickListener {
            model.addEntry()
        }

        return view
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == count - 1) 0 else 1
    }

    override fun getViewTypeCount() = 2

    override fun getItem(i: Int) = model.getEntries().value?.get(i)

    override fun getItemId(i: Int) = i.toLong()

    override fun getCount() = model.getEntries().value?.size ?: 0
}