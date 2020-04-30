package com.almworks.items.gui.meta.util;

import com.almworks.api.engine.DBCommons;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface FieldInfo {
  DBNamespace NS = DBCommons.NS.subNs("itemGuiUtil");
  DBAttribute<String> ICON_URL = NS.string("iconUrl");


  DBStaticObject createColumn();

  DBStaticObject createDescriptor();

  DBStaticObject createViewerField();

  DBStaticObject createModelKey();

  boolean hasDnDChange();

  @Nullable
  DBStaticObject createDnDChange();

  DBStaticObject createExport();

  @Nullable
  DBStaticObject createReorder();

  boolean hasExport();

  class Delegating implements FieldInfo {
    private final FieldInfo mySource;

    public Delegating(FieldInfo source) {
      mySource = source;
    }

    @Override
    public DBStaticObject createColumn() {
      return mySource.createColumn();
    }

    @Override
    public DBStaticObject createDescriptor() {
      return mySource.createDescriptor();
    }

    @Override
    public DBStaticObject createViewerField() {
      return mySource.createViewerField();
    }

    @Override
    public DBStaticObject createModelKey() {
      return mySource.createModelKey();
    }

    @Override
    public boolean hasDnDChange() {
      return mySource.hasDnDChange();
    }

    @Override
    @Nullable
    public DBStaticObject createDnDChange() {
      return mySource.createDnDChange();
    }

    @Override
    public DBStaticObject createExport() {
      return mySource.createExport();
    }

    @Override
    @Nullable
    public DBStaticObject createReorder() {
      return mySource.createReorder();
    }

    @Override
    public boolean hasExport() {
      return mySource.hasExport();
    }
  }
  
  class Wrapper extends Delegating {
    private final boolean myNoColumn;
    private final boolean myNoSearch;
    private final boolean myNoReorder;
    private final boolean myNoViewerField;

    public Wrapper(FieldInfo source, boolean noColumn, boolean noSearch, boolean noReorder, boolean noViewerField) {
      super(source);
      myNoColumn = noColumn;
      myNoSearch = noSearch;
      myNoReorder = noReorder;
      myNoViewerField = noViewerField;
    }

    @Override
    public DBStaticObject createColumn() {
      return myNoColumn ? null : super.createColumn();
    }

    @Override
    public DBStaticObject createDescriptor() {
      return myNoSearch ? null : super.createDescriptor();
    }

    @Override
    public DBStaticObject createViewerField() {
      return myNoViewerField ? null : super.createViewerField();
    }
  }
  
  class OverrideDnD extends Delegating {
    private final DBStaticObject myDnD;
    
    public OverrideDnD(@NotNull FieldInfo source, @Nullable DBStaticObject dnD) {
      super(source);
      myDnD = dnD;
    }
    
    public static FieldInfo override(@NotNull FieldInfo source, @Nullable DBStaticObject dnD) {
      //noinspection ConstantConditions
      if (source == null) {
        LogHelper.error("Missing source");
        return null;
      }
      return dnD == null ? source : new OverrideDnD(source, dnD);
    }

    @Override
    public boolean hasDnDChange() {
      return myDnD != null || super.hasDnDChange();
    }

    @Override
    public DBStaticObject createDnDChange() {
      return myDnD != null ? myDnD : super.createDnDChange();
    }
  }
}
