package com.almworks.dbproperties;

import com.almworks.util.collections.ByteArray;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import java.util.*;

public class MapSerializeTests extends BaseTestCase {
  private static final TypedKey<String> TEXT = TypedKey.create("a", String.class);
  private static final TypedKey<Boolean> BOOL = TypedKey.create("b", Boolean.class);
  private static final TypedKey<Map> MAP = TypedKey.create("c", Map.class);
  private static final TypedKey<List<String>> LIST = TypedKey.create("d", (Class<List<String>>)(Class)List.class);
  private static final TypedKey<List<Map<TypedKey<?> , ?>>> LIST_MAPS = TypedKey.create("e", (Class<List<Map<TypedKey<?> , ?>>>)(Class)List.class);

  private static final SerializeSchema SCHEMA;
  static {
    SCHEMA = new SerializeSchema();
    SCHEMA.addKey(BOOL);
    SCHEMA.addKey(TEXT);
    SCHEMA.addKey(MAP);
    SCHEMA.addListKey(LIST, String.class);
    SCHEMA.addListKey(LIST_MAPS, (Class)Map.class);
  }

  public void testEmpty() {
    HashMap<TypedKey<?>, Object> map = Collections15.hashMap();
    byte[] bytes = MapSerializer.serialize(map, SCHEMA);
    ByteArray.Stream stream = new ByteArray.Stream(bytes);
    assertEquals(ByteSerializer.TYPE_MAP, stream.nextByte());
    assertEquals(0, stream.nextInt());
    assertEquals(8, stream.nextLong());
    assertTrue(stream.isSuccessfullyAtEnd());

    Map<? extends TypedKey<?>, ?> restore = MapDeserializer.restore(bytes, SCHEMA);
    assertEquals(0, restore.size());
  }

  public void testScalars() {
    HashMap<TypedKey<?>, Object> map = Collections15.hashMap();
    TEXT.putTo(map, "12");
    BOOL.putTo(map, true);
    byte[] bytes = MapSerializer.serialize(map, SCHEMA);
    ByteArray.Stream stream = new ByteArray.Stream(bytes);
    assertEquals(ByteSerializer.TYPE_MAP, stream.nextByte());
    assertEquals(2, stream.nextInt());
    assertEquals("a", stream.nextUTF8());
    assertEquals(ByteSerializer.TYPE_TEXT, stream.nextByte());
    assertEquals("b", stream.nextUTF8());
    assertEquals(ByteSerializer.TYPE_BOOLEAN, stream.nextByte());
    int mapStart = stream.getPosition();
    long length = stream.nextLong();
    assertEquals(0, stream.nextInt());
    assertEquals("12", stream.nextUTF8());
    assertEquals(1, stream.nextInt());
    assertEquals(true, stream.nextBoolean());
    assertTrue(stream.isSuccessfullyAtEnd());
    assertEquals(length, stream.getPosition() - mapStart);

    Map<? extends TypedKey<?>, ?> restore = MapDeserializer.restore(bytes, SCHEMA);
    assertEquals(2, restore.size());
    assertEquals("12", map.get(TEXT));
    assertEquals(true, map.get(BOOL));
  }

  public void testMap() {
    HashMap<TypedKey<?>, Object> map = Collections15.hashMap();
    BOOL.putTo(map, true);
    HashMap<TypedKey<?>, Object> inner = Collections15.hashMap();
    BOOL.putTo(inner, false);
    MAP.putTo(inner, Collections.emptyMap());
    MAP.putTo(map, inner);
    byte[] bytes = MapSerializer.serialize(map, SCHEMA);
    ByteArray.Stream stream = new ByteArray.Stream(bytes);
    assertEquals(ByteSerializer.TYPE_MAP, stream.nextByte());
    assertEquals(2, stream.nextInt());
    assertEquals("b", stream.nextUTF8());
    assertEquals(ByteSerializer.TYPE_BOOLEAN, stream.nextByte());
    assertEquals("c", stream.nextUTF8());
    assertEquals(ByteSerializer.TYPE_MAP, stream.nextByte());
    int mapStart = stream.getPosition();
    long length = stream.nextLong();
    assertEquals(0, stream.nextInt());
    assertEquals(true, stream.nextBoolean());
    assertEquals(1, stream.nextInt());
    int innerStart = stream.getPosition();
    long innerLength = stream.nextLong();
    assertEquals(0, stream.nextInt());
    assertEquals(false, stream.nextBoolean());
    assertEquals(1, stream.nextInt());
    assertEquals(8, stream.nextLong());
    assertEquals(innerLength, stream.getPosition() - innerStart);
    assertTrue(stream.isSuccessfullyAtEnd());
    assertEquals(length, stream.getPosition() - mapStart);

    Map<? extends TypedKey<?>, ?> restore = MapDeserializer.restore(bytes, SCHEMA);
    assertEquals(2, restore.size());
    assertEquals(true, restore.get(BOOL));
    @SuppressWarnings("unchecked")
    Map<TypedKey<?>, ?> innerRestore = MAP.getFrom(restore);
    //noinspection ConstantConditions
    assertEquals(false, innerRestore.get(BOOL));
    //noinspection ConstantConditions
    assertEquals(0, MAP.getFrom(innerRestore).size());
  }

  public void testList() {
    HashMap<TypedKey<?>, Object> map = Collections15.hashMap();
    LIST.putTo(map, Arrays.asList("1", "2"));
    Map<TypedKey<?>, Object> inner = Collections15.hashMap();
    BOOL.putTo(inner, false);
    LIST_MAPS.putTo(map, Arrays.<Map<TypedKey<?>, ?>>asList(inner));


    byte[] bytes = MapSerializer.serialize(map, SCHEMA);
    assertNotNull(bytes);
    ByteArray.Stream stream = new ByteArray.Stream(bytes);
    assertEquals(ByteSerializer.TYPE_MAP, stream.nextByte());
    assertEquals(3, stream.nextInt());
    assertEquals("b", stream.nextUTF8());
    assertEquals(ByteSerializer.TYPE_BOOLEAN, stream.nextByte());
    assertEquals("d", stream.nextUTF8());
    assertEquals(ByteSerializer.TYPE_LIST, stream.nextByte());
    assertEquals(ByteSerializer.TYPE_TEXT, stream.nextByte());
    assertEquals("e", stream.nextUTF8());
    assertEquals(ByteSerializer.TYPE_LIST, stream.nextByte());
    assertEquals(ByteSerializer.TYPE_MAP, stream.nextByte());
    int mapStart = stream.getPosition();
    long mapLength = stream.nextLong();
    assertEquals(1, stream.nextInt());
    int listStart = stream.getPosition();
    long listLength = stream.nextLong();
    assertEquals("1", stream.nextUTF8());
    assertEquals("2", stream.nextUTF8());
    assertEquals(listLength, stream.getPosition() - listStart);
    assertEquals(2, stream.nextInt());
    listStart = stream.getPosition();
    listLength = stream.nextLong();
    int innerStart = stream.getPosition();
    long innerLength = stream.nextLong();
    assertEquals(0, stream.nextInt());
    assertEquals(false, stream.nextBoolean());
    assertEquals(innerLength, stream.getPosition() - innerStart);
    assertEquals(listLength, stream.getPosition() - listStart);
    assertEquals(mapLength, stream.getPosition() - mapStart);

    Map<? extends TypedKey<?>, ?> restore = MapDeserializer.restore(bytes, SCHEMA);
    assertNotNull(restore);
    assertEquals(2, restore.size());
    List<String> restoreList = LIST.getFrom(restore);
    assertNotNull(restoreList);
    assertEquals(2, restoreList.size());
    assertEquals("1", restoreList.get(0));
    assertEquals("2", restoreList.get(1));
    List<Map<TypedKey<?>, ?>> restoreMapsList = LIST_MAPS.getFrom(restore);
    assertNotNull(restoreMapsList);
    assertEquals(1, restoreMapsList.size());
    Map<TypedKey<?>, ?> restoreInner = restoreMapsList.get(0);
    assertNotNull(restoreInner);
    assertEquals(1, restoreInner.size());
    assertEquals(Boolean.FALSE, BOOL.getFrom(restoreInner));
  }
}
