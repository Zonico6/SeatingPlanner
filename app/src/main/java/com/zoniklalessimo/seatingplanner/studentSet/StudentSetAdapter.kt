package com.zoniklalessimo.seatingplanner.studentSet

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.zoniklalessimo.seatingplanner.R
import java.lang.Exception
import java.lang.IllegalArgumentException

class StudentSetAdapter(private val model: StudentSetViewModel) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val CLOSED_STUDENT_VIEW_TYPE = 0
        const val OPENED_STUDENT_VIEW_TYPE = 1
    }

    open class ClosedStudentVH(view: View) : RecyclerView.ViewHolder(view) {
        val name: EditText = view.findViewById(R.id.name)
        val neighbourCount: TextView = view.findViewById(R.id.neighbour_count)
        val distantCount: TextView = view.findViewById(R.id.distant_count)
    }

    class OpenStudentVH(view: View) : ClosedStudentVH(view) {
        val wishes: LinearLayout = view as LinearLayout
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
        if (holder !is ClosedStudentVH)
            throw IllegalArgumentException("ViewHolder is not a subtype of ClosedStudentVH.")

        val student = model.getStudent(position)
        holder.name.setText(student.name, TextView.BufferType.EDITABLE)
        holder.neighbourCount.text = student.neighbours.size.toString()
        holder.distantCount.text = student.distants.size.toString()

        // Are we dealing with a closed or opened holder?
        if (holder !is OpenStudentVH) { // Closed
            holder.neighbourCount.setOnClickListener {
                model.openNeighbours(position)
            }
            holder.distantCount.setOnClickListener {
                model.openNeighbours(position)
            }
        } else { // Opened
            // Display wishes
            model.getOpenedWishes(position)?.forEach {
                val inflater = LayoutInflater.from(holder.wishes.context)
                val wish = inflater.inflate(R.layout.student_wish_item, holder.wishes)
                wish.findViewById<TextView>(R.id.name).text = it
                holder.wishes.addView(wish)
            } ?: throw Exception("Student with opened holder was closed.")

            fun getChooseStudentDialogBuilder(onChosen: (name: String) -> Unit) {
                val builder = AlertDialog.Builder(holder.name.context)
                builder.setItems(model.getNames().toTypedArray()) { inter, index ->
                    val name = model.getStudent(index).name
                    onChosen(name)
                }
            }

            // The button that's not opened, opens the respective wishes, the other one adds on wish
            if (model.hasNeighboursOpened(position)) {
                holder.neighbourCount.setOnClickListener {
                    getChooseStudentDialogBuilder { name ->
                        model.appendNeighbour(position, name, true)
                    }
                }

                holder.distantCount.setOnClickListener {
                    model.openDistants(position)
                }
            } else {
                holder.distantCount.setOnClickListener {
                    getChooseStudentDialogBuilder {  name ->
                        model.appendDistant(position, name, true)
                    }
                }

                holder.neighbourCount.setOnClickListener {
                    model.openNeighbours(position)
                }
            }

            holder.name.setOnClickListener {
                model.close(position)
            }
        }
    }

    override fun getItemCount(): Int = model.studentCount

    override fun getItemViewType(position: Int): Int =
            if (model.isClosed(position))
                CLOSED_STUDENT_VIEW_TYPE
            else
                OPENED_STUDENT_VIEW_TYPE
}