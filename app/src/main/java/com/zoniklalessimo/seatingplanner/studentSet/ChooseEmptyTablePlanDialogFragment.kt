package com.zoniklalessimo.seatingplanner.studentSet

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.zoniklalessimo.seatingplanner.*
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.ChoosePlanDialogViewModel
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.ChoosePlanEntry
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.EmptyDataTablePlan
import java.io.File
import java.io.Serializable

class ChooseEmptyTablePlanDialogFragment : DialogFragment() {

    private lateinit var model: StudentSetViewModel
    private val entries = ChoosePlanDialogViewModel.loadEntries(
            File(HomeActivityViewModel.baseDirectory, HomeActivityViewModel.EMPTY_PLAN_ENTRY_FILE),
            File(HomeActivityViewModel.baseDirectory, HomeActivityViewModel.EMPTY_PLANS_DIR_NAME)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            model = ViewModelProviders.of(it).get(StudentSetViewModel::class.java)
        } ?: throw Exception("Can't retrieve activity to construct View Model.")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val adapter = DisplayEntriesAdapter(entries)

        val builder = AlertDialog.Builder(context!!).setAdapter(adapter) { _, i ->
            val src = entries[i].src
            val intent = Intent(context, AssignRowsActivity::class.java).apply {
                putExtra(StudentsExtraKey, model.getStudents() as Serializable)
                putExtra(context!!.getString(R.string.table_plan_extra), EmptyDataTablePlan.fromSaveFile(src))
            }

            startActivity(intent)
        }

        return builder.create()
    }
}

class DisplayEntriesAdapter(val entries: List<ChoosePlanEntry>) : BaseAdapter() {
    private data class EntryViewHolder(val name: TextView, val rows: TextView, val seats: TextView)

    override fun getView(i: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: EntryViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(parent.context)
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

    override fun getCount() = entries.size
    override fun getItem(i: Int) = entries[i]
    override fun getItemId(i: Int) = i.toLong()
}