package com.almworks.items.gui.meta.schema.enums;

import com.almworks.api.engine.ConnectionIconsManager;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.items.util.BadUtil;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.commons.Procedure;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.net.MalformedURLException;

public interface IconLoader {
  Icon loadIcon(DBReader reader, long item);

  abstract class AttributeLoader implements SerializableFeature<IconLoader> {
    @Override
    public IconLoader restore(DBReader reader, ByteArray.Stream stream, @Nullable Procedure<LongList> invalidate) {
      long attrItem = stream.nextLong();
      DBAttribute<String> attribute = BadUtil.getScalarAttribute(reader, attrItem, String.class);
      if (attribute == null || !stream.isSuccessfullyAtEnd()) {
        LogHelper.error("Failed to load", attrItem, attribute, stream);
        return null;
      }
      return createIconLoader(attribute);
    }

    protected abstract IconLoader createIconLoader(DBAttribute<String> attribute);

    @Override
    public Class<IconLoader> getValueClass() {
      return IconLoader.class;
    }
  }

  abstract class ByAttribute implements IconLoader {
    private final DBAttribute<String> myAttribute;

    protected ByAttribute(DBAttribute<String> attribute) {
      myAttribute = attribute;
    }

    @Override
    public Icon loadIcon(DBReader reader, long item) {
      return loadIcon(reader, item, myAttribute.getValue(item, reader));
    }

    protected abstract Icon loadIcon(DBReader reader, long item, String iconValue);

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      ByAttribute other = Util.castNullable(ByAttribute.class, obj);
      return other != null && other.getClass() == getClass() && Util.equals(myAttribute, other.myAttribute);
    }

    @Override
    public int hashCode() {
      return myAttribute.hashCode();
    }
  }

  class FromUrl extends ByAttribute {
    public static final SerializableFeature<IconLoader> LOADER  =new AttributeLoader() {
      @Override
      protected IconLoader createIconLoader(DBAttribute<String> attribute) {
        return new FromUrl(attribute);
      }
    };

    private FromUrl(DBAttribute<String> iconUrl) {
      super(iconUrl);
    }

    @Override
    protected Icon loadIcon(DBReader reader, long item, String iconUrl) {
      if (iconUrl == null) return null;
      try {
        iconUrl = new org.apache.commons.httpclient.URI(iconUrl, false, "UTF-8").getEscapedURI();
      } catch (org.apache.commons.httpclient.URIException e) {
        LogHelper.error("Failed to escape icon URL", iconUrl, e);
      }
      ConnectionIconsManager icons = reader.getDatabaseUserData().getUserData(ConnectionIconsManager.ROLE);
      if (icons == null) {
        LogHelper.error();
        return null;
      }
      Long connection = SyncAttributes.CONNECTION.getValue(item, reader);
      if (connection == null) return null;
      try {
        return icons.getIcon(connection, iconUrl);
      } catch (MalformedURLException e) {
        LogHelper.warning("bad icon url", iconUrl, BranchSource.trunk(reader).forItem(item));
        return null;
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
    }
  }
}
