package com.zoniklalessimo.seatingplanner.choosing

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.zoniklalessimo.seatingplanner.EditEmptyPlanActivity
import com.zoniklalessimo.seatingplanner.HomeActivityViewModel
import com.zoniklalessimo.seatingplanner.R

class ChooseEmptyTablePlanDialogFragment : DialogFragment() {
    class OnAddPlanListener(val context: Context?, private val model: ChoosePlanDialogViewModel, val title: EditText) : View.OnClickListener {
        override fun onClick(v: View?) {
            val name = title.text.toString()
            if (name.isNotBlank()) {
                model.createPlan(name)
            } else {
                Toast.makeText(context, R.string.no_blank_or_empty_title, Toast.LENGTH_LONG).show()
            }
            title.text = null
        }
    }

    private lateinit var model: ChoosePlanDialogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            model = ViewModelProviders.of(it).get(HomeActivityViewModel::class.java)
        } ?: throw Exception("Can't retrieve activity to construct View Model.")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreate(savedInstanceState)

        val adapter = ChooseTablePlanAdapter(model)

        model.getEntries().observe(this, Observer {
            adapter.notifyDataSetChanged()
        })

        val builder = AlertDialog.Builder(context!!).setAdapter(adapter) { _, i ->
            val src = model.getEntries().value!![i].src
            val intent = Intent(context, EditEmptyPlanActivity::class.java).apply {
                putExtra(context!!.getString(R.string.entry_file_extra), model.emptyPlanEntries)
                putExtra(context!!.getString(R.string.table_plan_extra), EmptyDataTablePlan.fromSaveFile(src))
            }

            startActivity(intent)
        }

        if (ChooseTablePlanAdapter.WITH_ADD_ENTRY_ITEM)
            builder.setTitle(R.string.choose_empty_plan_dialog_title)
        else
            setupAddEntryTitle(builder)

        return builder.create()
    }

    @SuppressLint("InflateParams")
    private fun setupAddEntryTitle(builder: AlertDialog.Builder) {
        val titleView = activity!!.layoutInflater.inflate(R.layout.add_entry_title, null)

        val createBtn = titleView.findViewById(R.id.add_entry) as ImageButton
        val planTitle = titleView.findViewById(R.id.title_text) as EditText
        createBtn.setOnClickListener(OnAddPlanListener(context, model, planTitle))

        builder.setCustomTitle(titleView)
    }

    override fun onResume() {
        super.onResume()
        dialog.window.clearFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    }
}