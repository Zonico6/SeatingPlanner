// automatically generated by the FlatBuffers compiler, do not modify

package com.zoniklalessimo.seatingplanner.schema;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class StudentSet extends Table {
  public static StudentSet getRootAsStudentSet(ByteBuffer _bb) { return getRootAsStudentSet(_bb, new StudentSet()); }
  public static StudentSet getRootAsStudentSet(ByteBuffer _bb, StudentSet obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; }
  public StudentSet __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public String src() { int o = __offset(4); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer srcAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public ByteBuffer srcInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 4, 1); }
  public String name() { int o = __offset(6); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer nameAsByteBuffer() { return __vector_as_bytebuffer(6, 1); }
  public ByteBuffer nameInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 6, 1); }
  public Student students(int j) { return students(new Student(), j); }
  public Student students(Student obj, int j) { int o = __offset(8); return o != 0 ? obj.__assign(__indirect(__vector(o) + j * 4), bb) : null; }
  public int studentsLength() { int o = __offset(8); return o != 0 ? __vector_len(o) : 0; }

  public static int createStudentSet(FlatBufferBuilder builder,
      int srcOffset,
      int nameOffset,
      int studentsOffset) {
    builder.startObject(3);
    StudentSet.addStudents(builder, studentsOffset);
    StudentSet.addName(builder, nameOffset);
    StudentSet.addSrc(builder, srcOffset);
    return StudentSet.endStudentSet(builder);
  }

  public static void startStudentSet(FlatBufferBuilder builder) { builder.startObject(3); }
  public static void addSrc(FlatBufferBuilder builder, int srcOffset) { builder.addOffset(0, srcOffset, 0); }
  public static void addName(FlatBufferBuilder builder, int nameOffset) { builder.addOffset(1, nameOffset, 0); }
  public static void addStudents(FlatBufferBuilder builder, int studentsOffset) { builder.addOffset(2, studentsOffset, 0); }
  public static int createStudentsVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startStudentsVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static int endStudentSet(FlatBufferBuilder builder) {
    return builder.endObject();
  }
  public static void finishStudentSetBuffer(FlatBufferBuilder builder, int offset) { builder.finish(offset); }
  public static void finishSizePrefixedStudentSetBuffer(FlatBufferBuilder builder, int offset) { builder.finishSizePrefixed(offset); }
}

