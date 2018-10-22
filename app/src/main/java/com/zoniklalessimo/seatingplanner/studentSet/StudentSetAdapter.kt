package com.zoniklalessimo.seatingplanner.studentSet

import android.service.autofill.TextValueSanitizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.zoniklalessimo.seatingplanner.R
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

class StudentSetAdapter(private val model: StudentSetViewModel) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val CLOSED_STUDENT_VIEW_TYPE = 0
        const val OPENED_STUDENT_VIEW_TYPE = 1
    }

    class ClosedStudentVH(view: View) : RecyclerView.ViewHolder(view) {
        val nameTV: EditText = view.findViewById(R.id.name)
        init {

        }
    }

    class OpenStudentVH(convertView: View) : RecyclerView.ViewHolder(convertView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.student_closed, parent, false)

        return when (viewType) {
            CLOSED_STUDENT_VIEW_TYPE -> ClosedStudentVH(view)
            OPENED_STUDENT_VIEW_TYPE -> OpenStudentVH(view)
            else -> throw IllegalArgumentException("Unknown View Type received.")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    }

    override fun getItemCount(): Int = model.studentCount

    override fun getItemViewType(position: Int): Int =
            if (model.isClosed(position))
                CLOSED_STUDENT_VIEW_TYPE
            else
                OPENED_STUDENT_VIEW_TYPE
}