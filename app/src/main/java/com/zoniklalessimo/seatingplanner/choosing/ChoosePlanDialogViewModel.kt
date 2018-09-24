package com.zoniklalessimo.seatingplanner.choosing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.io.FileReader

class ChoosePlanDialogViewModel(val src: File) : ViewModel() {
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

class ChoosePlanModelFactory(private val entrySrc: File) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChoosePlanDialogViewModel(entrySrc) as T
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