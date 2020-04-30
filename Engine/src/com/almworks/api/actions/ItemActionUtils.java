package com.almworks.api.actions;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.MetaInfo;
import com.almworks.api.application.UiItem;
import com.almworks.api.edit.EditLifecycle;
import com.almworks.api.engine.Connection;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.DialogResult;
import com.almworks.api.gui.WindowController;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.sync.SyncManager;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.commons.Factory;
import com.almworks.util.components.PlaceHolder;
import com.almworks.util.config.ConfigAccessors;
import com.almworks.util.config.Configuration;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.concurrent.SynchronizedBoolean;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.util.Collection;
import java.util.List;

/**
 * @author dyoma
 */
public class ItemActionUtils {
  public static final String COMMIT_NAME = "Upload";
  public static final String COMMIT_TOOLTIP = "Save changes and upload to server";
  public static final String SAVE_NAME = "Save Draft";
  public static final String SAVE_TOOLTIP = "Save changes to the local database without uploading";
  public static final String CANCEL_NAME = "Cancel";
  public static final String CANCEL_TOOLTIP = "Discard changes and close window";

  public static final DataRole<PlaceHolder> ITEM_EDIT_WINDOW_TOOLBAR_RIGHT_PANEL =
    DataRole.createRole(PlaceHolder.class);
  @Deprecated
  public static final AnAction DISCARD = setupCancelEditAction(
    new SimpleAction() {
      @Override
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.putPresentationProperty(PresentationKey.SHORTCUT, Shortcuts.ESCAPE);
      }
      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        if (context.getAvailableRoles().contains(EditLifecycle.ROLE)) {
          context.getSourceObject(EditLifecycle.ROLE).discardEdit(context);
        } else {
          Log.warn("Commit action: no edit lifecycle (" + tryGetWindowTitle(context) + ')');
          assert false;
          WindowController.CLOSE_ACTION.perform(context);
        }
      }
    });

  public static List<ItemWrapper> basicUpdate(UpdateContext context, boolean acceptDeleted) throws CantPerformException {
    watchItemsCollection(context);
    List<ItemWrapper> collection = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    for (ItemWrapper wrapper : collection) {
      CantPerformException.ensure(acceptDeleted || !wrapper.services().isRemoteDeleted());
      Connection connection = wrapper.getConnection();
      CantPerformException.ensure(connection != null && !connection.getState().getValue().isDegrading());
    }
    return collection;
  }

  public static void watchItemsCollection(@NotNull UpdateContext context) {
    context.watchRole(ItemWrapper.ITEM_WRAPPER);
    context.watchRole(LoadedItem.LOADED_ITEM);
  }

  public static void checkNotLocked(ActionContext context, Collection<? extends UiItem> items) throws CantPerformException {
    final SyncManager syncMan = context.getSourceObject(SyncManager.ROLE);
    for(UiItem item : items) {
      checkNotLocked(syncMan, item);
    }
  }

  @Nullable
  public static <I extends UiItem> I findLocked(ActionContext context, Collection<I> items) throws CantPerformException {
    if (items == null || items.isEmpty()) return null;
    SyncManager manager = context.getSourceObject(SyncManager.ROLE);
    for (I item : items) {
      if (item == null) continue;
      if (manager.findLock(item.getItem()) != null) return item;
    }
    return null;
  }

  public static void checkNotLocked(SyncManager locker, UiItem item) throws CantPerformException {
    checkNotLocked(locker, item.getItem());
  }

  public static void checkNotLocked(ActionContext context, LongList items) throws CantPerformException {
    final SyncManager syncMan = context.getSourceObject(SyncManager.ROLE);
    CantPerformException.ensure(syncMan.findAnyLock(items) == null);
  }

  public static void checkNotLocked(SyncManager locker, long item) throws CantPerformException {
    CantPerformException.ensure(locker.findLock(item) == null);
  }

  public static AnAction setupCancelEditAction(SimpleAction action) {
    action.setDefaultPresentation(PresentationKey.NAME, L.actionName(CANCEL_NAME));
    action.setDefaultPresentation(PresentationKey.SMALL_ICON, Icons.ACTION_GENERIC_CANCEL_OR_REMOVE);
    action.setDefaultPresentation(PresentationKey.ENABLE, EnableState.ENABLED);
    action.setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, L.tooltip(CANCEL_TOOLTIP));
    return action;
  }

  public static AnAction setupCommitAction(SimpleAction action) {
    action.setDefaultPresentation(PresentationKey.NAME, L.actionName(COMMIT_NAME));
    action.setDefaultPresentation(PresentationKey.SMALL_ICON, Icons.ACTION_COMMIT_ARTIFACT);
    action.setDefaultPresentation(PresentationKey.ENABLE, EnableState.DISABLED);
    action.setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, L.tooltip(COMMIT_TOOLTIP));
    return action;
  }

  public static AnAction setupSaveAction(SimpleAction action) {
    action.setDefaultPresentation(PresentationKey.NAME, L.actionName(SAVE_NAME));
    action.setDefaultPresentation(PresentationKey.SMALL_ICON, Icons.ACTION_SAVE);
    action.setDefaultPresentation(PresentationKey.ENABLE, EnableState.DISABLED);
    action.setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
      L.tooltip(SAVE_TOOLTIP));
    return action;
  }

  public static Factory<DialogBuilder> getDialogBuilder(final ActionContext context, final String dialogId) throws CantPerformException {
    final DialogManager dialogManager = context.getSourceObject(DialogManager.ROLE);
    return new Factory<DialogBuilder>() {
      @Override
      public DialogBuilder create() {
        return dialogManager.createBuilder(dialogId);
      }
    };
  }

  public static LongArray collectItems(Collection<? extends UiItem> items) {
    if (items == null || items.isEmpty()) return new LongArray();
    LongArray result = new LongArray();
    for (UiItem item : items) result.add(item.getItem());
    return result;
  }

  public static MetaInfo getUniqueMetaInfo(List<? extends ItemWrapper> items) throws CantPerformException {
    MetaInfo metaInfo = null;
    for (ItemWrapper wrapper : items) {
      MetaInfo currentMeta = wrapper.getMetaInfo();
      if (metaInfo == null) {
        metaInfo = currentMeta;
      } else if (metaInfo != currentMeta) {
        throw new CantPerformException(Local.parse("Can't apply to different types of " + Terms.ref_artifacts));
      }
    }
    return metaInfo;
  }

  public static JCheckBox createCommitImmediatelyCheckbox(
    Configuration config, @Nullable final SynchronizedBoolean flagHolder)
  {
    final ConfigAccessors.Bool commitImmediately = ConfigAccessors.bool(config, "commitImmediately", true);

    final JCheckBox checkbox = new JCheckBox(Local.text("app.Comments.UploadImmediately", "Upload immediately"));
    checkbox.setMnemonic('U');

    final boolean flagSet = commitImmediately.getBool();
    if(flagHolder != null) {
      flagHolder.set(flagSet);
    }
    checkbox.setSelected(flagSet);

    checkbox.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        final boolean selected = checkbox.isSelected();
        if(flagHolder != null) {
          flagHolder.set(selected);
        }
        commitImmediately.setBool(selected);
      }
    });

    return checkbox;
  }

  /**
   * Asks confirmation, optionally allows to select immediate upload or save draft
   * @param askUpload adds "upload now" check box
   * @return null if question is not confirmed.<br>
   * true if user ask to upload immediately, requires askUpload to be set to true<br>
   * false - save as draft
   */
  public static Boolean askConfirmation(ActionContext context, JComponent component, String title, String windowId, boolean askUpload) throws CantPerformException {
    SynchronizedBoolean commitImmediately = askUpload ? new SynchronizedBoolean(true) : null;
    DialogResult<Boolean> result = prepareConfirmationDialog(context, commitImmediately, windowId);
    if (!result.showModal(title, component)) return null;
    return askUpload ? commitImmediately.get() : Boolean.FALSE;
  }

  public static DialogResult<Boolean> prepareConfirmationDialog(ActionContext context, @Nullable SynchronizedBoolean commitImmediately, String windowId) throws CantPerformException {
    DialogResult<Boolean> result = DialogResult.create(context, windowId);
    result.setOkResult(true);
    result.setCancelResult(false);
    if (commitImmediately != null) result.setBottomLineComponent(createCommitImmediatelyCheckbox(result.getConfig(), commitImmediately));
    return result;
  }

  @NotNull
  private static String tryGetWindowTitle(ActionContext context) {
    String windowTitle = "";
    try {
      WindowController wc = context.getSourceObject(WindowController.ROLE);
      windowTitle = wc.getTitle();
    } catch (CantPerformException ex) {
    }
    return windowTitle;
  }
}