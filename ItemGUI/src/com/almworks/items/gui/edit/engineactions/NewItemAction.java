package com.almworks.items.gui.edit.engineactions;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.gui.MainMenu;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.helper.EditItemHelper;
import com.almworks.util.commons.Factory;
import com.almworks.util.images.IconHandle;
import com.almworks.util.ui.actions.*;

public class NewItemAction extends SimpleAction {
  private final EditFeature myEditor;

  private NewItemAction(Factory<String> name, IconHandle icon, Factory<String> shortDescription, EditFeature editor) {
    super(name, icon);
    setDefaultFactory(PresentationKey.SHORT_DESCRIPTION, shortDescription);
    myEditor = editor;
  }

  @Deprecated
  private NewItemAction(String name, IconHandle icon, String shortDescription, EditFeature editor) {
    super(name, icon);
    setDefaultText(PresentationKey.SHORT_DESCRIPTION, shortDescription);
    myEditor = editor;
  }

  public static AnAction primary(Factory<String> name, IconHandle icon, Factory<String> shortDescription, EditFeature editor) {
    return new NewItemAction(name, icon, shortDescription, editor);
  }

  @Deprecated
  public static AnAction slaves(String name, IconHandle icon, String shortDescription, EditFeature editor) {
    NewItemAction action = new NewItemAction(name, icon, shortDescription, editor);
    action.watchRole(ItemWrapper.ITEM_WRAPPER);
    return action;
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    EditItemAction.doUpdate(context, myEditor);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    EditDescriptor descriptor = EditItemAction.preparePerform(context, myEditor);
    peform(context, myEditor, descriptor);
  }

  public static void peform(ActionContext context, EditFeature editor, EditDescriptor descriptor)
    throws CantPerformException
  {
    EditItemHelper helper = context.getSourceObject(EditItemHelper.ROLE);
    helper.startCreateNewItem(context, editor, descriptor);
  }

  public static void registerActions(ActionRegistry registry) {
    registry.registerAction(MainMenu.NewItem.COMMIT, CommitEditAction.UPLOAD);
    registry.registerAction(MainMenu.NewItem.SAVE_DRAFT, CommitEditAction.SAVE);
    registry.registerAction(MainMenu.NewItem.DISCARD, CommitEditAction.DISCARD);
  }
}
