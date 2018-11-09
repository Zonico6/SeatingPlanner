package com.zoniklalessimo.seatingplanner.choosingStudentSet

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.zoniklalessimo.seatingplanner.EditStudentSetActivity
import com.zoniklalessimo.seatingplanner.HomeActivityViewModel
import com.zoniklalessimo.seatingplanner.R
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.OnAddItemListener
import com.zoniklalessimo.seatingplanner.core.seating.Student

class ChooseStudentSetDialogFragment : DialogFragment() {
    class OnAddSetListener(context: Context?, model: ChooseStudentSetViewModel, title: EditText) :
            OnAddItemListener(context, title, { _title -> model.createSet(_title) })

    private lateinit var model: ChooseStudentSetViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            model = ViewModelProviders.of(it).get(HomeActivityViewModel::class.java)
        } ?: throw Exception("Can't retrieve activity to construct View Model.")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val adapter = ChooseStudentSetAdapter(model)

        model.getStudentSets().observe(this, Observer {
            adapter.notifyDataSetChanged()
        })

        val builder = AlertDialog.Builder(context!!).setAdapter(adapter) { _, index ->
            val set = model.getStudentSets().value!![index]
            val i = Intent(context, EditStudentSetActivity::class.java)

            val students = Array(set.studentsLength()) {
                val student = set.students(it)
                val name = student.name()
                val neighbours = Array<String>(student.neighboursLength(), student::neighbours)
                val distants = Array<String>(student.distantsLength(), student::distants)
                Student(name, neighbours, distants)
            }
            i.putExtra(getString(R.string.students_extra), students)
            i.putExtra(getString(R.string.name_extra), set.name())
            i.putExtra(getString(R.string.src_extra), set.src())
            startActivity(i)
        }

        if (ChooseStudentSetAdapter.WITH_ADD_SET_ITEM)
            builder.setTitle(R.string.choose_student_set_dialog_title)
        else
            setupAddSetTitle(builder)

        return builder.create()
    }

    @SuppressLint("InflateParams")
    private fun setupAddSetTitle(builder: AlertDialog.Builder) {
        val titleView = activity!!.layoutInflater.inflate(R.layout.add_student_set_title, null)

        val createBtn = titleView.findViewById(R.id.add_student_set) as ImageButton
        val setTitle = titleView.findViewById(R.id.title_text) as EditText
        createBtn.setOnClickListener(OnAddSetListener(context, model, setTitle))

        builder.setCustomTitle(titleView)
    }

    override fun onResume() {
        super.onResume()
        dialog.window.clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    }
}