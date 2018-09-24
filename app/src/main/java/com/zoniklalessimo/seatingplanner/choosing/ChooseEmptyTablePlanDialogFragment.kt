package com.zoniklalessimo.seatingplanner.choosing

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.zoniklalessimo.seatingplanner.EditEmptyPlanActivity
import com.zoniklalessimo.seatingplanner.R
import java.io.File

class ChooseEmptyTablePlanDialogFragment : DialogFragment() {
    private val model: ChoosePlanDialogViewModel = run {
        val factory = ChoosePlanModelFactory(
                arguments!![getString(R.string.entry_file_extra)] as File,
                arguments!![getString(R.string.empty_table_plan_dir)] as File)

        ViewModelProviders.of(this, factory).get(ChoosePlanDialogViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val adapter = ChooseTablePlanAdapter(context!!, model)
        model.getEntries().observe(this, Observer {
            adapter.notifyDataSetChanged()
        })
        val builder = AlertDialog.Builder(context!!).setAdapter(adapter) { _, i ->
            val src = model.getEntries().value!![i].src
            val intent = Intent(context, EditEmptyPlanActivity::class.java)
            intent.putExtra(context!!.getString(R.string.table_plan_extra), EmptyDataTablePlan.fromSaveFile(src))
            startActivity(intent)
        }
        return builder.create()
    }
}