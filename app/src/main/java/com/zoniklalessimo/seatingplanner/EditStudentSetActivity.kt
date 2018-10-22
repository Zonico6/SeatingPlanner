package com.zoniklalessimo.seatingplanner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zoniklalessimo.seatingplanner.studentSet.StudentSetAdapter
import com.zoniklalessimo.seatingplanner.studentSet.StudentSetViewModel

class EditStudentSetActivity : AppCompatActivity() {

    private lateinit var model: StudentSetViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recycler = RecyclerView(this)
        setContentView(recycler)

        model = ViewModelProviders.of(this).get(StudentSetViewModel::class.java)

        val layoutManager = LinearLayoutManager(this)
        val adapter = StudentSetAdapter(model)

        model.bindWithAdapter(this, adapter)

        recycler.let {
            it.adapter = adapter
            it.layoutManager = layoutManager
        }
    }
}
