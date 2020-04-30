package com.almworks.items.gui.edit.util;

import com.almworks.integers.WritableLongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.sync.EditPrepare;
import com.almworks.util.LogHelper;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * The subclasses are edit features that can provide different editors depending on invocation context.<br>
 * During update and before actual edit invocation it asks subclass to choose edit behaviour and then delegates all subsequent calls to it.
 */
public abstract class MultiplexerEditFeature implements EditFeature {
  private final TypedKey<EditFeature> myFeatureHint = TypedKey.create("feature");

  @Override
  public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    return CantPerformException.ensureNotNull(chooseEdit(context, updateRequest)).checkContext(context, updateRequest);
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
    EditFeature feature = CantPerformException.ensureNotNull(chooseEdit(context, null));
    DefaultEditModel.Root model = feature.setupModel(context, itemsToLock);
    model.putHint(myFeatureHint, feature);
    return model;
  }

  @Override
  public void prepareEdit(DBReader reader, DefaultEditModel.Root model, @Nullable EditPrepare editPrepare) {
    EditFeature feature = model.getValue(myFeatureHint);
    if (feature != null) feature.prepareEdit(reader, model, editPrepare);
    else LogHelper.error("Missing edit feature");
  }

  @Override
  public JComponent editModel(Lifespan life, EditItemModel model, Configuration editorConfig) {
    EditFeature feature = model.getValue(myFeatureHint);
    if (feature != null) return feature.editModel(life, model, editorConfig);
    LogHelper.error("Missing edit feature");
    return null;
  }

  /**
   * Chooses the actual {@link EditFeature edit feature} for the context.<br>
   * The features probably should affect the {@link EditDescriptor.Impl#putPresentationProperty(com.almworks.util.ui.actions.PresentationKey, Object) actions presentation}
   * @param updateRequest optional. Allows to subscribe to context changes
   * @return the edit feature, or null if edit is not possible in the context
   * @see EditDescriptor#update(com.almworks.util.ui.actions.UpdateContext)
   */
  @Nullable
  protected abstract EditFeature chooseEdit(ActionContext context, @Nullable UpdateRequest updateRequest) throws CantPerformException;
}
