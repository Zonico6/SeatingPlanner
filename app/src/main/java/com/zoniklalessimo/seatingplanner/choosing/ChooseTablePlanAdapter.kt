package com.zoniklalessimo.seatingplanner.choosing

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView

class ChooseTablePlanAdapter(private val context: Context, private val model: ChoosePlanDialogViewModel) : BaseAdapter() {
    companion object {
        const val WITH_ADD_ENTRY_ITEM = false

        const val VIEW_TYPE_SHOW = 0
        const val VIEW_TYPE_ADD = 1
    }

    private data class EntryViewHolder(val name: TextView, val rows: TextView, val seats: TextView)
    private data class AddEntryViewHolder(val add: ImageButton, val title: EditText)

    override fun getView(i: Int, convertView: View?, parent: ViewGroup?): View {
        return when (getItemViewType(i)) {
            VIEW_TYPE_SHOW -> getEntryView(i, convertView, parent)
            VIEW_TYPE_ADD -> getAddEntryView(convertView, parent)
            else -> throw Exception("The index supplied yielded an unexpected ItemViewType.")
        }
    }

    private fun getEntryView(i: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: EntryViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.choose_empty_table_plan_item, parent, false)

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
        holder.seats.text = entry.seats.toString()

        return view
    }

    private fun getAddEntryView(convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: AddEntryViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.add_entry_item, parent, false)

            val add = view.findViewById(R.id.add_entry) as ImageButton
            val title = view.findViewById(R.id.title_text) as EditText

            holder = AddEntryViewHolder(add, title)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as AddEntryViewHolder
        }

        holder.add.setOnClickListener(
                ChooseEmptyTablePlanDialogFragment.OnAddPlanListener(context, model, holder.title))

        return view
    }

    override fun getItemViewType(position: Int): Int =
            if (!WITH_ADD_ENTRY_ITEM || position != count - 1) VIEW_TYPE_SHOW
            else VIEW_TYPE_ADD

    override fun getViewTypeCount() = if (WITH_ADD_ENTRY_ITEM) 2 else 1

    override fun getItem(i: Int) = model.getEntries().value?.get(i)

    override fun getItemId(i: Int) = i.toLong()

    // Number of items, plus one if we should show the item to add an entry
    override fun getCount() = (model.getEntries().value?.size
        ?: 0) + if (WITH_ADD_ENTRY_ITEM) 1 else 0
}