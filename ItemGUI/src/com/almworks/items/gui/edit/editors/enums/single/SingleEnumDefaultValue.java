package com.almworks.items.gui.edit.editors.enums.single;

import com.almworks.api.application.ItemKey;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.sync.VersionSource;
import com.almworks.util.Pair;
import org.jetbrains.annotations.Nullable;

/**
 * This interface represents a default value for single enum editors. It supports:<br>
 * 1. {@link #prepare(com.almworks.items.sync.VersionSource, com.almworks.items.gui.edit.EditItemModel) Load} value from DB on editor initialization<br>
 * 2. Provide that value {@link #isEnabled(com.almworks.items.gui.edit.EditItemModel) has been loaded} and the {@link #getValue(com.almworks.items.gui.edit.EditItemModel) loaded value}
 */
public interface SingleEnumDefaultValue {
  /**
   * Prepare special value before edit begins. Call during {@link com.almworks.items.gui.edit.FieldEditor#prepareModel(com.almworks.items.sync.VersionSource, com.almworks.items.gui.edit.EditItemModel, com.almworks.items.sync.EditPrepare)}
   */
  void prepare(VersionSource source, EditItemModel model);

  /**
   * Checks if the value can be set to the wrapped editor. If false is returned than no additional button is added. This method is called during {@link com.almworks.items.gui.edit.FieldEditor#createComponents(org.almworks.util.detach.Lifespan, com.almworks.items.gui.edit.EditItemModel)},
   * so it is call only once (during single edit session).
   * @param model the same model was passed when {@link #prepare(com.almworks.items.sync.VersionSource, com.almworks.items.gui.edit.EditItemModel)} was called.
   * @return add button or not
   */
  boolean isEnabled(EditItemModel model);

  /**
   * The value for editor when user presses the button
   * @return ItemKey and long presentations for the value. Null return value is equal to &lt;null, null&gt; pair and means "set null".
   */
  @Nullable
  Pair<? extends ItemKey, Long> getValue(EditItemModel model);
}
