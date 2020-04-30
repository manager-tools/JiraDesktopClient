package com.almworks.items.gui.edit;

import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.items.util.AttributeMap;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.RemoveableModifiable;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public abstract class EditItemModel implements EditModelState, RemoveableModifiable, ChangeListener {
  /**
   * Model master can provide default values for attributes via AttributeMap pointed by this key. Editors may modify the map on commit to change default value for next editor invocation.<br>
   * This key is intended to use with new item models. There is no contract for edit existing items.
   */
  public static final TypedKey<AttributeMap> DEFAULT_VALUES = TypedKey.create("defaultValues");

  public abstract void registerEditor(FieldEditor editor);

  public abstract void registerSingleEnum(DBAttribute<Long> attribute, Convertor<EditModelState, LongList> getter);

  public abstract void registerMultiEnum(DBAttribute<? extends Collection<Long>> attribute, Convertor<EditModelState, LongList> getter);

  public abstract void registerAttributeSource(Lifespan life, AttributeValueSource source);

  public abstract void setEditorEnabled(FieldEditor editor, boolean enable);

  /**
   * @return modifiable list copy
   */
  public abstract List<FieldEditor> getEnabledEditors();

  /**
   * @return modifiable list copy
   */
  public abstract List<FieldEditor> getCommitEditors();

  public abstract List<FieldEditor> getAllEditors();

  /**
   * Models may contain nested (child) models. This method returns root of the whole edit operation
   */
  @NotNull
  public abstract EditItemModel getRootModel();

  public final boolean hasDataToCommit() {
    return FieldEditorUtil.hasDataToCommit(this, getEnabledEditors());
  }

  /**
   * Checks that value for the key was not changed respecting {@link Object#equals(Object)}
   * @return true iff current value equals to fixed original one
   */
  public final boolean isEqualValue(TypedKey<?> key) {
    Object initial = getInitialValue(key);
    Object current = getValue(key);
    return Util.equals(initial, current);
  }

  @NotNull
  public final DataVerification verifyData(DataVerification.Purpose purpose) {
    DataVerification context = new DataVerification(this, purpose);
    verifyData(context);
    return context;
  }

  public void verifyData(DataVerification context) {
    LogHelper.assertError(context.getModel() == this, "Wrong model", context, this);
    Collection<? extends FieldEditor> editors = getEnabledEditors();
    for (FieldEditor editor : editors) editor.verifyData(context);
  }

  @Override
  public final ItemHypercube collectHypercube(Collection<? extends DBAttribute<?>> attributes) {
    ItemHypercubeImpl cube = new ItemHypercubeImpl();
    for (DBAttribute<?> attribute : attributes) {
      Pair<LongList, LongList> axisValues = getCubeAxis(attribute);
      if (axisValues == null) continue;
      cube.addAxisIncluded(attribute, axisValues.getFirst());
      cube.addAxisExcluded(attribute, axisValues.getSecond());
    }
    return cube;
  }

  public abstract void addChildModel(EditItemModel child);

  public abstract boolean isEnabled(FieldEditor editor);

  public static void notifyItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {
    for (FieldEditor editor : model.getAllEditors()) editor.onItemsChanged(model, newValues);
  }

  public interface AttributeValueSource extends Modifiable {
    /**
     * @return null if cannot provide value, not null means the source provides the value. If the value is provided by not available at the moment returns empty list
     */
    @Nullable
    LongList getItemAttributeValue(DBAttribute<Long> attribute);

    /**
     * @return pair [included, excluded] or null if the axis is not constrained.
     */
    Pair<LongList,LongList> getItemCubeAxis(DBAttribute<?> axis);
  }
}
