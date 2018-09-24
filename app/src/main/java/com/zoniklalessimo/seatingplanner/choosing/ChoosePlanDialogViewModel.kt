package com.zoniklalessimo.seatingplanner.choosing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.io.FileReader

class ChoosePlanDialogViewModel(val src: File, private val planDirectory: File?) : ViewModel() {
    private var entries = MutableLiveData<List<ChoosePlanEntry>>()

    init {
        entries = MutableLiveData()
        fetchEntries(src)
    }

    fun getEntries(): LiveData<List<ChoosePlanEntry>> = entries
    fun setEntries(value: List<ChoosePlanEntry>) {
        entries.value = value
    }

    fun addEntry(entry: ChoosePlanEntry) {
        entries.value = entries.value ?: listOf(entry)
    }

    fun removeEntry(entry: ChoosePlanEntry): Boolean {
        val old = entries.value
        return if (old != null && old.contains(entry)) {
            entries.value = old - entry
            true
        } else false
    }

    /**
     * Create a new TablePlan with the associated file and return said file
     */
    fun createPlan(name: String): ChoosePlanEntry {
        val plan = EmptyDataTablePlan(name, listOf(EmptyDataTable(0.5f, 0.5f, 4)), File(planDirectory, name))
        plan.save()
        val entry = ChoosePlanEntry(plan)
        addEntry(entry)
        return entry
    }

    private fun fetchEntries(file: File) {
        val input = FileReader(file)
        val content = input.readText()

        val ret = mutableListOf<ChoosePlanEntry>()

        var separatorCount = 0 // Increments for every character and every row or seat digit
        var entryString = String()
        for (c in content) {
            entryString += c
            if (separatorCount >= ChoosePlanEntry.SEPARATORS_PER_ENTRY || c == ChoosePlanEntry.ENTRY_FIELD_SEPARATOR) {
                separatorCount++
            }
            if (separatorCount == ChoosePlanEntry.SEPARATORS_PER_ENTRY + ChoosePlanEntry.ROW_DIGITS + ChoosePlanEntry.SEATS_DIGITS) {
                ret += (ChoosePlanEntry).parse(entryString, planDirectory)
                separatorCount = 0
                entryString = String()
            }
        }

        entries.value = ret
    }

}

class ChoosePlanModelFactory(private val entrySrc: File, private val planDirectory: File?) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChoosePlanDialogViewModel(entrySrc, planDirectory) as T
    }
}

data class ChoosePlanEntry(val name: String, val rows: Int, val seats: Int, val src: File) {
    constructor(plan: EmptyDataTablePlan, src: File? = plan.src) : this(plan.name, plan.tables.count(), plan.tables.sumBy { it.seatCount },
            src
                ?: throw IllegalArgumentException("No source-file argument supplied that wasn't null."))

    // Format: name|src|3-digits-rows 3-digits-seats
    companion object {
        const val SEPARATORS_PER_ENTRY = 2
        const val ENTRY_FIELD_SEPARATOR = '␟' // 'Unit separator' to separator fields
        const val ROW_DIGITS = 3
        const val SEATS_DIGITS = 3

        fun parse(input: String, planDirectory: File?): ChoosePlanEntry {
            var name: String? = null
            var src: String? = null
            var rows: Int = -1
            var seats: Int = -1

            var segment = ""
            parser@ for (c in input) {
                when {
                    src != null -> {
                        if (rows == -1 && segment.length == ROW_DIGITS)
                            rows = segment.toInt()
                        else if (seats == -1 && segment.length == SEATS_DIGITS)
                            seats = segment.toInt()
                        else {
                            segment += c
                            continue@parser
                        }
                        segment = c.toString()
                    }
                    c == ENTRY_FIELD_SEPARATOR -> {
                        if (name == null)
                            name = segment
                        else
                            src = segment
                        segment = String()
                    }
                    else -> segment += c
                }
            }
            return ChoosePlanEntry(name as String, rows, seats, if (planDirectory == null) File(src as String) else File(planDirectory, src as String))
        }
    }

    /**
     * Parses the file until it finds an entry that matches the src file of this entry and
     * then updates it's properties with those it finds in the file.
     */
    fun update(file: File): Boolean {
        TODO()
    }
}