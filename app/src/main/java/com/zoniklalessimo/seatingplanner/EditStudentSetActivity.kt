package com.zoniklalessimo.seatingplanner

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zoniklalessimo.seatingplanner.studentSet.OpenableStudent
import com.zoniklalessimo.seatingplanner.studentSet.StudentSetAdapter
import com.zoniklalessimo.seatingplanner.studentSet.StudentSetViewModel
import java.io.File

typealias CoreStudent = com.zoniklalessimo.seatingplanner.core.seating.Student

class EditStudentSetActivity : AppCompatActivity() {

    private lateinit var model: StudentSetViewModel
    private lateinit var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = intent.getStringExtra(getString(R.string.name_extra))
        val src = File(intent.getStringExtra(getString(R.string.src_extra)))
        @Suppress("UNCHECKED_CAST")
        val students = intent.getSerializableExtra(getString(R.string.students_extra)) as Array<CoreStudent>

        val recycler = RecyclerView(this)
        setContentView(recycler)

        // Do not separate these lines. They are initializing mandatory values
        model = ViewModelProviders.of(this).get(StudentSetViewModel::class.java)
        model.initName(name)
        model.initSrc(src)
        model.initStudents(students)

        adapter = StudentSetAdapter(model)
        // Without this line, all students added before won't be recognized.
        model.bindWithAdapter(this, adapter)

        // model.getSrc().observe(this, Observer { })
        model.getName().observe(this, Observer {
            title = it
        })

        val layoutManager = LinearLayoutManager(this)

       recycler.let {
            it.adapter = adapter
            it.layoutManager = layoutManager
        }
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    fun save() {
        model.save()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.edit_student_set_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.add_student -> {
                model.addStudent(this, adapter, OpenableStudent(String(),
                        emptyArray(), emptyArray(), OpenableStudent.CLOSED))
            }
            R.id.save -> {
                save()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
