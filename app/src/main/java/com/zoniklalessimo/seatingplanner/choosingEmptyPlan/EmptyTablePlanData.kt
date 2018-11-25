package com.zoniklalessimo.seatingplanner.choosingEmptyPlan

import android.os.Parcel
import android.os.Parcelable
import android.util.JsonReader
import android.util.JsonWriter
import com.zoniklalessimo.seatingplanner.scene.Table
import java.io.*
import java.util.*

// TODO: Store the key names of the json properties somewhere central, maybe SharedPreferences

data class EmptyDataTablePlan(val name: String, val tables: Iterable<EmptyDataTable>, val src: File? = null) : Parcelable {
    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<EmptyDataTablePlan> = object : Parcelable.Creator<EmptyDataTablePlan> {
            override fun createFromParcel(parcel: Parcel): EmptyDataTablePlan =
                    EmptyDataTablePlan(parcel)

            override fun newArray(size: Int): Array<EmptyDataTablePlan?> =
                    arrayOfNulls(size)
        }

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

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.createTypedArrayList(EmptyDataTable.CREATOR),
            File(parcel.readString()))

    constructor() : this(String(), emptyList())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeTypedList(tables.toList())
        parcel.writeString(src?.absolutePath)
    }

    fun save(location: File? = null) {
        val file = location ?: src
        ?: throw IllegalStateException("Both the stored and supplied file were null.")
        JsonWriter(OutputStreamWriter(FileOutputStream(file), "UTF-8")).use {json ->
            // TODO: Store Json names somewhere central
            json.beginObject()
            json.name("name").value(name)
            json.name("tables")
            saveTables(json, tables)
            json.endObject()
        }
    }

    override fun describeContents(): Int {
        return 0
    }
}

open class EmptyDataTable(val xBias: Float, val yBias: Float, override var seatCount: Int, override var separators: SortedSet<Int> = sortedSetOf()) : Table, Parcelable {
    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<EmptyDataTable> {
            override fun createFromParcel(parcel: Parcel): EmptyDataTable =
                    EmptyDataTable(parcel)

            override fun newArray(size: Int): Array<EmptyDataTable?> =
                    arrayOfNulls(size)
        }

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

    constructor(parcel: Parcel) : this(
            parcel.readFloat(),
            parcel.readFloat(),
            parcel.readInt(),
            parcel.createIntArray().toSortedSet())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(xBias)
        parcel.writeFloat(yBias)
        parcel.writeInt(seatCount)
        parcel.writeIntArray(separators.toIntArray())
    }

    fun write(writer: JsonWriter) {
        with(writer) {
            beginObject()
            name("xBias").value(xBias)
            name("yBias").value(yBias)
            name("seats").value(seatCount)
            name("separators")
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

    override fun describeContents(): Int {
        return 0
    }
}