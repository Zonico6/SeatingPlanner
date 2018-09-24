package com.zoniklalessimo.seatingplanner.choosing

import android.util.JsonReader
import android.util.JsonWriter
import com.zoniklalessimo.seatingplanner.scene.Table
import java.io.*
import java.util.*

// TODO: Store the key names of the json properties somewhere central, maybe SharedPreferences

class EmptyDataTablePlan(val name: String, val tables: Iterable<EmptyDataTable>, val src: File? = null) : Serializable {
    companion object {

        fun fromSaveFile(src: File): EmptyDataTablePlan {
            val tables = mutableListOf<EmptyDataTable>()

            val reader = JsonReader(InputStreamReader(FileInputStream(src)))
            reader.beginObject()
            var nameVal = ""
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "name" -> nameVal = reader.nextString()
                    "tables" -> tables.addAll(readTables(reader))
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            val name = nameVal
            reader.close()

            return EmptyDataTablePlan(name, tables, src)
        }

        fun readTables(reader: JsonReader): Array<EmptyDataTable> {
            val tables = mutableListOf<EmptyDataTable>()
            reader.beginArray()

            while (reader.hasNext()) {
                tables.add(EmptyDataTable.fromJson(reader))
            }

            reader.endArray()
            return tables.toTypedArray()
        }

        fun saveTables(writer: JsonWriter, tables: Iterable<EmptyDataTable>) {
            writer.beginArray()
            for (table in tables) {
                table.write(writer)
            }
            writer.endArray()
        }
    }

    fun save(location: File? = null) {
        val file = location ?: src
        ?: throw IllegalStateException("Both the stored and supplied file were null.")
        val writer = OutputStreamWriter(FileOutputStream(file), "UTF-8")
        val json = JsonWriter(writer)
        json.name("name").value(name)
        saveTables(json, tables)
        writer.close()
    }
}

data class EmptyDataTable(val xBias: Float, val yBias: Float, override var seatCount: Int, override var separators: SortedSet<Int>) : Table {
    companion object {
        fun fromJson(reader: JsonReader): EmptyDataTable {
            var x = -1f
            var y = -1f
            var seatCount = -1
            var separators: SortedSet<Int> = sortedSetOf()

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "xBias" -> x = reader.nextDouble().toFloat()
                    "yBias" -> y = reader.nextDouble().toFloat()
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
            name("xBias").value(xBias)
            name("yBias").value(yBias)
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