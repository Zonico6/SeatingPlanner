package com.zoniklalessimo.seatingplanner.choosing

import android.util.JsonReader
import android.util.JsonWriter
import com.zoniklalessimo.seatingplanner.scene.Table
import java.io.*
import java.util.*
import kotlin.concurrent.thread

// TODO: Store the key names of the json properties somewhere central, maybe SharedPreferences

class EmptyDataTablePlan(val src: File, reader: JsonReader, onTablesRead: (List<EmptyDataTable>) -> Unit) {
    companion object {
        fun read(reader: JsonReader): Array<EmptyDataTable> {
            val tables = mutableListOf<EmptyDataTable>()
            reader.beginArray()

            while (reader.hasNext()) {
                tables.add(EmptyDataTable.fromJson(reader))
            }

            reader.endArray()
            return tables.toTypedArray()
        }

        fun save(writer: JsonWriter, tables: Iterable<EmptyDataTable>) {
            writer.beginArray()
            for (table in tables) {

            }
            writer.endArray()
        }
    }

    constructor(src: File, onTablesRead: (List<EmptyDataTable>) -> Unit = {}) : this(
            src,
            JsonReader(InputStreamReader(FileInputStream(src))),
            onTablesRead)

    private var ioThread: Thread?

    init {
        ioThread = thread {
            tables.addAll(read(reader))
            reader.close()
            hasData = true
            onTablesRead(tables)
        }
    }

    val tables = mutableListOf<EmptyDataTable>()
    var hasData = false
        private set

    fun isWriting() = ioThread != null && hasData
    fun isResting() = ioThread == null

    fun save(onFinishWriting: () -> Unit = {}) {
        ioThread = thread {
            val writer = OutputStreamWriter(FileOutputStream(src), "UTF-8")
            save(JsonWriter(writer), tables)
            writer.close()
            onFinishWriting()
            ioThread = null
        }
    }
}

class EmptyDataTable(val x: Float, val y: Float, override var seatCount: Int, override var separators: SortedSet<Int>) : Table {
    companion object {
        fun fromJson(reader: JsonReader): EmptyDataTable {
            var x = -1f
            var y = -1f
            var seatCount = -1
            var separators: SortedSet<Int> = sortedSetOf()

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "x" -> x = reader.nextDouble().toFloat()
                    "y" -> y = reader.nextDouble().toFloat()
                    "seats" -> seatCount = reader.nextInt()
                    "separators" -> separators = readSeparators(reader)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            return EmptyDataTable(x, y, seatCount, separators)
        }

        private fun readSeparators(reader: JsonReader): SortedSet<Int> {
            val ret = sortedSetOf<Int>()
            reader.beginArray()

            while (reader.hasNext()) {
                ret.add(reader.nextInt())
            }

            reader.endArray()
            return ret
        }
    }

    fun write(writer: JsonWriter) {
        with(writer) {
            beginObject()
            name("x").value(x)
            name("y").value(y)
            name("seats").value(seatCount)
            writeSeparators()
            endObject()
        }
    }

    private fun JsonWriter.writeSeparators() {
        beginArray()
        for (i in separators)
            value(i)
        endArray()
    }
}