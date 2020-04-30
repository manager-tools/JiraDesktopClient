package com.almworks.items.gui.edit;

import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.ItemEditor;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.util.text.NameMnemonic;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface FieldEditor {
  void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare);

  /**
   * Create components to of the editor.
   *
   * @param life editor life span
   * @param model editing model
   * @return components to edit or empty list if the editor does not require user interaction
   */
  @NotNull
  List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model);

  /**
   * Called my {@link EditItemModel model} after initial values are fixed due to call to {@link com.almworks.items.gui.edit.DefaultEditModel#saveInitialValues()}<br>
   * This happens when editor preparation is done right before editor are made accessible to user to make changes.
   * @param model the fixed model
   */
  void afterModelFixed(EditItemModel model);

  /**
   * Detect if the model data is changed by the editor.<br>
   * Unlike {@link #hasDataToCommit(EditItemModel)} the result of the method depends on user action. It answers the question
   * "Has the user changed anything? Or if user close and reopen the editor she see the same values?"
   * @param model model to be checked
   * @return true if the editor has made modifications
   * @see #hasDataToCommit(EditItemModel)
   */
  boolean isChanged(EditItemModel model);

  /**
   * Checks if the editor has any data to write to DB. In other words this method answers "Is there any reason to commit
   * the editor?"<br>
   * Unlike {@link #isChanged(EditItemModel)} the editor may contain new data for DB right after edit is started (for
   * example when creating new item there is already new data).<br>
   * For the most simple editors this method at first checks model for {@link #isChanged(EditItemModel) changes}, and
   * than checks {@link com.almworks.items.gui.edit.EditItemModel#isNewItem() new item model} for not empty value.
   * @param model model to be checked
   * @return true if editor has a data which is not already in DB
   * @see #isChanged(EditItemModel)
   */
  boolean hasDataToCommit(EditItemModel model);

  /**
   * Checks if the model contains not empty value - the value that should be visible to user. An example of empty value is
   * text that contains spaces only.
   * @param model model to check
   * @return true if there is not trivial value
   */
  boolean hasValue(EditModelState model);

  /**
   * Verifies current model state for some kind of correctness.
   */
  void verifyData(DataVerification verifyContext);

  /**
   * Called right after model copy is created. If editor keeps modifiable objects in the model it should create a copy of
   * such object in copied model.<br>
   * Model copy is created before commit (to fix the state), and may be copied in other cases.
   * @param copy the copy of the original model.
   */
  void afterModelCopied(EditItemModel copy);

  /**
   * Commits model state to DB for single item. Writes changes from model to specified item.<br>
   * This method is called on every {@link EditItemModel#getCommitEditors() commitable editor} for each {@link EditModelState#getEditingItems() editing item} separately.
   *
   * @throws CancelCommitException if the change cannot be written
   */
  void commit(CommitContext context) throws CancelCommitException;

  @NotNull
  NameMnemonic getLabelText(EditModelState model);

  /**
   * Notification that items are just concurrently committed.
   * @param newValues new items state
   * @see ItemEditor#onItemsChanged(gnu.trove.TLongObjectHashMap)
   */
  void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues);
}
