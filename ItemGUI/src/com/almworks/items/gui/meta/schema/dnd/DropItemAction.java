package com.almworks.items.gui.meta.schema.dnd;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.engine.Connection;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.English;
import com.almworks.util.Terms;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DropItemAction extends SimpleAction {
  public static final int MAX_MESSAGE_LEGTH = 70;
  private final ItemHypercube myTarget;
  private final String myFrameId;
  private final boolean myMove;

  public DropItemAction(ItemHypercube target, String frameId, boolean move) {
    myTarget = target;
    myFrameId = frameId;
    myMove = move;
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    DnDApplication application = prepareApplication(context);
    String problem = application.getProblemMessage();
    if (problem != null) throw new CantPerformExceptionExplained(problem);
    List<DnDFieldEditor> changes = application.getChanges();
    if (changes.isEmpty()) throw new CantPerformExceptionExplained("Nothing to change");
    String changeDescription;
    if (!application.isSilent()) changeDescription = application.isMove() ? "Move Here..." : "Add Here...";
    else changeDescription = getDescription(context, changes);
    context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, changeDescription);
//    context.putPresentationProperty(PresentationKey.DESCRIPTION_FG_BG, Pair.<Color, Color>create());
    context.putPresentationProperty(PresentationKey.DESCRIPTION_FG_BG, null);
    context.setEnabled(EnableState.ENABLED);
  }

  private String getChangeDescription(ActionContext context, List<DnDFieldEditor> changes) throws CantPerformException {
    String full = getDescription(context, changes, true);
    if (full.length() < MAX_MESSAGE_LEGTH) return full;
    String shortForm = getDescription(context, changes, false);
    if (shortForm.length() < MAX_MESSAGE_LEGTH) return shortForm;
    return "Change " + changes.size() + " " + English.getSingularOrPlural("field", changes.size());
  }

  private String getDescription(ActionContext context, List<DnDFieldEditor> changes) throws CantPerformException {
    StringBuilder builder = new StringBuilder();
    int andMoreFields = 0;
    for (DnDFieldEditor change : changes) {
      String description = change.getDescription(context, true);
      if (description.length() >= MAX_MESSAGE_LEGTH) description = change.getDescription(context, false);
      if (description.length() >= MAX_MESSAGE_LEGTH) andMoreFields++;
      else {
        if (builder.length() > 0) builder.append("\n");
        builder.append(description);
      }
    }
    if (andMoreFields > 0) {
      if (builder.length() > 0) builder.append("\nAnd ");
      else builder.append("Change ");
      builder.append(andMoreFields).append(" ").append(English.getSingularOrPlural("field", andMoreFields));
    }
    return builder.toString();
  }

  private String getDescription(ActionContext context, List<DnDFieldEditor> changes, boolean full) throws CantPerformException {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (DnDFieldEditor change : changes) {
      if (!first) builder.append("; ");
      first = false;
      builder.append(change.getDescription(context, full));
    }
    return builder.toString();
  }

  private DnDApplication prepareApplication(ActionContext context) throws CantPerformException {
    List<ItemWrapper> issues = CantPerformException.ensureNotEmpty(selectExisting(context));
    GuiFeaturesManager features = context.getSourceObject(GuiFeaturesManager.ROLE);
    DnDApplication application = new DnDApplication(issues, features, myTarget, myFrameId, myMove);
    for (DBAttribute<?> axis : application.getAttributes()) {
      DnDChange change = features.getDnDChange(axis);
      if (change != null) change.prepare(application);
      else if (SyncAttributes.CONNECTION.equals(axis)) checkConnection(application);
      else application.getProblems().addBasic("Cannot change read-only value");
    }
    ItemWrapper lockedIssue = ItemActionUtils.findLocked(context, issues);
    if (lockedIssue != null) {
      String message = Local.parse("You are editing some " + Terms.ref_artifacts + ". Please close the editor and retry.");
      application.getProblems().addBasic(message);
    }
    return application;
  }

  private List<ItemWrapper> selectExisting(ActionContext context) throws CantPerformException {
    ArrayList<ItemWrapper> wrappers = Collections15.arrayList(context.getSourceCollection(ItemWrapper.ITEM_WRAPPER));
    for (Iterator<ItemWrapper> it = wrappers.iterator(); it.hasNext(); ) {
      ItemWrapper wrapper = it.next();
      if (wrapper == null || wrapper.services().isRemoteDeleted()) it.remove();
    }
    return wrappers;
  }

  private void checkConnection(DnDApplication application) {
    LongList included = application.getIncluded(SyncAttributes.CONNECTION);
    for (ItemWrapper wrapper : application.getItems()) {
      Connection connection = wrapper.getConnection();
      if (connection == null) {
        application.getProblems().addNoValue("Connection is unknown");
        return;
      } else if (!included.contains(connection.getConnectionItem())) {
        application.getProblems().addBasic("Cannot change connection");
        return;
      }
    }
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    DnDApplication application = prepareApplication(context);
    CantPerformException.ensure(application.getProblemMessage() == null && !application.getChanges().isEmpty());
    application.apply(context);
  }
}
