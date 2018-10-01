package com.zoniklalessimo.seatingplanner.choosing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.RandomAccessFile

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

    override fun onCleared() {
        val builder = StringBuilder(entries.value?.size ?: 0 * 30)
        entries.value?.forEach {
            builder.append(it.toSaveString())
        }
        val output = FileOutputStream(src)
        output.write(builder.toString().toByteArray())
        output.close()
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
    // Example: Old Room␟old_room_plan.txt␟004015
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

    fun toSaveString(): String {
        val builder = StringBuilder(name.length + src.name.length + SEPARATORS_PER_ENTRY + ROW_DIGITS + SEATS_DIGITS)
        builder.append(name)
        builder.append(ENTRY_FIELD_SEPARATOR)
        builder.append(src.name)
        builder.append(ENTRY_FIELD_SEPARATOR)
        var rowsString = rows.toString()
        when (rowsString.length) {
            0 -> builder.append("000")
            1 -> builder.append("00")
            2 -> builder.append('0')
            3 -> {
            }
            else -> {
                builder.append("999")
                rowsString = String()
            }
        }
        builder.append(rowsString)
        var seatsString = seats.toString()
        when (seatsString.length) {
            0 -> builder.append("000")
            1 -> builder.append("00")
            2 -> builder.append('0')
            3 -> {
            }
            else -> {
                builder.append("999")
                seatsString = String()
            }
        }
        builder.append(seatsString)
        return builder.toString()
    }

    /**
     * Parses the file until it finds an entry that matches the src file of this entry and
     * then updates it's properties with those it finds in the file.
     */
    fun update(file: File): Boolean {
        val access = RandomAccessFile(file, "rw")
        val bytes = ByteArray(access.length().toInt())
        access.read(bytes)

        val location = findInstance(bytes)
        return update(access, location)
    }

    fun update(file: File, offset: Int): Boolean {
        val access = RandomAccessFile(file, "rw")
        val bytes = ByteArray(access.length().toInt())
        access.read(bytes)

        var length = 0
        var sepCount = 0
        for (b in bytes.slice(offset..access.length().toInt())) {
            if (b.toChar() == ENTRY_FIELD_SEPARATOR) {
                sepCount++
                if (sepCount == SEPARATORS_PER_ENTRY) {
                    length += SEATS_DIGITS + ROW_DIGITS
                    break
                }
            }
            length++
        }

        return update(access, FileEntry(offset, length))
    }

    fun update(file: File, offset: Int, length: Int) =
            update(RandomAccessFile(file, "rw"), FileEntry(offset, length))

    /**
     * @return True if the new entry was as long as the old. False if otherwise,
     * then the entire content of the file afterwards was rewritten as well.
     */
    private fun update(access: RandomAccessFile, location: FileEntry, close: Boolean = true): Boolean {
        val entryString = toSaveString()
        val ret = if (location.length == entryString.length) {
            access.seek(location.offset.toLong())
            access.write(entryString.toByteArray())
            true
        } else {
            val bytes = ByteArray(access.length().toInt() - location.offset)
            access.seek(location.offset.toLong() + location.length)
            access.readFully(bytes)
            access.seek(location.offset.toLong())
            access.write((entryString + bytes).toByteArray())
            false
        }
        if (close)
            access.close()
        return ret
    }

    private data class FileEntry(val offset: Int, val length: Int) {
        companion object {
            fun createFromBounds(firstSep: Int, lastSep: Int) =
                    FileEntry(firstSep + ROW_DIGITS + SEATS_DIGITS, lastSep + ROW_DIGITS + SEATS_DIGITS)
        }
    }

    private fun findInstance(bytes: ByteArray): FileEntry {
        var sepCount = 0
        var lastSep = -6
        var src = String()
        for ((i, b) in bytes.withIndex()) {
            val char = b.toChar()

            val inSource = sepCount % SEPARATORS_PER_ENTRY == 1
            if (char == ENTRY_FIELD_SEPARATOR) {
                sepCount++
                if (inSource) {
                    if (src == this.src.name) {
                        return FileEntry.createFromBounds(lastSep, i)
                    }
                }
                lastSep = i
            }

            if (inSource) {
                src += char
            }
        }
        return FileEntry(bytes.size, 0)
    }
}