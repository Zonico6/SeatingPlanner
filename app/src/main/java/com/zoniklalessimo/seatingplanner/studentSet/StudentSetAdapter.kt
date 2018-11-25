package com.zoniklalessimo.seatingplanner.studentSet

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import com.jmedeisis.draglinearlayout.DragLinearLayout
import com.zoniklalessimo.seatingplanner.R
import kotlinx.android.synthetic.main.student_wish_item.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

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
        fun getChooseStudentDialogBuilder(onChosen: (name: String) -> Unit): AlertDialog.Builder? {
            val names = model.getNames().asSequence().filter {
                !student.contains(it) && it != student.name
            }.sorted().toList().toTypedArray()

            if (names.isEmpty())
                return null

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
                }?.show()
            }

            holder.distantCount.setOnClickListener {
                model.openDistants(position)
            }
        } else {
            holder.distantCount.setOnClickListener {
                getChooseStudentDialogBuilder {  name ->
                    model.appendDistant(position, name, true)
                }?.show()
            }

            holder.neighbourCount.setOnClickListener {
                model.openNeighbours(position)
            }
        }

        holder.name.setOnClickListener {
            model.close(position)
        }
    }
    private fun OpenStudentVH.addWishItem(position: Int, name: String,
                                          inflater: LayoutInflater = LayoutInflater.from(wishes.context)) {
        val wish = inflater.inflate(R.layout.student_wish_item, wishes, false)
        wish.findViewById<TextView>(R.id.name).text = name

        // Lazily because this function is called for every wish item so by doing it lazily
        // Those things only get calculated when needed, i.e. when the user presses one of the buttons
        val wishIndex by lazy {
            model.indexOf(name)
        }

        wish.goto_student.setOnClickListener {
            // Scroll to the student and open the type of wishes that our wish is also in
            if (model.hasNeighboursOpened(wishIndex))
                model.openNeighbours(wishIndex)
            else
                model.openDistants(wishIndex)

            parent.smoothScrollToPosition(wishIndex)
        }

        wish.swap_wish.setOnClickListener {
            model.swapWish(position, name, true)
        }

        wish.delete.setOnClickListener {
            model.removeOpen(position, name)
        }

        wishes.addDragView(wish, wish.change_order)

        // Unfortunately, we have no means of detecting when a drag has ended. However we only want to
        // Update our view model when that's the case so we save the changes but don't interrupt the
        // Dragging whenever two items switch positions and the listener is called.
        // Therefore, as soon as the drag begins, i.e. the listener is first called, we set up a thread
        // That checks every other moment if we have ended the drag and if so, we update our model.
        var checkingDragEnded = false
        val checkDragEndedMaxRepetitions = 25
        val checkDragEndedIntervalMs = 650L

        wishes.setOnViewSwapListener { firstView, _, _, _ ->
            // Probably best not to use GlobalScope in the end -> acquire on how to run launch on Ui
            // Thread and then change it accordingly
            if (checkingDragEnded)
                return@setOnViewSwapListener
            checkingDragEnded = true

            GlobalScope.launch {

                for (i in 0 .. checkDragEndedMaxRepetitions) {
                    delay(checkDragEndedIntervalMs)

                    if (firstView.visibility == View.VISIBLE) // Drag ended
                        break
                }

                val newWishes = Array(model.getStudent(position).openWishes?.size ?: 0) {
                    (wishes[it + 1] as LinearLayout).name.text.toString()
                }
                model.postStudent(position,
                        model.getStudent(position).
                                withOpenWishes(newWishes))

                checkingDragEnded = false
            }
        }

        /* var checkDragEndedThread = null as Thread?

        wishes.setOnViewSwapListener { firstView, _, _, _ ->
            checkDragEndedThread = checkDragEndedThread ?: thread {

                for (i in 0..checkDragEndedMaxRepetitions) {
                    Thread.sleep(checkDragEndedIntervalMs)
                    if (firstView.visibility == View.VISIBLE) // Drag ended
                        break
                }
                val newWishes = Array(model.getStudent(position).openWishes?.size ?: 0) {
                    (wishes[it + 1] as LinearLayout).name.text.toString()
                }
                model.postStudent(position,
                        model.getStudent(position).
                                withOpenWishes(newWishes))
            }
        }*/
    }

    override fun getItemCount(): Int = model.studentCount

    override fun getItemViewType(position: Int): Int =
            if (model.isClosed(position))
                CLOSED_STUDENT_VIEW_TYPE
            else
                OPENED_STUDENT_VIEW_TYPE
}