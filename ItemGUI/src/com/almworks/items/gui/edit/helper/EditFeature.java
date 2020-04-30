package com.almworks.items.gui.edit.helper;

import com.almworks.integers.WritableLongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.sync.EditPrepare;
import com.almworks.util.config.Configuration;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface EditFeature {
  /**
   * If top editor component does not have own bottom border it should set the property to make EditorContent append upper border to error message.
   */
  ComponentProperty<Boolean> ERROR_BORDER = ComponentProperty.createProperty("errorBorder");

  /**
   * Inspects if the feature is applicable in current context. Also the feature has to subscribe to the context.
   * @param context an action context
   * @param updateRequest
   * @throws CantPerformException if edit is not possible right now
   */
  EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException;

  /**
   * Creates and initialize model to edit the current context (or to create new item(s) in current context). Also collects
   * items which has to be locked for this edit
   *
   * @param context the where an edit action invoked
   * @param itemsToLock collector for items to lock for the edit
   * @return model with data obtained from context
   * @throws CantPerformException if edit is not possible right now
   */
  DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException;

  /**
   * Reads DB before edit and load initial values into model.
   * Called during {@link com.almworks.items.sync.EditorFactory#prepareEdit(com.almworks.items.api.DBReader, com.almworks.items.sync.EditPrepare) prepare edit}.
   *
   * @param reader DB read access
   * @param model model to edit
   * @param editPrepare edit prepare context provided by item sync. This parameter is null in case edit does not lock any item.
   * @see com.almworks.items.sync.EditorFactory
   * @see EditPrepare
   */
  void prepareEdit(DBReader reader, DefaultEditModel.Root model, @Nullable EditPrepare editPrepare);

  /**
   * Creates UI component to perform edit. The component should be prepared for insertion into window - has all required decoration<br>
   * Initial model values are not fixed yet, so changes made to the model during the method invocation are treated as initial,
   * not changed by user.
   *
   * @param life editor life
   * @param model model to be edited
   * @param editorConfig editor config
   * @return editor UI component. null result means editing is not possible and should be terminated
   */
  @Nullable
  @ThreadAWT
  JComponent editModel(Lifespan life, EditItemModel model, Configuration editorConfig);
}
