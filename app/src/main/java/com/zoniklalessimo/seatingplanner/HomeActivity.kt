package com.zoniklalessimo.seatingplanner

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.ChooseEmptyTablePlanDialogFragment
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.ChoosePlanDialogViewModel
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.ChoosePlanEntry
import com.zoniklalessimo.seatingplanner.choosingStudentSet.ChooseStudentSetViewModel
import com.zoniklalessimo.seatingplanner.schema.StudentSet
import com.zoniklalessimo.seatingplanner.studentSet.StudentSetViewModel
import kotlinx.android.synthetic.main.activity_home.*
import java.io.File

class HomeActivity : AppCompatActivity() {

    lateinit var model: HomeActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        model = ViewModelProviders.of(this).get(HomeActivityViewModel::class.java)
        model.baseDir = dataDir

        sample_table.setOnClickListener {
            val i = Intent(this, EditStudentSetActivity::class.java)
            startActivity(i)
        }
    }

    fun displayChooseTablePlanDialog(view: View) {
        window.setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

        with(ChooseEmptyTablePlanDialogFragment()) {
            show(supportFragmentManager, "choose_plan_dialog")
        }
    }
}

interface ChooseDialogViewModel {
    fun createNewItem(title: String)
}

// Next time, make the view models their own classes and merge them somehow.
// Forcing them to interfaces cuts a lot of possibilities and makes it more complex than it needs to be.
class HomeActivityViewModel : ViewModel(), ChoosePlanDialogViewModel, ChooseStudentSetViewModel {
    override val studentSets: MutableLiveData<List<StudentSet>> = MutableLiveData()

    var baseDir: File? = null
        set(value) {
            if (value != null) {
                value.delete()

                emptyPlanDir = File(value, "emptyPlans")
                emptyPlanDir.delete()
                if (!emptyPlanDir.exists())
                    emptyPlanDir.mkdir()

                emptyPlanEntries = File(value, "emptyPlanEntries.txt")
                emptyPlanEntries.delete()
                if (!emptyPlanEntries.exists())
                    emptyPlanEntries.createNewFile()
            }
        }

    override lateinit var emptyPlanDir: File
    override lateinit var emptyPlanEntries: File
    override val entries: MutableLiveData<List<ChoosePlanEntry>> = MutableLiveData()

    fun deleteEmptyPlans() {
        emptyPlanDir.listFiles().forEach { it.delete() }
        emptyPlanEntries.delete()
        emptyPlanEntries.createNewFile()
    }

    override fun createNewItem(title: String) {
        throw NotImplementedError("You can't invoke createNewItem on HomeActivityViewModel. " +
                "Instead, cast to your desired ViewModel and invoke it on that.")
    }

    override fun onCleared() {
        super<ChooseStudentSetViewModel>.onCleared()
        super<ChoosePlanDialogViewModel>.onCleared()
    }
}