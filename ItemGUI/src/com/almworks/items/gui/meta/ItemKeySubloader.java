package com.almworks.items.gui.meta;

import com.almworks.api.application.ItemKeyCache;
import com.almworks.api.application.ResolvedFactory;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.constraints.Descriptors;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.BadUtil;
import com.almworks.util.Break;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.LongSet;
import com.almworks.util.commons.Procedure;
import com.almworks.util.properties.TypedKeyWithEquality;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface ItemKeySubloader {
  Object getValue(long item, DBReader reader, ItemKeyCache cache, ResolvedFactory<LoadedItemKey> factory);
  Object getKey();

  public static class Attribute implements ItemKeySubloader {
    public static ScalarSequence sequence(DBAttribute<?> attribute) {
      return ScalarSequence.create(Descriptors.FEATURE_ATTRIBUTE_SUBLOADER, DBIdentity.fromDBObject(attribute));
    }

    public static SerializableFeature<ItemKeySubloader> SERIALIZABLE = new Serializable() {
      @Override
      public ItemKeySubloader restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
        try {
          final DBAttribute<?> attribute = readAttribute(stream, reader);
          checkAtEnd(stream);
          return new Attribute(attribute);
        } catch(Break b) {
          LogHelper.error(b.getMessage(), stream);
          return null;
        }
      }
    };

    @NotNull private final DBAttribute myAttribute;

    public Attribute(@NotNull DBAttribute attribute) {
      myAttribute = attribute;
    }

    @Override
    public Object getKey() {
      return myAttribute;
    }

    @Override
    public Object getValue(long item, DBReader reader, ItemKeyCache cache, ResolvedFactory<LoadedItemKey> factory) {
      return reader.getValue(item, myAttribute);
    }

    @Override
    public int hashCode() {
      return 29 * Attribute.class.hashCode() + myAttribute.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if(o == this) {
        return true;
      }
      if(o == null) {
        return false;
      }
      if(o.getClass() != getClass()) {
        return false;
      }
      return myAttribute.equals(((Attribute)o).myAttribute);
    }
  }

  public static class Parent implements ItemKeySubloader {
    public static ScalarSequence sequence(DBAttribute<Long> attribute, TypedKeyWithEquality<LoadedItemKey> key) {
      return new ScalarSequence.Builder()
        .append(Descriptors.FEATURE_PARENT_SUBLOADER)
        .append(attribute)
        .append(String.valueOf(key.getEqualityClass()))
        .create();
    }

    public static SerializableFeature<ItemKeySubloader> SERIALIZABLE = new Serializable() {
      @Override
      public ItemKeySubloader restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
        try {
          final DBAttribute<Long> attribute = readParentAttribute(stream, reader);
          final TypedKeyWithEquality<LoadedItemKey> key = readTypedKey(stream);
          checkAtEnd(stream);
          return new Parent(attribute, key);
        } catch(Break b) {
          LogHelper.error(b.getMessage(), stream);
          return null;
        }
      }
    };

    @NotNull private final DBAttribute<Long> myParentAttribute;
    @NotNull private final TypedKeyWithEquality<LoadedItemKey> myParentKey;

    public Parent(@NotNull DBAttribute<Long> parentAttribute, @NotNull TypedKeyWithEquality<LoadedItemKey> parentKey) {
      myParentAttribute = parentAttribute;
      myParentKey = parentKey;
    }

    @Override
    public Object getKey() {
      return myParentKey;
    }

    @Override
    public Object getValue(long item, DBReader reader, ItemKeyCache cache, ResolvedFactory<LoadedItemKey> factory) {
      final Long parentId = reader.getValue(item, myParentAttribute);
      if(parentId == null || parentId <= 0L) {
        return null;
      }
      return cache.getItemKeyOrNull(parentId, reader, factory);
    }

    public int hashCode() {
      int hash = Parent.class.hashCode();
      hash = 29 * hash + myParentAttribute.hashCode();
      hash = 29 * hash + myParentKey.hashCode();
      return hash;
    }

    @Override
    public boolean equals(Object o) {
      if(o == this) {
        return true;
      }
      if(o == null) {
        return false;
      }
      if(o.getClass() != getClass()) {
        return false;
      }
      final Parent that = (Parent)o;
      return myParentAttribute.equals(that.myParentAttribute) && myParentKey.equals(that.myParentKey);
    }
  }

  public static class Subtree implements ItemKeySubloader {
    public static ScalarSequence sequence(DBAttribute<Long> attribute, TypedKeyWithEquality<Set<Long>> key) {
      return new ScalarSequence.Builder()
        .append(Descriptors.FEATURE_SUBTREE_SUBLOADER)
        .append(attribute)
        .append(String.valueOf(key.getEqualityClass()))
        .create();
    }

    public static SerializableFeature<ItemKeySubloader> SERIALIZABLE = new Serializable() {
      @Override
      public ItemKeySubloader restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
        try {
          final DBAttribute<Long> attribute = readParentAttribute(stream, reader);
          final TypedKeyWithEquality<Set<Long>> key = readTypedKey(stream);
          checkAtEnd(stream);
          return new Subtree(attribute, key);
        } catch(Break b) {
          LogHelper.error(b.getMessage(), stream);
          return null;
        }
      }
    };

    @NotNull private final DBAttribute<Long> myParentAttribute;
    @NotNull private final TypedKeyWithEquality<Set<Long>> mySubtreeKey;

    public Subtree(@NotNull DBAttribute<Long> parentAttribute, @NotNull TypedKeyWithEquality<Set<Long>> subtreeKey) {
      myParentAttribute = parentAttribute;
      mySubtreeKey = subtreeKey;
    }

    @Override
    public Object getKey() {
      return mySubtreeKey;
    }

    @Override
    public Object getValue(long item, DBReader reader, ItemKeyCache cache, ResolvedFactory<LoadedItemKey> factory) {
      final LongSet result = new LongSet();

      LongArray front = new LongArray();
      front.add(item);

      while(!front.isEmpty()) {
        final LongArray nextFront = reader.query(DPEquals.equalOneOf(myParentAttribute, front)).copyItemsSorted();
        result.addAll(nextFront);
        front = nextFront;
      }

      return result.toObjectSet();
    }

    public int hashCode() {
      int hash = Subtree.class.hashCode();
      hash = 29 * hash + myParentAttribute.hashCode();
      hash = 29 * hash + mySubtreeKey.hashCode();
      return hash;
    }

    @Override
    public boolean equals(Object o) {
      if(o == this) {
        return true;
      }
      if(o == null) {
        return false;
      }
      if(o.getClass() != getClass()) {
        return false;
      }
      final Subtree that = (Subtree)o;
      return myParentAttribute.equals(that.myParentAttribute) && mySubtreeKey.equals(that.mySubtreeKey);
    }
  }

  static abstract class Serializable implements SerializableFeature<ItemKeySubloader> {
    protected static void checkAtEnd(ByteArray.Stream stream) throws Break {
      Break.breakIf(!stream.isSuccessfullyAtEnd(), "Expected end of stream");
    }

    protected static DBAttribute<?> readAttribute(ByteArray.Stream stream, DBReader reader) throws Break {
      final long attrId = stream.nextLong();
      Break.breakIf(attrId <= 0L, "Bad attribute ID: %d", attrId);

      final DBAttribute<?> attribute = BadUtil.getAttribute(reader, attrId);
      Break.breakIfNull(attribute, "Can't get attribute");

      return attribute;
    }

    protected static DBAttribute<Long> readParentAttribute(ByteArray.Stream stream, DBReader reader) throws Break {
      final DBAttribute<?> attribute = readAttribute(stream, reader);
      Break.breakIf(attribute.getScalarClass() != Long.class, "Attribute of type Long expected: %s", attribute);
      Break.breakIf(attribute.getComposition() != DBAttribute.ScalarComposition.SCALAR, "Scalar attribute expected: %s", attribute);
      return (DBAttribute<Long>)attribute;
    }

    protected static String readString(ByteArray.Stream stream) throws Break {
      final String str = stream.nextUTF8();
      Break.breakIfNull(str, "Null string");
      Break.breakIf(str.isEmpty(), "Empty string");
      return str;
    }

    protected static <T> TypedKeyWithEquality<T> readTypedKey(ByteArray.Stream stream) throws Break {
      final String keyId = readString(stream);
      return TypedKeyWithEquality.create((Object)keyId);
    }

    @Override
    public Class<ItemKeySubloader> getValueClass() {
      return ItemKeySubloader.class;
    }
  }
}
