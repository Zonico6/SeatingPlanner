package com.zoniklalessimo.seatingplanner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zoniklalessimo.seatingplanner.studentSet.OpenableStudent
import com.zoniklalessimo.seatingplanner.studentSet.StudentSetAdapter
import com.zoniklalessimo.seatingplanner.studentSet.StudentSetViewModel
import java.util.*

class EditStudentSetActivity : AppCompatActivity() {

    private lateinit var model: StudentSetViewModel
    private lateinit var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recycler = RecyclerView(this)
        setContentView(recycler)

        model = ViewModelProviders.of(this).get(StudentSetViewModel::class.java)

        val layoutManager = LinearLayoutManager(this)
        adapter = StudentSetAdapter(model)

        model.bindWithAdapter(this, adapter)

        model.addStudent(this, adapter, OpenableStudent("Annie", emptyArray(), emptyArray(), OpenableStudent.CLOSED))

        recycler.let {
            it.adapter = adapter
            it.layoutManager = layoutManager
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_student_set_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.add_student -> {
                model.addStudent(this, adapter, OpenableStudent("Annie" + Random().nextInt().toString().substring(0..4), emptyArray(), emptyArray(), OpenableStudent.CLOSED))
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
