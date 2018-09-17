package com.zoniklalessimo.seatingplanner.choosing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.FileReader
import kotlin.properties.Delegates.observable

class ChoosePlanDialogViewModel : ViewModel() {

    private var entries = MutableLiveData<List<ChoosePlanEntry>>()

    fun getEntries(): LiveData<List<ChoosePlanEntry>> = entries

    var src: File? by observable(null as File?) { _, _, new ->
        if (new != null)
            fetchEntries(new)
        else
            entries = MutableLiveData()
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
                ret += (ChoosePlanEntry).parse(entryString)
                separatorCount = 0
                entryString = String()
            }
        }

        entries.value = ret
    }
}

data class ChoosePlanEntry(val name: String, val rows: Int, val seats: Int, val src: File) {
    // Format: name|src|3-digits-rows 3-digits-seats
    companion object {
        const val SEPARATORS_PER_ENTRY = 2
        const val ENTRY_FIELD_SEPARATOR = '␟' // 'Unit separator' to separator fields
        const val ROW_DIGITS = 3
        const val SEATS_DIGITS = 3

        fun parse(input: String): ChoosePlanEntry {
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
            return ChoosePlanEntry(name as String, rows, seats, File(src as String))
        }
    }
}