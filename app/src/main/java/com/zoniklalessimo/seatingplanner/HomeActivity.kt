package com.zoniklalessimo.seatingplanner

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.ChooseEmptyTablePlanDialogFragment
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.ChoosePlanDialogViewModel
import com.zoniklalessimo.seatingplanner.choosingEmptyPlan.ChoosePlanEntry
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
            model.deleteStudentSets()
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
    override val studentSets: MutableLiveData<List<StudentSet>> = MutableLiveData()
    override lateinit var studentSetDir: File

    var baseDir: File? = null
        set(value) {
            if (value != null) {
                studentSetDir = File(value, "studentSets")
                if (!studentSetDir.exists())
                    studentSetDir.mkdir()

                emptyPlanDir = File(value, "emptyPlans")
                if (!emptyPlanDir.exists())
                    emptyPlanDir.mkdir()

                emptyPlanEntries = File(value, "emptyPlanEntries.txt")
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

    fun deleteStudentSets() {
        studentSetDir.listFiles().forEach { it.delete() }
    }

    override fun onCleared() {
        super<ChoosePlanDialogViewModel>.onCleared()
    }
}