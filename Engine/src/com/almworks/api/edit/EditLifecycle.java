package com.almworks.api.edit;

import com.almworks.integers.LongList;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditControl;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

/**
 * Supports lifecycle of editor with a window. Use it to update commit actions, manage close confirmations etc.
 * @deprecated replaced in JC3
 */
@Deprecated
@ThreadAWT
public interface EditLifecycle {
  TypedKey<EditLifecycle> SERVICE_KEY = TypedKey.create("editLife");
  Role<EditLifecycle> ROLE = Role.role(EditLifecycle.class);
  Role<Modifiable> MODIFIABLE = Role.role("EditLifecycle.MODIFIABLE");

  void commit(ActionContext context, EditCommit commit) throws CantPerformException;

  void commit(ActionContext context, EditCommit commit, boolean unsafe) throws CantPerformException;

  /**
   * Releases current edit. All children edits will be released too.
   * Does not do anything if {@link #isDuringCommit}.
   * @param context
   */
  @ThreadAWT
  void discardEdit(ActionContext context) throws CantPerformException;

  /**
   * @return modifiable that fires when commit state is changed
   */
  Modifiable getModifiable();

  /**
   * @throws CantPerformException if commit action should be disabled
   */
  void checkCommitAction() throws CantPerformException;

  /**
   * @return true if commit has been announced and has not finished yet
   */
  boolean isDuringCommit();

  LongList getEditingItems();

  /**
   * @return edit control if some items have been locked for this edit
   */
  @Nullable
  EditControl getControl();
}
