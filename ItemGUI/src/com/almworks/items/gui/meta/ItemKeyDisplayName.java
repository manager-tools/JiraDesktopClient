package com.almworks.items.gui.meta;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.util.BadUtil;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ItemKeyDisplayName {
  public abstract String getDisplayName(DBReader reader, long item);

  public static final SerializableFeature<ItemKeyDisplayName> FIRST_NOT_NULL = new SerializableFeature<ItemKeyDisplayName>() {
    @Override
    public ItemKeyDisplayName restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      final int count = stream.nextInt();
      final DBAttribute<?>[] attributes = new DBAttribute[count];
      for(int i = 0; i < count; i++) {
        DBAttribute<?> attribute = BadUtil.getAttribute(reader, stream.nextLong());
        if(attribute == null || attribute.getComposition() != DBAttribute.ScalarComposition.SCALAR) {
          LogHelper.error("Wrong attribute", attribute, (attribute != null ? attribute.getComposition() : ""), stream);
          return null;
        }
        attributes[i] = attribute;
      }
      if(stream.isSuccessfullyAtEnd()) {
        return new FirstNotNull(attributes);
      }
      LogHelper.error("Cannot read stream", stream);
      return null;
    }

    @Override
    public Class<ItemKeyDisplayName> getValueClass() {
      return ItemKeyDisplayName.class;
    }
  };

  public static final SerializableFeature<ItemKeyDisplayName> PATH_FROM_ROOT = new SerializableFeature<ItemKeyDisplayName>() {
    @Override
    public ItemKeyDisplayName restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      final long attrItem = stream.nextLong();
      final DBAttribute<Long> parentAttr = BadUtil.getScalarAttribute(reader, attrItem, Long.class);
      if(parentAttr != null) {
        final String pathSep = stream.nextUTF8();
        final ItemKeyDisplayName element = FIRST_NOT_NULL.restore(reader, stream, invalidate);
        if(element != null && stream.isSuccessfullyAtEnd()) {
          return new PathFromRoot(parentAttr, element, pathSep);
        }
      }
      LogHelper.error("Cannot read stream", stream);
      return null;
    }

    @Override
    public Class<ItemKeyDisplayName> getValueClass() {
      return ItemKeyDisplayName.class;
    }
  };

  private static class FirstNotNull extends ItemKeyDisplayName {
    private final DBAttribute<?>[] myAttributes;

    public FirstNotNull(DBAttribute<?>[] attributes) {
      myAttributes = attributes;
    }

    @Override
    public String getDisplayName(DBReader reader, long item) {
      for (DBAttribute<?> attribute : myAttributes) {
        Object val = attribute.getValue(item, reader);
        if (val != null) return String.valueOf(val);
      }
      return null;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      FirstNotNull other = Util.castNullable(FirstNotNull.class, obj);
      if (other == null || myAttributes.length != other.myAttributes.length) return false;
      for (int i = 0; i < myAttributes.length; i++) {
        if (!Util.equals(myAttributes[i], other.myAttributes[i])) return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int hashCode = FirstNotNull.class.hashCode();
      for (DBAttribute<?> attribute : myAttributes) hashCode = hashCode ^ Util.hashCode(attribute);
      return hashCode;
    }
  }

  private static class PathFromRoot extends ItemKeyDisplayName {
    @NotNull private final DBAttribute<Long> myParentAttribute;
    @NotNull private final ItemKeyDisplayName myElementRenderer;
    @NotNull private final String myPathSeparator;

    private final StringBuilder myBuilder = new StringBuilder();

    public PathFromRoot(@NotNull DBAttribute<Long> parentAttribute, @NotNull ItemKeyDisplayName elementRenderer, @NotNull String pathSeparator) {
      myParentAttribute = parentAttribute;
      myElementRenderer = elementRenderer;
      myPathSeparator = pathSeparator;
    }

    @Override
    public String getDisplayName(DBReader reader, long item) {
      myBuilder.setLength(0);
      Long element = item;
      while(element != null && element > 0L) {
        if(myBuilder.length() > 0) {
          myBuilder.insert(0, myPathSeparator);
        }
        myBuilder.insert(0, myElementRenderer.getDisplayName(reader, element));
        element = reader.getValue(element, myParentAttribute);
      }
      return myBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
      if(this == o) {
        return true;
      }
      if(o == null || getClass() != o.getClass()) {
        return false;
      }
      final PathFromRoot that = (PathFromRoot) o;
      return myParentAttribute.equals(that.myParentAttribute)
        && myPathSeparator.equals(that.myPathSeparator)
        && myElementRenderer.equals(that.myElementRenderer);
    }

    @Override
    public int hashCode() {
      int result = myParentAttribute.hashCode();
      result = 31 * result + myElementRenderer.hashCode();
      result = 31 * result + myPathSeparator.hashCode();
      return result;
    }
  }
}
