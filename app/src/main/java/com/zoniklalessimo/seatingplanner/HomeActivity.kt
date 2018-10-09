package com.zoniklalessimo.seatingplanner

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.zoniklalessimo.seatingplanner.choosing.ChooseEmptyTablePlanDialogFragment
import com.zoniklalessimo.seatingplanner.choosing.ChoosePlanDialogViewModel
import com.zoniklalessimo.seatingplanner.choosing.ChoosePlanEntry
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
            val i = Intent(this@HomeActivity, EditEmptyPlanActivity::class.java)
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

class HomeActivityViewModel : ViewModel(), ChoosePlanDialogViewModel, StudentsViewModel {
    var baseDir: File? = null
        set(value) {
            if (value != null) {
                /*if( !value.exists() )
                    value.mkdirs()
                else if( !value.isDirectory && value.canWrite() ){
                    value.delete()
                    value.mkdirs()
                }*/

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

    override fun onCleared() {
        super<StudentsViewModel>.onCleared()
        super<ChoosePlanDialogViewModel>.onCleared()
    }
}

interface StudentsViewModel {
    fun onCleared() {

    }
}