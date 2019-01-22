package com.zoniklalessimo.seatingplanner

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.*
import com.zoniklalessimo.seatingplanner.choosingStudentSet.ChooseStudentSetDialogFragment
import com.zoniklalessimo.seatingplanner.choosingStudentSet.ChooseStudentSetViewModel
import com.zoniklalessimo.seatingplanner.schema.StudentSet
import kotlinx.android.synthetic.main.activity_home.*
import java.io.File

class HomeActivity : AppCompatActivity() {

    lateinit var model: HomeActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Don't separate these two lines, the second one initializes a necessary field
        model = ViewModelProviders.of(this).get(HomeActivityViewModel::class.java)
        model.baseDir = dataDir

        sample_table.setOnClickListener {
            Log.d("-----------", "--------------")
            for (entry in model.entries.value!!) {
                Log.d("Entry", entry.toSaveString())
            }
            Log.d("-----------", "----------------")
            for (table in EmptyDataTablePlan.fromSaveFile(
                    model.emptyPlanDir.listFiles().first()).tables) {
                Log.d("Table", "x: ${table.xBias}, y: ${table.yBias}, seats: ${table.seatCount}")
            }
        }

        delete_student_sets.setOnClickListener {
            model.deleteStudentSets()
        }
        delete_empty_plans.setOnClickListener {
            model.deleteEmptyPlans()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun displayChooseStudentSetDialog(view: View) {
        with (ChooseStudentSetDialogFragment()) {
            show(supportFragmentManager ,"choose_set_dialog")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun displayChooseTablePlanDialog(view: View) {
        with (ChooseEmptyTablePlanDialogFragment()) {
            show(supportFragmentManager, "choose_plan_dialog")
        }
    }
}

class HomeActivityViewModel : ViewModel(), ChoosePlanDialogViewModel, ChooseStudentSetViewModel {
    companion object {
        const val EMPTY_PLAN_ENTRY_FILE = "emptyPlanEntries.txt"
        const val EMPTY_PLANS_DIR_NAME = "emptyPlans"
        const val STUDENT_SETS_DIR_NAME = "studentSets"

        lateinit var baseDirectory: File
    }

    var baseDir: File? = null
        set(value) {
            if (value != null) {
                baseDirectory = value

                studentSetDir = File(value, STUDENT_SETS_DIR_NAME)
                if (!studentSetDir.exists())
                    studentSetDir.mkdir()

                emptyPlanDir = File(value, EMPTY_PLANS_DIR_NAME)
                if (!emptyPlanDir.exists())
                    emptyPlanDir.mkdir()

                emptyPlanEntries = File(value, EMPTY_PLAN_ENTRY_FILE)
                if (!emptyPlanEntries.exists())
                    emptyPlanEntries.createNewFile()
            }
        }

    override val studentSets: MutableLiveData<List<StudentSet>> = MutableLiveData()
    override lateinit var studentSetDir: File

    override lateinit var emptyPlanDir: File
    override lateinit var emptyPlanEntries: File
    override val entries: MutableLiveData<List<ChoosePlanEntry>> = MutableLiveData()

    fun deleteEmptyPlans() {
        emptyPlanDir.listFiles().forEach { it.delete() }
        emptyPlanEntries.delete()
        emptyPlanEntries.createNewFile()
    }

    fun deleteStudentSets() {
        studentSetDir.listFiles().forEach { it.delete() }
    }

    override fun onCleared() {
        super<ChoosePlanDialogViewModel>.onCleared()
    }
}