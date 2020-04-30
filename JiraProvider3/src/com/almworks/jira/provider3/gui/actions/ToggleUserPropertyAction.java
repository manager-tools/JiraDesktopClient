package com.almworks.jira.provider3.gui.actions;

import com.almworks.api.actions.InstantToggleSupport;
import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemDownloadStageKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.UiItem;
import com.almworks.api.engine.EngineUtils;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.gui.edit.engineactions.NotUploadedMessage;
import com.almworks.items.gui.meta.commons.LoadedItemUtils;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.util.AggregatingEditCommit;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.permissions.IssuePermissions;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.util.English;
import com.almworks.util.Terms;
import com.almworks.util.collections.PrimitiveUtils;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract class ToggleUserPropertyAction extends SimpleAction {
  private final DBAttribute<Boolean> myProperty;
  private final DBAttribute<Integer> myListCount;
  private final DBAttribute<Set<Long>> myList;
  private final DBStaticObject myPropertyKey;
  private final String mySwitchOnName;
  private final String mySwitchOffName;
  private final String mySwitchOnTooltip;
  private final String mySwitchOffTooltip;

  private final InstantToggleSupport myInstantToggle;
  private final String myMessageTitle;

  private ToggleUserPropertyAction(String switchOnName, String switchOffName, Icon icon, String switchOnTooltip, String switchOffTooltip,
    DBAttribute<Boolean> property, DBAttribute<Integer> listCount, DBAttribute<Set<Long>> list,
    DBStaticObject propertyKey)
  {
    super(switchOnName, icon);
    myProperty = property;
    myListCount = listCount;
    myList = list;
    myPropertyKey = propertyKey;
    mySwitchOnName = switchOnName;
    mySwitchOffName = switchOffName;
    mySwitchOnTooltip = switchOnTooltip;
    mySwitchOffTooltip = switchOffTooltip;
    myInstantToggle = new InstantToggleSupport(switchOnName);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    setDefaultPresentation(PresentationKey.TOGGLED_ON, false);
    updateOnChange(myInstantToggle.getModifiable());
    myMessageTitle = switchOnName;
  }

  public static ToggleUserPropertyAction VOTE = new ToggleUserPropertyAction("Vote", "Remove Vote", Icons.VOTE_ACTION, "Vote for the selected %s", "Remove your vote from the selected %s",
    Issue.VOTED, Issue.VOTES_COUNT, Issue.VOTERS, MetaSchema.KEY_VOTED)
  {
    @Override
    protected boolean checkPermission(ItemWrapper issue) {
      return IssuePermissions.hasPermission(issue, IssuePermissions.TOGGLE_VOTE_ISSUE);
    }
  };

  public static ToggleUserPropertyAction WATCH = new ToggleUserPropertyAction("Watch", "Stop Watching", Icons.WATCH_ACTION, "Start watching the selected %s", "Stop watching the selected %s",
    Issue.WATCHING, Issue.WATCHERS_COUNT, Issue.WATCHERS, MetaSchema.KEY_WATCHING) {
    @Override
    protected boolean checkPermission(ItemWrapper issue) {
      return IssuePermissions.hasPermission(issue, IssuePermissions.TOGGLE_WATCH_ISSUE);
    }
  };

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    List<ItemWrapper> wrappers = CantPerformException.ensureNotEmpty(ItemActionUtils.basicUpdate(context, false));
    SyncManager syncManager = context.getSourceObject(SyncManager.ROLE);
    context.updateOnChange(syncManager.getModifiable());
    for (ItemWrapper issue : wrappers) {
      CantPerformException.ensure(CantPerformException.cast(JiraConnection3.class, issue.getConnection()).isUploadAllowed());
      CantPerformException.ensure(checkPermission(issue));
      CantPerformException.ensure(syncManager.findLock(issue.getItem()) == null);
    }
    CantPerformException.ensure(ItemDownloadStageKey.isAllHasActualDetails(wrappers));
    boolean commonValue = CantPerformException.ensureNotNull(getCommonValue(wrappers));
    Boolean instantToggle = myInstantToggle.getState(context);
    if (instantToggle != null) commonValue = instantToggle;
    context.putPresentationProperty(PresentationKey.ENABLE, EnableState.ENABLED);
    context.putPresentationProperty(PresentationKey.TOGGLED_ON, commonValue);
    context.putPresentationProperty(PresentationKey.NAME, commonValue ? mySwitchOffName : mySwitchOnName);
    String tooltipTemplate = commonValue ? mySwitchOffTooltip : mySwitchOnTooltip;
    String tooltip = String.format(tooltipTemplate, English.getSingularOrPlural(Local.parse(Terms.ref_artifact), wrappers.size()));
    context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, tooltip);
  }

  @Nullable
  private Boolean getCommonValue(List<ItemWrapper> wrappers) {
    Boolean commonValue = null;
    for (ItemWrapper w : wrappers) {
      boolean v = Util.NN(LoadedItemUtils.getModelKeyValue(w, myPropertyKey, Boolean.class), false);
      if (commonValue != null && v != commonValue) return null;
      commonValue = v;
    }
    return commonValue;
  }

  protected abstract boolean checkPermission(ItemWrapper issue);

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    List<ItemWrapper> wrappers = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    Boolean commonValue = getCommonValue(wrappers);
    if (commonValue == null) throw new CantPerformExceptionSilently("No common value for items, cannot " + mySwitchOnName + ", items: " + wrappers);

    LongList items = PrimitiveUtils.collect(UiItem.GET_ITEM, wrappers);
    AggregatingEditCommit commit = new AggregatingEditCommit();
    commit.addProcedure(null, new CommitToggle(items, !commonValue));
    NotUploadedMessage.addTo(context, commit, myMessageTitle);
    boolean started = context.getSourceObject(SyncManager.ROLE).commitEdit(items, commit);
    CantPerformException.ensure(started);

    myInstantToggle.setState(context, !commonValue);
  }

  private final class CommitToggle extends EditCommit.Adapter {
    private final LongList myItems;
    private final boolean mySwitchOn;

    public CommitToggle(LongList items, boolean switchOn) {
      myItems = items;
      mySwitchOn = switchOn;
    }

    @Override
    public void performCommit(EditDrain drain) throws DBOperationCancelledException {
      for (ItemVersionCreator item : drain.changeItems(myItems)) {
        Long me = EngineUtils.getMe(item);
        item.setValue(myProperty, mySwitchOn);
        item.setValue(myListCount, Math.max(Util.NN(item.getValue(myListCount), 0) + (mySwitchOn ? +1 : -1), 0));
        Set<Long> oldList = Util.NN(item.getValue(myList), new HashSet<Long>(1));
        item.setValue(myList, mySwitchOn ? Collections15.add(oldList, me) : Collections15.remove(oldList, me));
      }
    }
  }
}
