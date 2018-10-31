package com.zoniklalessimo.seatingplanner.choosingStudentSet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.zoniklalessimo.seatingplanner.R

class ChooseStudentSetAdapter(private val model: ChooseStudentSetViewModel) : BaseAdapter() {
    companion object {
        const val WITH_ADD_SET_ITEM = false

        const val VIEW_TYPE_SHOW = 0
        const val VIEW_TYPE_ADD = 1
    }

    private data class StudentSetViewHolder(val name: TextView, val studentCount: TextView)
    private data class AddStudentSetViewHolder(val add: ImageButton, val title: EditText)

    override fun getView(i: Int, convertView: View?, parent: ViewGroup): View {
        return when (getItemViewType(i)) {
            VIEW_TYPE_SHOW -> getStudentSetView(i, convertView, parent)
            VIEW_TYPE_ADD -> getAddSetView(convertView, parent)
            else -> throw Exception("The index supplied yielded an unexpected ItemViewType.")
        }
    }

    private fun getStudentSetView(i: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: StudentSetViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(parent.context)
            view = inflater.inflate(R.layout.choose_student_set_item, parent, false)

            val name = view.findViewById(R.id.name) as TextView
            val studentCount = view.findViewById(R.id.student_count) as TextView

            holder = StudentSetViewHolder(name, studentCount)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as StudentSetViewHolder
        }

        val studentSet = getItem(i) ?: return view

        holder.name.text = studentSet.title()
        holder.studentCount.text = studentSet.studentsLength().toString()

        return view
    }

    private fun getAddSetView(convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: AddStudentSetViewHolder
        if (convertView == null) {
            val inflater = LayoutInflater.from(parent.context)
            view = inflater.inflate(R.layout.add_student_set_item, parent, false)

            val add = view.findViewById(R.id.add_student) as ImageButton
            val title = view.findViewById(R.id.title_text) as EditText

            holder = AddStudentSetViewHolder(add, title)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as AddStudentSetViewHolder
        }

        holder.add.setOnClickListener(
                ChooseStudentSetDialogFragment.OnAddSetListener(parent.context, model, holder.title))

        return view
    }

    override fun getItemViewType(position: Int): Int =
            if (!WITH_ADD_SET_ITEM || position != count - 1) VIEW_TYPE_SHOW
            else VIEW_TYPE_ADD

    override fun getViewTypeCount() = if (WITH_ADD_SET_ITEM) 2 else 1

    override fun getItem(i: Int) = model.getSetAt(i)

    override fun getItemId(i: Int) = i.toLong()

    override fun getCount() = model.setsCount + viewTypeCount - 1
}