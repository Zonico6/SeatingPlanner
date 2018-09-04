package com.zoniklalessimo.seatingplanner

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.zoniklalessimo.seatingplanner.choosing.ChooseEmptyTablePlanDialogFragment
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sample_table.setOnClickListener {
            val i = Intent(this@HomeActivity, EditEmptyPlanActivity::class.java)
            startActivity(i)
        }
    }

    fun displayChooseTablePlanDialog(view: View) {
        ChooseEmptyTablePlanDialogFragment().show(supportFragmentManager, "choose_plan_dialog")
    }
}
