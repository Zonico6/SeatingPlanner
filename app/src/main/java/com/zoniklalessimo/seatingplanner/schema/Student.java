// automatically generated by the FlatBuffers compiler, do not modify

package com.zoniklalessimo.seatingplanner.schema;

import java.nio.*;
import java.lang.*;

import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class Student extends Table {
  public static Student getRootAsStudent(ByteBuffer _bb) { return getRootAsStudent(_bb, new Student()); }
  public static Student getRootAsStudent(ByteBuffer _bb, Student obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; }
  public Student __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public String name() { int o = __offset(4); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer nameAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public ByteBuffer nameInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 4, 1); }
  public String neighbours(int j) { int o = __offset(6); return o != 0 ? __string(__vector(o) + j * 4) : null; }
  public int neighboursLength() { int o = __offset(6); return o != 0 ? __vector_len(o) : 0; }
  public String distants(int j) { int o = __offset(8); return o != 0 ? __string(__vector(o) + j * 4) : null; }
  public int distantsLength() { int o = __offset(8); return o != 0 ? __vector_len(o) : 0; }

  public static int createStudent(FlatBufferBuilder builder,
      int nameOffset,
      int neighboursOffset,
      int distantsOffset) {
    builder.startObject(3);
    Student.addDistants(builder, distantsOffset);
    Student.addNeighbours(builder, neighboursOffset);
    Student.addName(builder, nameOffset);
    return Student.endStudent(builder);
  }

  public static void startStudent(FlatBufferBuilder builder) { builder.startObject(3); }
  public static void addName(FlatBufferBuilder builder, int nameOffset) { builder.addOffset(0, nameOffset, 0); }
  public static void addNeighbours(FlatBufferBuilder builder, int neighboursOffset) { builder.addOffset(1, neighboursOffset, 0); }
  public static int createNeighboursVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startNeighboursVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static void addDistants(FlatBufferBuilder builder, int distantsOffset) { builder.addOffset(2, distantsOffset, 0); }
  public static int createDistantsVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startDistantsVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static int endStudent(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}
