package com.almworks.items.gui.edit.engineactions;

import com.almworks.integers.LongArray;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.helper.EditItemHelper;
import com.almworks.items.sync.SyncManager;
import com.almworks.util.commons.Factory;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.IconHandle;
import com.almworks.util.ui.actions.*;
import org.jetbrains.annotations.Nullable;

public class EditItemAction extends SimpleAction {
  private final EditFeature myEditor;

  public EditItemAction(Factory<String> name, IconHandle icon, @Nullable Factory<String> shortDescription, EditFeature editor) {
    super(name, icon);
    watchModifiableRole(SyncManager.MODIFIABLE);
    if (shortDescription != null) setDefaultFactory(PresentationKey.SHORT_DESCRIPTION, shortDescription);
    myEditor = editor;
  }

  @Deprecated
  public EditItemAction(String name, IconHandle icon, @Nullable String shortDescription, EditFeature editor) {
    super(name, icon);
    watchModifiableRole(SyncManager.MODIFIABLE);
    if (shortDescription != null) setDefaultText(PresentationKey.SHORT_DESCRIPTION, Local.parse(shortDescription));
    myEditor = editor;
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    doUpdate(context, myEditor);
  }

  public static void doUpdate(UpdateContext context, EditFeature editor) throws CantPerformException {
    EditDescriptor descriptor = editor.checkContext(context, context.getUpdateRequest());
    context.setEnabled(EnableState.ENABLED);
    descriptor.update(context);
    context.getSourceObject(EditItemHelper.ROLE);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    startEdit(context, myEditor);
  }

  public static EditDescriptor preparePerform(ActionContext context, EditFeature editor) throws CantPerformException {
    EditDescriptor descriptor = editor.checkContext(context, new UpdateRequest(Updatable.NEVER, context));
    CantPerformException.ensure(descriptor.isEnabled());
    return descriptor;
  }

  public static void startEdit(ActionContext context, EditFeature editor) throws CantPerformException {
    EditDescriptor descriptor = preparePerform(context, editor);
    LongArray itemsToLock = new LongArray();
    DefaultEditModel.Root model = editor.setupModel(context, itemsToLock);
    EditItemHelper helper = context.getSourceObject(EditItemHelper.ROLE);
    helper.startEdit(itemsToLock, editor, descriptor, model);
  }
}
