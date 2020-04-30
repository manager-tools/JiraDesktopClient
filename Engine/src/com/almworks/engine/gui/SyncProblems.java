package com.almworks.engine.gui;

import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelMap;
import com.almworks.api.engine.ItemSyncProblem;
import com.almworks.api.engine.Synchronizer;
import com.almworks.api.gui.MainMenu;
import com.almworks.util.Terms;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.model.SetHolderUtils;
import com.almworks.util.ui.actions.*;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import static com.almworks.util.collections.Functional.first;

public class SyncProblems {
  private final ModelMap myModel;
  private final ItemMessages myMessages;
  private static final TypedKey<Object> MESSAGE_KEY = TypedKey.create("syncProblem");
  private static final String REMOTE_NOT_FOUND = Local.parse("Remote " + Terms.ref_artifacts + " not found");
  private final Synchronizer mySynchronizer;

  public SyncProblems(ModelMap model, ItemMessages messages) {
    myModel = model;
    myMessages = messages;
    LoadedItemServices lis = itemServices();
    assert lis != null;
    mySynchronizer = lis.getEngine().getSynchronizer();
  }

  public void attach(Lifespan lifespan) {
    @SuppressWarnings({"ConstantConditions"})
    final long item = itemServices().getItem();
    mySynchronizer.getProblems().addInitListener(lifespan, ThreadGate.AWT, SetHolderUtils.fromChangeListener(new ChangeListener() { @Override public void onChange() {
      onSyncProblem();
    }}));
    showProblemMessage(first(mySynchronizer.getItemProblems(item)));
  }

  @Nullable
  private LoadedItemServices itemServices() {
    return LoadedItemServices.VALUE_KEY.getValue(myModel);
  }

  private void onSyncProblem() {
    LoadedItemServices lis = itemServices();
    if (lis == null) return;
    long item = lis.getItem();
    if (lis.getConnection() == null) return;
    showProblemMessage(first(mySynchronizer.getItemProblems(item)));
  }

  /**
   * Shows the specified problem with date and used credentials appended.
   */
  private void showProblemMessage(@Nullable final ItemSyncProblem problem) {
    if (problem == null) myMessages.setMessage(MESSAGE_KEY, null);
    else {
      TypedKey<Object> messageKey = MESSAGE_KEY;
      String shortDescription = null;
      AnAction[] resolutions = null;
      ItemSyncProblem.Cause cause = problem.getCause();
      if (cause != null)
        switch (cause) {
        case ACCESS_DENIED: shortDescription = "Access denied";
          break;
        case COMPATIBILITY: shortDescription = "Compatibility problem detected";
          break;
        case REMOTE_NOT_FOUND: shortDescription = REMOTE_NOT_FOUND;
          break;
        case REPETITIVE_UPLOAD:
          shortDescription = "Repetitive upload detected";
          resolutions = getActions(MainMenu.Edit.DISCARD);
          break;
        case UPLOAD_CONFLICT:
          shortDescription = LocalChangeMessage.CONFLICT_SHORT;
          resolutions = getActions(MainMenu.Edit.MERGE, MainMenu.Edit.DISCARD);
          messageKey = LocalChangeMessage.CONFLICT_KEY;
        break;
        case ILLEGAL_DATA:
          shortDescription = "Cannot upload illegal values";
          resolutions = getActions(MainMenu.Edit.EDIT_ITEM, MainMenu.Edit.DISCARD);
        case GENERIC_UPLOAD_FAILURE:
          break;
        }
      if (shortDescription == null) shortDescription = "A problem has occurred";
      if (resolutions == null && problem.isResolvable()) resolutions = new AnAction[] {
        new SimpleAction("Resolve", Icons.ACTION_RESOLVE_PROBLEM) {
          @Override
          protected void customUpdate(UpdateContext context) throws CantPerformException {
          }

          @Override
          protected void doPerform(ActionContext context) throws CantPerformException {
            problem.resolve(context);
          }
        }};
      myMessages.setMessage(messageKey, ItemMessage.synchProblem(shortDescription, getLongDescription(problem), resolutions));
      if (messageKey != MESSAGE_KEY) myMessages.setMessage(MESSAGE_KEY, null);
    }
  }

  @Nullable
  private AnAction[] getActions(String ... ids) {
    return ItemMessage.getActions(itemServices(), ids);
  }

  private String getLongDescription(ItemSyncProblem problem) {
    StringBuffer buffer = new StringBuffer();
    String description = Util.NN(problem.getLongDescription()).trim();
    description = description.replaceAll("\n", "<br>");
    buffer.append(description);
    if (buffer.length() > 0 && buffer.charAt(buffer.length() - 1) != '.')
      buffer.append('.');
    if (buffer.length() > 0) buffer.append("<p> ");
    buffer.append("The problem has occurred ")
      .append(DateUtil.toFriendlyDateTime(problem.getTimeHappened()))
      .append(", ")
      .append(getCredentialsDescription(problem))
      .append('.');
    String longDescription = buffer.toString();
    return longDescription;
  }

  public static String getCredentialsDescription(ItemSyncProblem problem) {
    if (problem.getCredentialState().getSecond() == null)
      return "accessed without " + Terms.userName + "/password";
    return "used " + Terms.userName + " '" + problem.getCredentialState().getFirst() + "'" +
      (problem.getCredentialState().getSecond() ? "" : " without password");
  }
}
