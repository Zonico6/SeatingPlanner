package com.zoniklalessimo.seatingplanner.choosingStudentSet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.flatbuffers.FlatBufferBuilder
import com.zoniklalessimo.seatingplanner.schema.StudentSet
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer

interface ChooseStudentSetViewModel {
    val studentSetDir: File

    val studentSets: MutableLiveData<List<StudentSet>>
    val setsCount: Int get() = studentSets.value?.size ?: 0

    fun getStudentSets(): LiveData<List<StudentSet>> {
        if (studentSets.value == null) {
            fetchSets()
        }
        return studentSets
    }

    fun fetchSets() {
        studentSets.value = studentSetDir.listFiles().map { file ->
            val f = RandomAccessFile(file, "r")

            val data = ByteArray(f.length().toInt())
            f.readFully(data)
            f.close()

            val buffer = ByteBuffer.wrap(data)

            StudentSet.getRootAsStudentSet(buffer)
        }
    }

    fun createSet(title: String) {
        val setFile = File(studentSetDir, title)
        setFile.createNewFile()

        val builder = FlatBufferBuilder()

        val name = builder.createString(title)
        val path = builder.createString(setFile.absolutePath)
        //TODO: Check if the title already exist as a filename and if so, use another filename
        val students = StudentSet.createStudentsVector(builder, intArrayOf())

        StudentSet.startStudentSet(builder)

        StudentSet.addName(builder, name)
        StudentSet.addStudents(builder, students)
        StudentSet.addSrc(builder, path)

        val set = StudentSet.endStudentSet(builder)
        builder.finish(set)

        val buffer = builder.dataBuffer()
        studentSets.value = studentSets.value!! + StudentSet.getRootAsStudentSet(buffer)

        FileOutputStream(setFile).use {
            it.write(builder.sizedByteArray())
        }
    }

    fun getSetAt(i: Int): StudentSet = checkNotNull(studentSets.value)[i]

    fun titleOf(i: Int): String =
            studentSets.value?.get(i)?.name() ?: ""

    fun studentCountOf(i: Int): Int =
            studentSets.value?.get(i)?.studentsLength() ?: 0
}