package com.almworks.items.gui.edit.merge;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.CancelCommitException;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.items.sync.util.ShadowVersionSource;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

public abstract class MergeValue {
  public static final DataRole<MergeValue> ROLE = DataRole.createRole(MergeValue.class);
  public static final Convertor<? super MergeValue,String> GET_DISPLAY_NAME = new Convertor<MergeValue, String>() {
    @Override
    public String convert(MergeValue value) {
      return Util.NN(value != null ? value.getDisplayName() : null);
    }
  };

  public static final int LOCAL = 0;
  public static final int BASE = 1;
  public static final int REMOTE = 2;

  private final String myDisplayName;
  private final long myItem;

  protected MergeValue(String displayName, long item) {
    myDisplayName = displayName;
    myItem = item;
  }

  public final String getDisplayName() {
    return myDisplayName;
  }

  public final long getItem() {
    return myItem;
  }

  public abstract void render(CellState state, Canvas canvas, int version);

  public abstract void setResolution(int version);

  public abstract boolean isConflict();

  public abstract boolean isChanged(boolean remote);

  public abstract Object getValue(int version);

  public static <T> T loadValue(DBReader reader, long item, DBAttribute<T> attribute, int version) {
    ItemVersion itemVersion = getItemVersion(reader, item, version);
    return itemVersion != null ? itemVersion.getValue(attribute) : null;
  }

  public static ItemVersion getItemVersion(DBReader reader, long item, int version) {
    VersionSource source;
    switch (version) {
    case LOCAL: source = BranchSource.trunk(reader); break;
    case BASE: source = ShadowVersionSource.base(reader); break;
    case REMOTE: source = ShadowVersionSource.conflict(reader); break;
    default:
      LogHelper.error("Unknown version", version, item);
      return null;
    }
    return source.forItem(item);
  }

  private static ItemVersion trunkIfNull(ItemVersion version, DBReader reader, long item) {
    if (version == null) version = SyncUtils.readTrunk(reader, item);
    return version;
  }

  public static long getSingleItem(EditItemModel model) {
    LongList items = model.getEditingItems();
    if (items.size() != 1) {
      LogHelper.error("Single item supported", items);
      return 0;
    }
    return items.get(0);
  }

  public final boolean isChangeOrConflict() {
    return !isResolved() && (isConflict() || isChanged(true) || isChanged(false));
  }

  /**
   * @return true iff the value has resolution (initially no value has resolution even it is not affected by local or server changes)
   */
  public abstract boolean isResolved();

  public abstract void addChangeListener(Lifespan life, ChangeListener listener);

  public abstract void commit(CommitContext context) throws CancelCommitException;


  public static abstract class Simple extends MergeValue {
    private final SimpleModifiable myModifiable = new SimpleModifiable();
    private boolean myResolved = false;

    protected Simple(String displayName, long item) {
      super(displayName, item);
    }

    @Override
    public final boolean isResolved() {
      return myResolved;
    }

    @Override
    public final void setResolution(int version) {
      doSetResolution(version);
      markResolved();
    }

    @Override
    public final void addChangeListener(Lifespan life, ChangeListener listener) {
      myModifiable.addAWTChangeListener(life, listener);
    }

    public final void markResolved() {
      myResolved = true;
      myModifiable.fireChanged();
    }

    @Override
    public final void commit(CommitContext context) throws CancelCommitException {
      FieldEditor editor = getEditor();
      EditItemModel model = getModel();
      if (editor.hasDataToCommit(model)) editor.commit(context.subContext(model));
    }

    protected abstract void doSetResolution(int version);

    protected abstract FieldEditor getEditor();

    protected abstract EditItemModel getModel();
  }
}
