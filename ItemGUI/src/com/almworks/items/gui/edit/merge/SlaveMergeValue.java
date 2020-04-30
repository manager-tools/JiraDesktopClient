package com.almworks.items.gui.edit.merge;

import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class SlaveMergeValue<V extends SlaveMergeValue.SlaveVersion> extends MergeValue {
  private final V[] myVersions;
  private final EditItemModel myModel;
  private final TypedKey<V> myResolutionKey;

  protected SlaveMergeValue(String displayName, long item, String debugName, EditItemModel model, V[] versions) {
    super(displayName, item);
    myVersions = versions;
    myModel = model;
    myResolutionKey = TypedKey.create(debugName + "@" + item);
  }

  public static <V extends SlaveVersion> SlaveMergeValue<V> create(String displayName, long item, String debugName, EditItemModel model, V[] versions) {
    return new SlaveMergeValue<V>(displayName, item, debugName, model, versions);
  }

  @Nullable
  public <VV extends SlaveVersion> SlaveMergeValue<VV> cast(Class<VV> versionClass) {
    boolean found = false;
    for (V version : myVersions) {
      if (Util.castNullable(versionClass, version) != null) found = true;
      else if (found) {
        LogHelper.error("Cannot cast to", versionClass, version);
        return null;
      }
    }
    //noinspection unchecked
    return found ? (SlaveMergeValue<VV>)this : null;
  }

  public ResolutionKind getResolutionKind(V resolution) {
    if (resolution == null) return ResolutionKind.UNRESOLVED;
    V remote = myVersions[REMOTE];
    if (resolution.equals(remote)) return ResolutionKind.DISCARD;
    if (resolution.isDeleted()) return ResolutionKind.DELETE;
    if (remote.isDeleted()) return ResolutionKind.COPY_NEW;
    return ResolutionKind.EDIT;
  }

  public V getVersion(int version) {
    return myVersions[version];
  }

  public EditItemModel getModel() {
    return myModel;
  }

  @NotNull
  public V tryFindSame(@NotNull V version) {
    for (V v : myVersions) {
      if (v.equals(version)) return v;
    }
    return version;
  }

  @Override
  public void render(CellState state, Canvas canvas, int version) {
    V slave = myVersions[version];
    boolean resolution = Util.equals(slave, getResolution());
    if (slave.isDeleted()) {
      canvas.setFontStyle(Font.ITALIC | (resolution ? Font.BOLD : 0));
      canvas.appendText("<Deleted>");
      return;
    }
    if (resolution) canvas.setFontStyle(Font.BOLD);
    slave.render(EngineConsts.getGuiFeaturesManager(myModel), canvas, resolution);
  }

  @Override
  public void setResolution(int version) {
    myModel.putValue(myResolutionKey, myVersions[version]);
  }

  public void setResolution(V version) {
    version = tryFindSame(version);
    myModel.putValue(myResolutionKey, version);
  }

  public V getResolution() {
    return myModel.getValue(myResolutionKey);
  }

  @Override
  public boolean isConflict() {
    return !isResolved() && isChanged(true) && isChanged(false) && !myVersions[LOCAL].equals(myVersions[REMOTE]);
  }

  @Override
  public boolean isChanged(boolean remote) {
    return !myVersions[BASE].equals(myVersions[remote ? REMOTE : LOCAL]);
  }

  @Override
  public Object getValue(int version) {
    return myVersions[version].toDisplayableString(EngineConsts.getGuiFeaturesManager(myModel));
  }

  @Override
  public boolean isResolved() {
    return getResolution() != null;
  }

  @Override
  public void addChangeListener(Lifespan life, ChangeListener listener) {
    myModel.addAWTChangeListener(life, listener);
  }

  @Override
  public void commit(CommitContext context) {
    V resolution = getResolution();
    if (resolution == null) return;
    if (resolution.equals(myVersions[REMOTE])) context.getDrain().discardChanges(getItem());
    else if (resolution.isDeleted()) context.getDrain().markMerged(getItem()).delete();
    else {
      boolean remoteDeleted = myVersions[REMOTE].isDeleted();
      if (remoteDeleted) {
        context.getDrain().discardChanges(getItem());
        resolution.commitCopy(context.getCreator());
      } else resolution.commitResolution(context.getDrain().markMerged(getItem()));
    }
  }

  public interface SlaveVersion {
    void render(GuiFeaturesManager guiFeaturesManager, Canvas canvas, boolean resolution);

    boolean isDeleted();

    Object toDisplayableString(@Nullable GuiFeaturesManager manager);

    void commitCopy(ItemVersionCreator issue);

    void commitResolution(ItemVersionCreator slave);
  }

  public enum ResolutionKind {
    UNRESOLVED,
    EDIT,
    COPY_NEW,
    DISCARD,
    DELETE
  }
}
