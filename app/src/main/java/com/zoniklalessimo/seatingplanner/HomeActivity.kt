package com.zoniklalessimo.seatingplanner

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.zoniklalessimo.seatingplanner.tablePlan.ConstructEmptyPlanActivity
import com.zoniklalessimo.seatingplanner.tablePlan.EmptyTableView
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sample_table.setOnClickListener {
            val i = Intent(this@HomeActivity, ConstructEmptyPlanActivity::class.java)
            startActivity(i)
        }
    }
}
