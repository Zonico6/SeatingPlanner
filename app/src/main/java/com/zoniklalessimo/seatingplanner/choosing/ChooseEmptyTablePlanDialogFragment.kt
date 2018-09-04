package com.zoniklalessimo.seatingplanner.choosing

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.zoniklalessimo.seatingplanner.R

class ChooseEmptyTablePlanDialogFragment : DialogFragment() {
    private val model: ChoosePlanDialogViewModel = ViewModelProviders.of(this).get(ChoosePlanDialogViewModel::class.java)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val adapter = ChooseTablePlanAdapter(context!!, model)
        model.getEntries().observe(this, Observer {
            adapter.notifyDataSetChanged()
        })
        val builder = AlertDialog.Builder(context!!).setAdapter(adapter) { _, i ->
            val src = model.getEntries().value!![i].src
            val intent = Intent()
            intent.putExtra(context!!.getString(R.string.edit_table_plan_extra), EmptyDataTablePlan(src))
            startActivity(intent)
        }
        return builder.create()
    }
}