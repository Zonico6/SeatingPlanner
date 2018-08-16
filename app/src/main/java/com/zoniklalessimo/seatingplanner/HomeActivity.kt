package com.zoniklalessimo.seatingplanner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zoniklalessimo.seatingplanner.tablePlan.ConstructEmptyPlanActivity
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
