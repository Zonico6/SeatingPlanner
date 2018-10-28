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
import com.jmedeisis.draglinearlayout.DragLinearLayout
import com.zoniklalessimo.seatingplanner.R
import kotlinx.android.synthetic.main.student_wish_item.view.*
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

    class OpenStudentVH(view: View, val parent: RecyclerView) : ClosedStudentVH(view) {
        val wishes = view as DragLinearLayout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            CLOSED_STUDENT_VIEW_TYPE -> {
                val view = inflater.inflate(R.layout.student_closed, parent, false)
                ClosedStudentVH(view)
            }
            OPENED_STUDENT_VIEW_TYPE -> {
                val view = inflater.inflate(R.layout.student_opened, parent, false)
                OpenStudentVH(view, parent as RecyclerView)
            }
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

        setupGeneralCallbacks(holder, position)

        // Are we dealing with a closed or opened holder?
        if (holder !is OpenStudentVH) { // Closed

            setupClosedCallbacks(holder, position)

        } else { // Opened
            // Clear old wishes
            val profile = holder.wishes.getChildAt(0)
            holder.wishes.removeAllViews()
            holder.wishes.addView(profile)

            // Display new wishes
            val inflater = LayoutInflater.from(holder.wishes.context)
            // We could do the following more efficient if we passed on to addWishItem if we are dealing
            // With neighbours or distants, then we wouldn't have to check which side we are
            // In the callbacks, e.g. when swapping a wish
            model.getOpenedWishes(position)?.forEach {
                holder.addWishItem(position, it, inflater)

            } ?: throw Exception("Student with opened holder was closed.")

            setupOpenedCallbacks(holder, position, student)
        }
    }

    private fun setupGeneralCallbacks(holder: ClosedStudentVH, position: Int) {
        holder.name.setOnEditorActionListener { tv, _, _ ->
            model.renameStudent(position, tv.text.toString())
            tv.clearFocus()
            true
        }
    }

    private fun setupClosedCallbacks(holder: ClosedStudentVH, position: Int) {
        holder.neighbourCount.setOnClickListener {
            model.openNeighbours(position)
        }
        holder.distantCount.setOnClickListener {
            model.openDistants(position)
        }
    }

    private fun setupOpenedCallbacks(holder: OpenStudentVH, position: Int,
                                     student: OpenableStudent = model.getStudent(position)) {
        fun getChooseStudentDialogBuilder(onChosen: (name: String) -> Unit): AlertDialog.Builder {
            val names = model.getNames().asSequence().filter {
                !student.contains(it) && it != student.name
            }.sorted().toList().toTypedArray()

            return AlertDialog.Builder(holder.name.context).
                    setItems(names) { _, index ->
                        onChosen(names[index])
                    }
        }

        // The button that's not opened, opens the respective wishes, the other one adds on wish
        if (model.hasNeighboursOpened(position)) {
            holder.neighbourCount.setOnClickListener {
                getChooseStudentDialogBuilder { name ->
                    model.appendNeighbour(position, name, true)
                }.show()
            }

            holder.distantCount.setOnClickListener {
                model.openDistants(position)
            }
        } else {
            holder.distantCount.setOnClickListener {
                getChooseStudentDialogBuilder {  name ->
                    model.appendDistant(position, name, true)
                }.show()
            }

            holder.neighbourCount.setOnClickListener {
                model.openNeighbours(position)
            }
        }

        holder.name.setOnClickListener {
            model.close(position)
        }
    }
    private fun OpenStudentVH.addWishItem(position: Int, name: String, inflater: LayoutInflater = LayoutInflater.from(wishes.context)) {
        val wish = inflater.inflate(R.layout.student_wish_item, wishes, false)
        wish.findViewById<TextView>(R.id.name).text = name

        // Lazily because this function is called for every wish item so by doing it lazily
        // Those things only get calculated when needed, i.e. when the user presses one of the buttons
        val index by lazy {
            model.indexOf(name)
        }
        val student by lazy {
            model.getStudent(index)
        }

        wish.goto_student.setOnClickListener {
            // Scroll to the student and open the type of wishes that our wish is also in
            model.openWishes(index, model.getStudent(position).opened)
            parent.smoothScrollToPosition(index)
        }

        wish.swap_wish.setOnClickListener {
            model.swapWish(position, name, true)
        }

        wish.delete.setOnClickListener {
            model.removeOpen(position, name)
        }

        // TODO: Changing order of wishes by clicking on change order button

        wishes.addView(wish)
    }

    override fun getItemCount(): Int = model.studentCount

    override fun getItemViewType(position: Int): Int =
            if (model.isClosed(position))
                CLOSED_STUDENT_VIEW_TYPE
            else
                OPENED_STUDENT_VIEW_TYPE
}